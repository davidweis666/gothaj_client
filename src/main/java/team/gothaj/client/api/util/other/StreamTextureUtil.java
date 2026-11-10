package team.gothaj.client.api.util.other;

import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class StreamTextureUtil {

    private static final Map<String, Integer> CACHE = new HashMap<>();

    public static int get(String url) {
        if (CACHE.containsKey(url)) {
            return CACHE.get(url);
        }

        try {
            int textureId = loadTexture(url);
            CACHE.put(url, textureId);
            return textureId;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static int loadTexture(String urlString) throws Exception {
        URL url = URI.create(urlString).toURL();
        InputStream stream = url.openStream();
        BufferedImage image = ImageIO.read(stream);

        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];

                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                buffer
        );

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return textureId;
    }

    public static void clear() {
        for (int textureId : CACHE.values()) {
            GL11.glDeleteTextures(textureId);
        }
        CACHE.clear();
    }
}