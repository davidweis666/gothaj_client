package team.gothaj.client.api.util.other;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.ByteBuffer;

public class SkinUtil implements Utils {


    public static BufferedImage makeHead(String skinUrl) {
        try {
            return ImageIO.read(new URL(skinUrl));
        } catch (Exception e) {
            Logger.log("Failed to read skin url. (" + skinUrl + ")");
            return null;
        }
    }

    public static ByteBuffer bufferedImageToByteBuffer(String skinUrl) {
        BufferedImage image = makeHead(skinUrl);
        assert image != null;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte)((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte)((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte)(pixel & 0xFF));         // Blue
                buffer.put((byte)((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();
        return buffer;
    }



    public static int uploadHeadTexture(ByteBuffer buffer, int width, int height) {
        int texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        return texId;
    }



}
