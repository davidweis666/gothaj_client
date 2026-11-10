package team.gothaj.client.api.util.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.lwjgl.BufferUtils;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.main.GothajClient;
import team.gothaj.cloud.client.Cloud;
import team.gothaj.cloud.share.packet.Packet;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class AvatarUtil implements Utils {

    private static final String FORUM_BASE = "https://forums.gothaj.top/";
    private static final String DEFAULT_AVATAR = FORUM_BASE + "images/default_avatar.png";
    private static final String[] AVATAR_EXTENSIONS = {"png", "jpg", "jpeg", "gif"};

    private static final Map<String, Integer> avatarCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> currentlyLoading = new ConcurrentHashMap<>();
    private static final HttpClient http = HttpClient.newHttpClient();

    public static int loadAvatarTexture(String uid) {
        if (uid == null || uid.isEmpty()) {
            return loadDefaultAvatar();
        }

        String cacheKey = "avatar_" + uid;
        if (avatarCache.containsKey(cacheKey)) {
            return avatarCache.get(cacheKey);
        }

        if (currentlyLoading.getOrDefault(cacheKey, false)) {
            return avatarCache.getOrDefault("default_avatar", -1);
        }

        currentlyLoading.put(cacheKey, true);

        new Thread(() -> {
            BufferedImage image = tryLoadUserAvatar(uid);
            if (image == null) {
                try {
                    image = ImageIO.read(new URL(DEFAULT_AVATAR));
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
            final BufferedImage finalImage = image;
            mc.addScheduledTask(() -> {
                int textureId = finalImage != null ? createTextureFromImage(finalImage) : -1;
                avatarCache.put(cacheKey, textureId);
                currentlyLoading.put(cacheKey, false);
            });
        }, "Avatar-Downloader-" + uid).start();

        return avatarCache.getOrDefault("default_avatar", -1);
    }

    private static BufferedImage tryLoadUserAvatar(String uid) {
        for (String ext : AVATAR_EXTENSIONS) {
            String avatarUrl = FORUM_BASE + "uploads/avatars/avatar_" + uid + "." + ext;
            try {
                BufferedImage image = ImageIO.read(new URL(avatarUrl));
                if (image != null) {
                    return image;
                }
            } catch (IOException e) {
                // continue
            }
        }
        return null;
    }

    private static int loadDefaultAvatar() {
        String cacheKey = "default_avatar";
        if (avatarCache.containsKey(cacheKey)) {
            return avatarCache.get(cacheKey);
        }

        if (currentlyLoading.getOrDefault(cacheKey, false)) {
            return -1;
        }

        currentlyLoading.put(cacheKey, true);

        new Thread(() -> {
            try {
                BufferedImage image = ImageIO.read(new URL(DEFAULT_AVATAR));
                mc.addScheduledTask(() -> {
                    if (!avatarCache.containsKey(cacheKey)) {
                        int textureId = createTextureFromImage(image);
                        avatarCache.put(cacheKey, textureId);
                    }
                    currentlyLoading.put(cacheKey, false);
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
                currentlyLoading.put(cacheKey, false);
            }
        }, "Avatar-Default-Loader").start();

        return -1;
    }

    private static int createTextureFromImage(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }

        buffer.flip();

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(),
                0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        return textureId;
    }

    /**
     * Draw Selected Forum Avatar
     */
    public static void drawForumAvatar(String uid, float x, float y, float width, float height,
                                       float radius, float alpha) {
        int avatarTexture = loadAvatarTexture(uid);

        if (avatarTexture != -1) {
            glBindTexture(GL_TEXTURE_2D, avatarTexture);
            RoundedUtil.drawRoundTextured(x, y, width, height, radius, alpha);
        }
    }

    /**
     * Draw Own Forum Avatar
     */
    public static void drawForumAvatar(float x, float y, float width, float height,
                                       float radius, float alpha) {
        drawForumAvatar(GothajClient.getInstance().getUser().getUid(), x, y, width, height, radius, alpha);
    }

    public static ResourceLocation prepareServerIcon(ServerData server) {
        ResourceLocation serverIcon = new ResourceLocation("servers/" + server.serverIP + "/icon");
        DynamicTexture dyn = (DynamicTexture) mc.getTextureManager().getTexture(serverIcon);
        String b64 = server.getBase64EncodedIconData();
        if (b64 == null) {
            return null;
        }
        ByteBuf buf = Unpooled.copiedBuffer(b64, Charsets.UTF_8);
        ByteBuf bufDec = Base64.decode(buf);
        try {
            BufferedImage img = TextureUtil.readBufferedImage(new ByteBufInputStream(bufDec));
            Validate.validState(img.getWidth() == 64, "Must be 64 pixels wide");
            Validate.validState(img.getHeight() == 64, "Must be 64 pixels high");
            if (dyn == null) {
                dyn = new DynamicTexture(img.getWidth(), img.getHeight());
                mc.getTextureManager().loadTexture(serverIcon, dyn);
            }
            int[] data = dyn.getTextureData();
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), data, 0, img.getWidth());
            dyn.updateDynamicTexture();
            return serverIcon;
        } catch (Throwable t) {
            server.setBase64EncodedIconData(null);
            return null;
        } finally {
            buf.release();
            bufDec.release();
        }
    }

    public static String getUID(String username) {
        try {

            String url = "https://forums.gothaj.top/uidcheck.php";
            String key = null;

            try {
                key = Cloud.getInstance().sendRequestSync(
                                Packet.builder()
                                        .type("GET_VALUE")
                                        .putString("name", "UID_KEY")
                                        .build(),
                                10000L)
                        .getString("value");
            }catch (Exception e) {
                return null;
            }

            String form = "key=" + URLEncoder.encode(key, StandardCharsets.UTF_8) +
                    "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.get("ok").getAsBoolean()) {
                    return json.get("uid").getAsString();
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error fetching UID: " + e.getMessage());
            return null;
        }
    }
}
