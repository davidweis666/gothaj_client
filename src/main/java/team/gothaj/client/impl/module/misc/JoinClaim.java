package team.gothaj.client.impl.module.misc;

import com.google.gson.JsonObject;
import net.minecraft.network.play.server.S40PacketDisconnect;
import org.jetbrains.annotations.NotNull;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Sends Mojang session join after handshake; non-blocking with cleanup.
 */
public class JoinClaim extends Module {

    public JoinClaim() {
        super("JoinClaim", "", ModuleCategory.MISC);
    }

    @Listen
    public void onUpdate(PacketEvent event) {
        if (event.getPacket() instanceof S40PacketDisconnect) {
            this.setToggled(false);
            return;
        }
        if (mc.thePlayer == null) return;

        if (mc.thePlayer.ticksExisted > 20) {
            final String accessToken = mc.getSession().getToken();
            final String selectedProfile = mc.getSession().getPlayerID();
            final String serverId = obtainServerId();

            if (accessToken == null || selectedProfile == null || serverId == null) return;

            CompletableFuture
                    .runAsync(() -> joinSession(accessToken, selectedProfile, serverId))
                    .exceptionally(ex -> {
                        return null;
                    });
            this.setToggled(false);
        }
    }

    private static void joinSession(String accessToken, String selectedProfile, String serverId) {
        HttpURLConnection connection = null;
        try {
            connection = makeJoinConnection();

            JsonObject body = new JsonObject();
            body.addProperty("accessToken", accessToken);
            body.addProperty("selectedProfile", selectedProfile);
            body.addProperty("serverId", serverId);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                    StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NotNull
    private static HttpURLConnection makeJoinConnection() throws IOException {
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/join");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        return conn;
    }

    private static String obtainServerId() {
        return "31531515";
    }
}
