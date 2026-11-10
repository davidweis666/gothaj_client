package com.cortex.legacy.image;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import com.cortex.legacy.render.gl.GlContext;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * A GPU-resident texture.
 *
 * <p>Deliberately a raw GL texture rather than a vanilla {@code DynamicTexture}: Cortex
 * binds textures itself inside its own shader passes, and routing through Minecraft's
 * texture manager would only add a {@code ResourceLocation} indirection and a
 * {@code GlStateManager} cache that the engine then has to fight.
 */
public final class Image {

    /** Widest mip chain that still helps; beyond this the levels are a pixel or two wide. */
    private static final int MAX_MIP_LEVEL = 8;

    public final int texture;
    public final int width;
    public final int height;

    Image(int texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    /** Uploads an AWT image with mipmaps, ready to be drawn at any size. */
    public static Image upload(BufferedImage source) {
        return upload(source, true);
    }

    /**
     * Uploads an AWT image, converting it to straight RGBA.
     *
     * @param mipmap build a mip chain. Wanted for anything that will be scaled down — a
     *               220px animation drawn into a 64px box shimmers badly without it —
     *               but must be off for distance-field atlases, where averaging texels
     *               between levels destroys the field the shader decodes.
     */
    public static Image upload(BufferedImage source, boolean mipmap) {
        int width = source.getWidth();
        int height = source.getHeight();

        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        bleedColorIntoTransparency(pixels, width, height);

        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int argb : pixels) {
            buffer.put((byte) ((argb >> 16) & 0xFF));
            buffer.put((byte) ((argb >> 8) & 0xFF));
            buffer.put((byte) (argb & 0xFF));
            buffer.put((byte) ((argb >>> 24) & 0xFF));
        }
        buffer.flip();

        try {
            return upload(buffer, width, height, mipmap);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    /** Uploads tightly packed RGBA bytes. */
    public static Image upload(ByteBuffer rgba, int width, int height) {
        return upload(rgba, width, height, false);
    }

    public static Image upload(ByteBuffer rgba, int width, int height, boolean mipmap) {
        GlContext.requireCurrent("uploading a " + width + "x" + height + " texture");

        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);

        if (mipmap) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, MAX_MIP_LEVEL);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return new Image(texture, width, height);
    }

    /**
     * Copies colour outward into fully transparent texels.
     *
     * <p>A transparent texel still carries an RGB value, and in an indexed format like GIF
     * that value is usually black. Bilinear filtering and mipmap generation both average
     * neighbouring texels <em>including</em> the invisible ones, which draws that black
     * back in as a dark halo around every edge. Growing the visible colour into the
     * transparent margin first gives those averages something sensible to read, and costs
     * nothing at draw time.
     *
     * <p>Skipped entirely when the image is fully opaque, which most are.
     */
    private static void bleedColorIntoTransparency(int[] pixels, int width, int height) {
        boolean hasTransparency = false;
        for (int argb : pixels) {
            if ((argb >>> 24) == 0) {
                hasTransparency = true;
                break;
            }
        }
        if (!hasTransparency) return;

        // Which texels carry a colour worth copying. This has to be tracked apart from the
        // alpha channel: a filled texel stays fully transparent, so alpha alone could never
        // tell the next round that it now has a colour to pass on.
        boolean[] hasColor = new boolean[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            hasColor[i] = (pixels[i] >>> 24) != 0;
        }

        // Two rounds reach two texels out, which covers bilinear sampling plus the first
        // couple of mip levels — past that the levels are too coarse for a halo to show.
        for (int pass = 0; pass < 2; pass++) {
            boolean[] filled = new boolean[pixels.length];
            boolean spread = false;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    if (hasColor[index]) continue;

                    int r = 0, g = 0, b = 0, samples = 0;

                    for (int dy = -1; dy <= 1; dy++) {
                        int ny = y + dy;
                        if (ny < 0 || ny >= height) continue;

                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            if (nx < 0 || nx >= width || (dx == 0 && dy == 0)) continue;

                            int neighbour = ny * width + nx;
                            if (!hasColor[neighbour]) continue;

                            r += (pixels[neighbour] >> 16) & 0xFF;
                            g += (pixels[neighbour] >> 8) & 0xFF;
                            b += pixels[neighbour] & 0xFF;
                            samples++;
                        }
                    }

                    if (samples > 0) {
                        // Colour only — alpha stays at zero, so this can never make
                        // anything visible that was not already.
                        pixels[index] = ((r / samples) << 16) | ((g / samples) << 8) | (b / samples);
                        filled[index] = true;
                        spread = true;
                    }
                }
            }

            if (!spread) break;

            for (int i = 0; i < filled.length; i++) {
                if (filled[i]) hasColor[i] = true;
            }
        }
    }

    public float aspectRatio() {
        return height == 0 ? 1f : (float) width / height;
    }

    public void dispose() {
        GL11.glDeleteTextures(texture);
    }
}
