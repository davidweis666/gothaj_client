package team.gothaj.cloud.client;

import org.java_websocket.WebSocket;
import team.gothaj.cloud.share.Connection;
import team.gothaj.cloud.share.packet.Packet;

public class CloudConnection extends Connection {
    private volatile String sessionToken = "";

    public CloudConnection(WebSocket webSocket) {
        super(webSocket, new CloudCipher());
    }

    @Override
    protected String getSessionToken() {
        return sessionToken;
    }

    @Override
    protected void setSessionToken(String token) {
        this.sessionToken = token != null ? token : "";
    }

    @Override
    protected void handlePacket(Packet packet, int requestId, String receivedToken) {
       // System.out.println("Client Received packet: " + packet);

        if (packet.getType().equals("AUTH") && packet.has("token")) {
            String token = packet.getString("token");
            if (token != null && !token.isEmpty()) {
                setSessionToken(token);
               // System.out.println("[ClientConnection] Session token received and stored: " + token);
            }
        }
        
        // Now call handlers - they will have access to the session token
        super.handlePacket(packet, requestId, receivedToken);
    }

    @Override
    public void onBinaryMessage(byte[] data) {
        super.onBinaryMessage(data);
    }
}
