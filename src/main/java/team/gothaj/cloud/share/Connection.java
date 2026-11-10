package team.gothaj.cloud.share;

import io.github.kawase.NativeObfuscate;
import io.github.kawase.VMProtect;
import org.java_websocket.WebSocket;


import java.io.*;

import ru.constantdynamic.hpke.crypto.param.impl.KeyParameter;
import ru.constantdynamic.hpke.crypto.param.impl.ParametersWithIV;
import team.gothaj.cloud.share.cipher.Cipher;
import team.gothaj.cloud.share.cipher.impl.HPKECipher;
import team.gothaj.cloud.share.packet.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Connection {
    protected final WebSocket webSocket;
    protected final Cipher cipher;
    
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<Packet>> pendingRequests = new ConcurrentHashMap<>();

    private final PacketRegistry packetRegistry = new PacketRegistry();
    
    protected volatile boolean running = true;

    public Connection(WebSocket webSocket, Cipher cipher) {
        this.webSocket = webSocket;
        this.cipher = cipher;
    }
    
    /**
     * Gets the current session token for this connection.
     * @return The session token, or null/empty if not authenticated
     */
    protected abstract String getSessionToken();
    
    /**
     * Sets the session token for this connection.
     * @param token The session token to set
     */
    protected abstract void setSessionToken(String token);

    public CompletableFuture<Packet> sendRequestAsync(Packet packet) {
        int requestId = requestIdCounter.getAndIncrement();
        CompletableFuture<Packet> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            sendPacket(requestId, packet);
        } catch (IOException e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    public Packet sendRequestSync(Packet packet, long timeoutMs) throws IOException {
        try {
            return sendRequestAsync(packet).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IOException("Request timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("Request failed", e.getCause());
        }
    }

    @NativeObfuscate
    @VMProtect(VMProtect.ProtectionLevel.VIRTUALIZATION)
    protected void sendPacket(int requestId, Packet packet) throws IOException {
        byte[] packetData = PacketCodec.serialize(packet);
        
        // Inline cipher.encrypt(packetData)
        byte[] encryptedData;
        HPKECipher hpke = (HPKECipher) cipher;
        synchronized (hpke) {
            if (!hpke.initialized) {
                if (hpke.isServer) {
                    // Server cannot encrypt before handbook handshake completion (receiving first packet)
                    throw new IllegalStateException("Server cannot send data before handshake completion");
                } else {
                    // Client initialization
                    hpke.initializeClient();
                }
            }

            // Prepare Cipher
            // byte[] nonce = xorNonce(sendBaseNonce, sendSequence);
            byte[] nonce = Arrays.copyOf(hpke.sendBaseNonce, hpke.sendBaseNonce.length);
            for (int i = 0; i < 8; i++) {
                nonce[hpke.sendBaseNonce.length - 8 + i] ^= (byte) ((hpke.sendSequence >>> (8 * (7 - i))) & 0xFF);
            }

            hpke.encryptor.init(true, new ParametersWithIV(new KeyParameter(hpke.sendKey), nonce));
            hpke.sendSequence++;

            // Encrypt
            byte[] output = new byte[hpke.encryptor.getOutputSize(packetData.length)];
            int len = hpke.encryptor.processByte(packetData, 0, packetData.length, output, 0);
            try {
                hpke.encryptor.doFinal(output, len);
            } catch (Exception e) {
                throw new RuntimeException("Encryption failed", e);
            }

            // If client and this is the very first packet (sequence was 0 before increment, now 1)
            if (!hpke.isServer && hpke.encapsulatedKey != null) {
                byte[] result = new byte[hpke.encapsulatedKey.length + output.length];
                System.arraycopy(hpke.encapsulatedKey, 0, result, 0, hpke.encapsulatedKey.length);
                System.arraycopy(output, 0, result, hpke.encapsulatedKey.length, output.length);
                hpke.encapsulatedKey = null; // Clear it, subsequent packets are raw
                encryptedData = result;
            } else {
                encryptedData = output;
            }
        }

        // Get session token (empty string if not authenticated)
        String sessionToken = getSessionToken();
        if (sessionToken == null) {
            sessionToken = "";
        }
        byte[] tokenBytes = sessionToken.getBytes("UTF-8");
        
        // Format: [requestId:4][tokenLength:4][token:N][encryptedData]
        // SecureBuffer buffer = SecureBuffer.allocate(4 + 4 + tokenBytes.length + encryptedData.length);
        int totalSize = 4 + 4 + tokenBytes.length + encryptedData.length;
        byte[] buffer = new byte[totalSize];
        int position = 0;

        // buffer.putInt(requestId);
        if (buffer.length - position < 4) {
             throw new IndexOutOfBoundsException("Buffer overflow");
        }
        buffer[position++] = (byte) (requestId >> 24);
        buffer[position++] = (byte) (requestId >> 16);
        buffer[position++] = (byte) (requestId >> 8);
        buffer[position++] = (byte) (requestId);

        // buffer.putInt(tokenBytes.length);
        int tokenLen = tokenBytes.length;
        if (buffer.length - position < 4) {
             throw new IndexOutOfBoundsException("Buffer overflow");
        }
        buffer[position++] = (byte) (tokenLen >> 24);
        buffer[position++] = (byte) (tokenLen >> 16);
        buffer[position++] = (byte) (tokenLen >> 8);
        buffer[position++] = (byte) (tokenLen);

        // buffer.put(tokenBytes);
        int len_tb = tokenBytes.length;
        if (len_tb > buffer.length - position) {
            throw new IndexOutOfBoundsException("Buffer overflow");
        }
        System.arraycopy(tokenBytes, 0, buffer, position, len_tb);
        position += len_tb;

        // buffer.put(encryptedData);
        int len_ed = encryptedData.length;
        if (len_ed > buffer.length - position) {
            throw new IndexOutOfBoundsException("Buffer overflow");
        }
        System.arraycopy(encryptedData, 0, buffer, position, len_ed);
        position += len_ed;
        
        synchronized (webSocket) {
            // webSocket.send(buffer.array());
            webSocket.send(buffer);
        }
    }

    @NativeObfuscate()
    @VMProtect(VMProtect.ProtectionLevel.VIRTUALIZATION)
    public void onBinaryMessage(byte[] data) {
        try {
            // SecureBuffer buffer = SecureBuffer.wrap(data);
            byte[] buffer = data;
            int position = 0;

            // int requestId = buffer.getInt();
            if (buffer.length - position < 4) {
                throw new IndexOutOfBoundsException("Buffer underflow");
            }
            int requestId = (((buffer[position++]) & 0xFF) << 24) |
                    (((buffer[position++]) & 0xFF) << 16) |
                    (((buffer[position++]) & 0xFF) << 8) |
                    (((buffer[position++]) & 0xFF));
            
            // Extract session token
            // int tokenLength = buffer.getInt();
            if (buffer.length - position < 4) {
                throw new IndexOutOfBoundsException("Buffer underflow");
            }
            int tokenLength = (((buffer[position++]) & 0xFF) << 24) |
                    (((buffer[position++]) & 0xFF) << 16) |
                    (((buffer[position++]) & 0xFF) << 8) |
                    (((buffer[position++]) & 0xFF));

            String receivedToken = "";
            if (tokenLength > 0) {
                byte[] tokenBytes = new byte[tokenLength];
                // buffer.get(tokenBytes);
                if (tokenLength > buffer.length - position) {
                    throw new IndexOutOfBoundsException("Buffer underflow");
                }
                System.arraycopy(buffer, position, tokenBytes, 0, tokenLength);
                position += tokenLength;

                receivedToken = new String(tokenBytes, "UTF-8");
            }
            
            // byte[] encryptedData = new byte[buffer.remaining()];
            int remaining = buffer.length - position;
            byte[] encryptedData = new byte[remaining];
            // buffer.get(encryptedData);
            if (remaining > buffer.length - position) {
                 throw new IndexOutOfBoundsException("Buffer underflow");
            }
            System.arraycopy(buffer, position, encryptedData, 0, remaining);
            position += remaining;
            
            // byte[] packetData = cipher.decrypt(encryptedData);
            byte[] packetData;
            HPKECipher hpke = (HPKECipher) cipher;
            synchronized (hpke) {
                byte[] dataToDecrypt = encryptedData;
                if (!hpke.initialized) {
                    if (hpke.isServer) {
                        hpke.initializeServer(dataToDecrypt);
                        // Adjust data to point to payload only
                        dataToDecrypt = Arrays.copyOfRange(dataToDecrypt, 32, dataToDecrypt.length);
                    } else {
                        throw new IllegalStateException("Client received data before sending handshake");
                    }
                }

                // Prepare Cipher
                // byte[] nonce = xorNonce(recvBaseNonce, recvSequence);
                byte[] nonce = Arrays.copyOf(hpke.recvBaseNonce, hpke.recvBaseNonce.length);
                for (int i = 0; i < 8; i++) {
                    nonce[hpke.recvBaseNonce.length - 8 + i] ^= (byte) ((hpke.recvSequence >>> (8 * (7 - i))) & 0xFF);
                }

                hpke.decryptor.init(false, new ParametersWithIV(new KeyParameter(hpke.recvKey), nonce));
                hpke.recvSequence++;

                // Decrypt
                byte[] output = new byte[hpke.decryptor.getOutputSize(dataToDecrypt.length)];
                int len = hpke.decryptor.processByte(dataToDecrypt, 0, dataToDecrypt.length, output, 0);
                try {
                    hpke.decryptor.doFinal(output, len);
                } catch (Exception e) {
                    throw new RuntimeException("Decryption failed", e);
                }
                packetData = output;
            }
            Packet packet = PacketCodec.deserialize(packetData);

            // Store session token from header if present and non-empty
            // This ensures token is available when packet handlers execute
            if (receivedToken != null && !receivedToken.isEmpty()) {
                String currentToken = getSessionToken();
                if (currentToken == null || currentToken.isEmpty() || !currentToken.equals(receivedToken)) {
                    setSessionToken(receivedToken);
                }
            }

            boolean futureHandled = false;
            // Handle Pending Requests (Futures)
            if (requestId != 0 && requestId != -1 && pendingRequests.containsKey(requestId)) {
                CompletableFuture<Packet> future = pendingRequests.remove(requestId);
                if (future != null) {
                    future.complete(packet);
                    futureHandled = true;
                }
            }

            // Handle Registered Listeners
            // We dispatch to listeners if:
            // 1. We have listeners for this packet type (Snooping / EventBus style)
            // 2. OR it wasn't handled by a future (Unsolicited / Broadcast)
            if (packetRegistry.hasHandlers(packet.getType())) {
                handlePacket(packet, requestId, receivedToken);
            } else if (!futureHandled) {
                // If nobody handled it (no future, no listener), then we treat it as unhandled packet logic
                // which might send an error response if it was a request.
                // We treat requestId -1 as a broadcast/push that doesn't expect a response, but 0 usually implies the same.
                handlePacket(packet, requestId, receivedToken);
            }
        } catch (IOException e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    protected void handlePacket(Packet packet, int requestId, String receivedToken) {
        PacketResponder responder = response -> {
            try {
                sendPacket(requestId, response);
            } catch (IOException e) {
                System.err.println("Failed to send response: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        };

        if (!packetRegistry.hasHandlers(packet.getType())) {
            Packet errorResponse = Packet.builder()
                    .type("ERROR")
                    .putString("message", "Unknown packet type: " + packet.getType())
                    .build();
            responder.respond(errorResponse);
        } else {
            packetRegistry.handle(packet.getType(), packet, responder);
        }
    }

    public void registerHandler(String type, PacketHandler handler) {
        packetRegistry.register(type, handler);
    }

    public void close() {
        running = false;
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.close();
        }
        
        IOException closeException = new IOException("Connection closed");
        for (CompletableFuture<Packet> future : pendingRequests.values()) {
            future.completeExceptionally(closeException);
        }
        pendingRequests.clear();
    }

    public boolean isConnected() {
        return webSocket != null && webSocket.isOpen();
    }
}
