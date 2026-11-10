package team.gothaj.cloud.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import team.gothaj.cloud.share.packet.Packet;
import team.gothaj.cloud.share.packet.PacketHandler;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Cloud extends WebSocketClient {
    private static final long KEEPALIVE_INTERVAL_MS = 15_000; // 15 seconds


    private static Cloud instance;

    private CloudConnection connection;

    private volatile boolean shouldReconnect = true;
    private volatile String lastSessionToken = ""; // Store last session token for reconnection

    // Handlers registered via registerHandler() must survive reconnects, since each
    // onOpen() creates a brand new CloudConnection backed by an empty PacketRegistry.
    private final List<HandlerRegistration> handlerRegistrations = new CopyOnWriteArrayList<>();

    private record HandlerRegistration(String type, PacketHandler handler) {
    }

    private final ScheduledExecutorService keepaliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Keepalive Timer");
        t.setDaemon(true);
        return t;
    });
    
    private volatile ScheduledFuture<?> keepaliveTask;

    private Cloud(URI serverUri) {
        super(serverUri);
        instance = this;
    }

    public void post() {
        shouldReconnect = true;
        new Thread(() -> {
            try {
                System.out.println("Connecting to server...");
                this.connectBlocking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Connection interrupted: " + e.getMessage());
            }
        }, "Cloud Client V2").start();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");
        connection = new CloudConnection(this);
        
        connection.registerHandler("AUTH", (packet, responder) -> {
           // System.out.println("Auth received on client handler: " + packet);
            if (packet.getBoolean("success")) {
                lastSessionToken = packet.getString("token");
                startKeepalive();
               // System.out.println("[Client] Authentication successful, keepalive timer started");
            }
        });
        
        // Register handler for RECONNECT responses
        connection.registerHandler("RECONNECT", (packet, responder) -> {
            if (packet.getBoolean("success")) {
                //System.out.println("[Client] Reconnection successful, session resumed");
                startKeepalive();
            } else {
              //  System.err.println("[Client] Reconnection failed: " + packet.getString("message"));
                // Clear stored token and close connection to trigger AUTH on next reconnect
                lastSessionToken = "";
            }
        });

        // Re-apply handlers registered via registerHandler() - the CloudConnection above is a
        // fresh instance with an empty registry, so these would otherwise be lost on reconnect.
        for (HandlerRegistration registration : handlerRegistrations) {
            connection.registerHandler(registration.type(), registration.handler());
        }

        // Attempt to reconnect with stored session token
        if (lastSessionToken != null && !lastSessionToken.isEmpty()) {
          //  System.out.println("[Client] Attempting to reconnect with stored session token");
            
            Packet reconnectPacket = Packet.builder()
                    .type("RECONNECT")
                    .putString("token", lastSessionToken)
                    .build();
            
            connection.sendRequestAsync(reconnectPacket)
                    .exceptionally(e -> {
                        System.err.println("[Client] RECONNECT packet failed: " + e.getMessage());
                        lastSessionToken = ""; // Clear token on failure
                        return null;
                    });
        }
    }

    @Override
    public void onMessage(String message) {
        // We're using binary messages for packet communication
        System.err.println("Unexpected text message received: " + message);
    }

    @Override
    public void onMessage(byte[] bytes) {
        if (connection != null) {
            try {
                // byte[] data = new byte[bytes.remaining()];
                int remaining = bytes.length;
                byte[] data = new byte[remaining];
                
                // bytes.get(data);
                System.arraycopy(bytes, 0, data, 0, remaining);

                connection.onBinaryMessage(data);
            } catch (Exception e) {
                 System.err.println("[Client] Error processing binary message: " + e.getMessage());
                 e.printStackTrace(System.err);
            }
        } else {
             System.err.println("[Client] Message received but connection is null!");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from server (code: " + code + ", reason: " + reason + ")");
        
        // Stop keepalive timer
        stopKeepalive();
        
        if (connection != null) {
            connection.close();
            connection = null;
        }

        if (shouldReconnect) {
            new Thread(() -> {
                try {
                    System.out.println("Reconnecting in 1 second...");
                    Thread.sleep(1000);
                    this.reconnect();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Client error: " + ex.getMessage());
        ex.printStackTrace(System.err);
    }

    public CompletableFuture<Packet> sendRequestAsync(Packet packet) {
        if (connection == null || !connection.isConnected()) {
            CompletableFuture<Packet> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Not connected to server"));
            return future;
        }
        return connection.sendRequestAsync(packet);
    }

    public Packet sendRequestSync(Packet packet, long timeoutMs) throws Exception {
        if (connection == null || !connection.isConnected()) {
            throw new RuntimeException("Not connected to server");
        }
        return connection.sendRequestSync(packet, timeoutMs);
    }

    public boolean isReady() {
        return connection != null && connection.isConnected();
    }

    public boolean waitUntilReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (isReady()) {
                return true;
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return isReady();
    }

    public void registerHandler(String type, PacketHandler handler) {
        handlerRegistrations.add(new HandlerRegistration(type, handler));
        if (connection != null) {
            connection.registerHandler(type, handler);
        }
    }
    
    /**
     * Sets whether the client should automatically reconnect on disconnect.
     * @param autoReconnect true to enable auto-reconnect, false to disable
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.shouldReconnect = autoReconnect;
    }
    
    /**
     * Starts the keepalive timer that sends KEEPALIVE packets every 30 seconds.
     */
    private void startKeepalive() {
        //System.out.println("[Client] startKeepalive() called on thread: " + Thread.currentThread().getName());
        stopKeepalive(); // Stop any existing keepalive task
        
        keepaliveTask = keepaliveExecutor.scheduleAtFixedRate(() -> {
            try {
             //   System.out.println("[Client] Keepalive timer tick");
                if (connection != null && connection.isConnected()) {
                 //   System.out.println("[Client] Sending keepalive packet...");
                    Packet keepalivePacket = Packet.builder()
                            .type("KEEPALIVE")
                            .build();
                    
                    connection.sendRequestAsync(keepalivePacket)
                            .thenAccept(response -> {
                                if (response.getType().equals("KEEPALIVE_ACK")) {
                                //    System.out.println("[Client] Keepalive acknowledged by server");
                                }
                            })
                            .exceptionally(e -> {
                                //System.err.println("[Client] Keepalive failed: " + e.getMessage());
                                return null;
                            });
                } else {
                   // System.out.println("[Client] Keepalive skipped: Connection not ready (Conn: " + (connection != null) + ")");
                }
            } catch (Exception e) {
               // System.err.println("[Client] Error sending keepalive: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }, KEEPALIVE_INTERVAL_MS, KEEPALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        //System.out.println("[Client] Keepalive timer started (interval: " + KEEPALIVE_INTERVAL_MS + "ms)");
    }
    
    /**
     * Stops the keepalive timer.
     */
    private void stopKeepalive() {
        if (keepaliveTask != null && !keepaliveTask.isCancelled()) {
            keepaliveTask.cancel(false);
            keepaliveTask = null;
            //System.out.println("[Client] Keepalive timer stopped");
        }
    }

    /**
     * Stops the client and prevents auto-reconnection.
     */
    public void stopClient() {
        shouldReconnect = false;
        stopKeepalive();
        if (connection != null) {
            connection.close();
        }
        keepaliveExecutor.shutdown();
        this.close();
    }

    public static synchronized Cloud getInstance() {
        if (instance == null) {
            try {
                URI serverUri = new URI("wss://api.gothaj.top");
                new Cloud(serverUri);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create client instance", e);
            }
            instance.post();
        }
        return instance;
    }

}
