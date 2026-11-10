package com.cortex.legacy.effect.blur;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import com.cortex.legacy.render.gl.RenderTarget;
import com.cortex.legacy.render.gl.RenderTargetPool;
import com.cortex.legacy.render.gl.ShaderProgram;

/**
 * Dual-Kawase blur.
 *
 * <p>Runs a chain of halving downsample passes followed by the matching doubling upsample
 * passes. Because each additional iteration doubles the effective radius while working on a
 * quarter of the pixels, a wide frosted-glass blur costs a small fraction of what an
 * equivalently wide Gaussian would.
 */
public final class KawaseBlur {

    /** Beyond this the chain reaches a 1-pixel target and further passes do nothing useful. */
    public static final int MAX_ITERATIONS = 6;

    private final ShaderProgram down;
    private final ShaderProgram up;
    private final RenderTargetPool pool;

    /**
     * The down/up chain's intermediate levels. Held as a field and reused: a fresh array
     * per call would be a small allocation on a path that runs for every panel, every frame.
     */
    private final RenderTarget[] chain = new RenderTarget[MAX_ITERATIONS];

    public KawaseBlur(ShaderProgram down, ShaderProgram up, RenderTargetPool pool) {
        this.down = down;
        this.up = up;
        this.pool = pool;
    }

    /** Maps a radius in device pixels onto a sensible number of down/up passes. */
    public static int iterationsFor(float radiusPx) {
        int iterations = (int) Math.floor(Math.log(Math.max(radiusPx, 2f)) / Math.log(2.0));
        return Math.max(1, Math.min(MAX_ITERATIONS, iterations));
    }

    /**
     * Blurs {@code sourceTexture} and returns a pooled target holding the result at
     * roughly half of {@code width} x {@code height}.
     *
     * <p>The result is deliberately half resolution; sample it by normalised coordinate and
     * let the hardware scale it back up.
     *
     * <p>The returned target belongs to the pool and is reclaimed by
     * {@link RenderTargetPool#endFrame()} — callers must not dispose it.
     *
     * <p>Expects a VAO to already be bound: the passes draw a generated fullscreen triangle
     * and read no vertex attributes, but a bound VAO is still required to issue the draw.
     */
    public RenderTarget blur(int sourceTexture, int width, int height, int iterations) {
        iterations = Math.max(1, Math.min(MAX_ITERATIONS, iterations));

        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        // Downsample chain. Each pass reads the previous level and writes one half its size.
        int srcTexture = sourceTexture;
        int srcWidth = width;
        int srcHeight = height;

        down.bind();
        down.set("u_Texture", 0);
        down.set("u_Offset", 1f);

        for (int i = 0; i < iterations; i++) {
            RenderTarget target = pool.acquire(Math.max(1, srcWidth / 2), Math.max(1, srcHeight / 2));
            chain[i] = target;

            pass(down, srcTexture, target, srcWidth, srcHeight);

            srcTexture = target.texture;
            srcWidth = target.width;
            srcHeight = target.height;
        }

        // Upsample chain, walking back down the levels we just built.
        up.bind();
        up.set("u_Texture", 0);
        up.set("u_Offset", 1f);

        for (int i = iterations - 2; i >= 0; i--) {
            RenderTarget target = chain[i];
            pass(up, srcTexture, target, srcWidth, srcHeight);

            srcTexture = target.texture;
            srcWidth = target.width;
            srcHeight = target.height;
        }

        // Stops at half resolution rather than running one more pass back to full size.
        // That last pass would be the single most expensive one in the chain — a
        // fullscreen write at native resolution — and it adds nothing a viewer can see,
        // because the content it carries has already had every detail finer than a couple
        // of pixels averaged out of it. Consumers sample by normalised screen coordinate,
        // so the smaller target is invisible to them and costs one less full-size texture.
        return chain[0];
    }

    /** Draws one fullscreen triangle from {@code sourceTexture} into {@code target}. */
    private void pass(ShaderProgram program, int sourceTexture, RenderTarget target,
                      int sourceWidth, int sourceHeight) {

        program.set("u_HalfPixel", 0.5f / sourceWidth, 0.5f / sourceHeight);

        target.bind();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceTexture);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    /** Releases the two pass programs. The pool is owned by the caller. */
    public void dispose() {
        down.dispose();
        up.dispose();
    }
}
