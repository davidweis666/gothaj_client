package com.cortex.legacy.render.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/**
 * An offscreen colour buffer: one FBO with a single RGBA8 texture attachment.
 *
 * <p>No depth or stencil attachment is created — every Cortex effect pass is a
 * fullscreen blit that neither tests nor writes depth.
 */
public final class RenderTarget {

    public final int fbo;
    public final int texture;
    public final int width;
    public final int height;

    /**
     * The frame this target was last handed out on, maintained by {@link RenderTargetPool}
     * so it can delete sizes that have fallen out of use. Lives here rather than in a
     * parallel structure in the pool purely to keep the free-list scan allocation-free.
     */
    int lastUsedFrame;

    private RenderTarget(int fbo, int texture, int width, int height) {
        this.fbo = fbo;
        this.texture = texture;
        this.width = width;
        this.height = height;
    }

    public static RenderTarget create(int width, int height) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // Kawase relies on bilinear filtering between its down/up passes, and clamping keeps
        // the kernel from wrapping bright edge pixels around to the opposite side.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, texture, 0);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL30.glDeleteFramebuffers(fbo);
            GL11.glDeleteTextures(texture);
            throw new IllegalStateException(
                    "Cortex: incomplete framebuffer (" + width + "x" + height + "), status 0x"
                            + Integer.toHexString(status));
        }

        return new RenderTarget(fbo, texture, width, height);
    }

    /** Binds this target for drawing and sets the viewport to cover it. */
    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, width, height);
    }

    /**
     * Clears to fully transparent black. Alpha must be zero: anything composited back over
     * Minecraft's own framebuffer would otherwise punch a hole in the frame behind it.
     */
    public void clear() {
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    public void dispose() {
        GL30.glDeleteFramebuffers(fbo);
        GL11.glDeleteTextures(texture);
    }
}
