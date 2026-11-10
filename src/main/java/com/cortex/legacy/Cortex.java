package com.cortex.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import team.gothaj.client.api.util.Logger;
import com.cortex.legacy.draw.Corners;
import com.cortex.legacy.draw.Gradient;
import com.cortex.legacy.draw.Region;
import com.cortex.legacy.effect.blur.KawaseBlur;
import com.cortex.legacy.effect.shader.CustomShaders;
import com.cortex.legacy.effect.shader.UniformBlock;
import com.cortex.legacy.image.AnimatedImage;
import com.cortex.legacy.image.Image;
import com.cortex.legacy.image.ImageLoader;
import com.cortex.legacy.render.batch.QuadBatcher;
import com.cortex.legacy.render.batch.ShapeBatcher;
import com.cortex.legacy.render.gl.GlStateSnapshot;
import com.cortex.legacy.render.gl.RenderTarget;
import com.cortex.legacy.render.gl.RenderTargetPool;
import com.cortex.legacy.render.gl.ShaderProgram;
import com.cortex.legacy.text.FontManager;
import com.cortex.legacy.text.TextStyle;
import com.cortex.legacy.text.font.BakedFont;
import com.cortex.legacy.text.font.Glyph;
import com.cortex.legacy.world.WorldDraw;

import java.awt.Color;

/**
 * Gothaj's 2D rendering engine: a shader-driven, batched replacement for the immediate-mode
 * helpers in {@code RenderUtil} and {@code ShaderUtil}.
 *
 * <h2>Using it</h2>
 * Draw from any {@code RenderOverlayEvent} handler. Coordinates are logical (GUI-scaled)
 * pixels and colours are packed ARGB, matching the rest of the client:
 * <pre>{@code
 * Cortex.blur(Region.of(10, 10, 160, 90), Corners.all(8), 12f, 0x50000000);
 * Cortex.gradientRect(panel, Corners.all(8), Gradient.vertical(0xFF2E86DE, 0xFF6C5CE7));
 * Cortex.text(18, 18, "Hello", TextStyle.of("main", 16, 0xFFFFFFFF));
 * }</pre>
 *
 * <h2>Fonts and images</h2>
 * Register once, then draw by name. Fonts bake on a background thread and fall back to
 * Minecraft's bitmap font until ready, so the first frame never stalls:
 * <pre>{@code
 * Cortex.fonts().register("main", "fonts/Inter-Medium.ttf");
 * Cortex.image("img/logo.png", Region.of(10, 10, 64, 64));
 * }</pre>
 *
 * <h2>How it fits together</h2>
 * {@link #beginFrame} and {@link #endFrame} bracket the overlay render from
 * {@code GuiIngame}, so callers never manage GL state themselves. Draws accumulate into
 * batches and are submitted when the shader or texture has to change — a HUD of mixed
 * shapes still costs one draw call, and a run of glyphs from one atlas costs one more.
 *
 * <h2>Notes for future work</h2>
 * Not yet implemented: scissor clipping, bloom, and text layout beyond single lines
 * (wrapping, bidi, and codepoints outside printable ASCII).
 */
public final class Cortex {

    private Cortex() {
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** Which kind of geometry the open batch holds, so a change can flush it. */
    private enum Batch {
        NONE, SHAPE, IMAGE, TEXT,
        /** Minecraft's own font renderer holds GL; nothing of ours is queued. */
        VANILLA_TEXT
    }

    private static boolean initialised;
    private static boolean failed;

    private static ShapeBatcher shapes;
    private static QuadBatcher quads;
    private static RenderTargetPool pool;
    private static KawaseBlur kawase;

    private static ShaderProgram shapeProgram;
    private static ShaderProgram blitProgram;
    private static ShaderProgram imageProgram;
    private static ShaderProgram msdfProgram;

    private static final FontManager fonts = new FontManager();
    private static final ImageLoader images = new ImageLoader();
    private static final CustomShaders customShaders = new CustomShaders();

    /**
     * The world pass. Replaced rather than reused on teardown, because disposing it frees
     * the off-heap buffers its batchers were built around.
     */
    private static WorldDraw world = new WorldDraw();

    /**
     * Tracked apart from {@link #failed} so the two passes fail independently: a driver that
     * cannot compile the world shader has no bearing on the HUD, and losing the HUD to a
     * broken ESP box would be a poor trade.
     */
    private static boolean worldFailed;

    /** Why the world pass switched itself off, for {@link #worldStatus()}. */
    private static String worldFailure;

    /** Bound while running effect passes, which generate their geometry from gl_VertexID. */
    private static int effectVao;

    private static Batch batch = Batch.NONE;
    private static int batchTexture;
    private static BakedFont batchFont;

    private static float viewportWidth;
    private static float viewportHeight;
    private static int scaleFactor = 1;
    private static int frameWidthPx;
    private static int frameHeightPx;

    private static boolean inFrame;

    /** Whether this frame has actually taken GL, which only happens once something draws. */
    private static boolean frameStarted;

    /**
     * Whether {@link #hostState} holds a capture that still has to be put back.
     *
     * <p>Tracked separately from {@link #frameStarted} because the two can disagree: setup
     * captures the state before it finishes, so a failure in between leaves state owed with
     * the frame never having started. Restoring is driven off this rather than off
     * {@code frameStarted} so that case cannot leak Minecraft's state.
     */
    private static boolean hostStateCaptured;

    private static final long startedAtNanos = System.nanoTime();

    /** Wall clock for this frame, shared by every animated gradient and custom effect. */
    private static float frameTimeSeconds;

    /** The state Minecraft was in when the frame began, restored when it ends. */
    private static final GlStateSnapshot hostState = new GlStateSnapshot();


    // ------------------------------------------------------------------
    // Registries
    // ------------------------------------------------------------------

    /** Registers and looks up MSDF fonts. */
    public static FontManager fonts() {
        return fonts;
    }

    /** Loads and caches images and animations. */
    public static ImageLoader images() {
        return images;
    }

    /** Registers custom GLSL effects. See {@link CustomShaders} for the contract. */
    public static CustomShaders shaders() {
        return customShaders;
    }

    /**
     * The world pass — boxes, tracers and billboards drawn among the blocks.
     *
     * <p>Draws through this are only accepted from a {@code RenderDimensionEvent} handler,
     * where {@link #beginWorld} has opened the pass; everywhere else they are ignored, the
     * same way 2D draws are outside the overlay. See {@link WorldDraw} for the API.
     */
    public static WorldDraw world() {
        return world;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Marks the engine ready to initialise. GL objects are created on the first frame
     * rather than here, so start-up ordering can't leave us compiling shaders before the
     * context is current.
     */
    public static void init() {
        // Re-initialising without tearing down first would abandon the previous set of
        // programs, buffers and textures with no handle left to delete them by.
        disposeRenderResources();

        failed = false;
    }

    /**
     * Checks the driver can actually run the engine before anything is built.
     *
     * <p>Without this the first symptom of an old context is a shader compile failure every
     * frame, which says nothing useful about the real cause. 3.3 is the bar because the
     * shaders are written against GLSL 330 — the same level the client's previous engine
     * required, so this rules out no hardware that already worked.
     */
    private static void requireCapabilities() {
        GLCapabilities caps = GL.getCapabilities();

        if (!caps.OpenGL33) {
            throw new IllegalStateException("Cortex needs OpenGL 3.3 or newer; this driver reports "
                    + GL11.glGetString(GL11.GL_VERSION)
                    + " (GLSL " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION) + ")");
        }
    }

    private static void createResources() {
        requireCapabilities();

        shapeProgram = ShaderProgram.load("shape");
        blitProgram = ShaderProgram.load("blit");
        imageProgram = ShaderProgram.load("textured", "image");
        msdfProgram = ShaderProgram.load("textured", "msdf");

        ShaderProgram down = ShaderProgram.load("fullscreen", "kawase_down");
        ShaderProgram up = ShaderProgram.load("fullscreen", "kawase_up");

        shapes = new ShapeBatcher();
        quads = new QuadBatcher();
        pool = new RenderTargetPool();
        kawase = new KawaseBlur(down, up, pool);

        effectVao = GL30.glGenVertexArrays();

        initialised = true;
        Logger.log("Cortex engine ready");
    }

    /**
     * Opens a frame. Cheap by design: nothing is built and no GL state is touched until
     * something actually asks to draw.
     *
     * <p>Most frames on most configurations draw nothing through the engine — every module
     * that would use it is off. Deferring the setup means those frames cost a couple of
     * field writes instead of a state capture, a framebuffer bind and a dozen state calls.
     */
    public static void beginFrame(ScaledResolution resolution) {
        if (failed) return;

        scaleFactor = Math.max(1, resolution.getScaleFactor());
        viewportWidth = resolution.getScaledWidth();
        viewportHeight = resolution.getScaledHeight();

        batch = Batch.NONE;
        inFrame = true;
        frameStarted = false;
    }

    /**
     * Performs the setup {@link #beginFrame} deferred, on the first draw of the frame.
     *
     * @return whether the engine is ready to draw
     */
    private static boolean startFrame() {
        if (frameStarted) return true;
        if (failed) return false;

        try {
            if (!initialised) {
                createResources();
            }

            int widthPx = mc.displayWidth;
            int heightPx = mc.displayHeight;

            // Pooled targets are sized against the framebuffer, so a resize invalidates
            // every one of them at once.
            if (widthPx != frameWidthPx || heightPx != frameHeightPx) {
                frameWidthPx = widthPx;
                frameHeightPx = heightPx;
                pool.dispose();
            }

            // Everything the engine is about to change is recorded first, so the frame can
            // end by putting it back rather than by guessing at sensible defaults. The flag
            // is set in the same breath: from here on the state is owed back even if the
            // rest of this method throws.
            hostState.capture();
            hostStateCaptured = true;

            bindHostFramebuffer();
            applyDrawState();

            // Gradients animate off the clock. Sampling it once here keeps every shape in
            // the frame on the same phase and keeps the draw path free of time lookups.
            frameTimeSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000f;
            Gradient.setFrameTime(frameTimeSeconds);

            frameStarted = true;
            return true;
        } catch (Throwable t) {
            fail("initialise", t);
            return false;
        }
    }

    /**
     * Submits anything still batched and hands GL back to Minecraft.
     *
     * <p>Safe to call unconditionally, and safe to call after the engine has failed: the
     * hand-back is the one part of the frame that must happen no matter what went wrong
     * inside it, because the alternative is Minecraft rendering the rest of the session
     * with depth testing and culling still switched off.
     */
    public static void endFrame() {
        if (!inFrame && !hostStateCaptured) return;

        inFrame = false;

        try {
            // A frame nobody drew into never took GL, so there is nothing to submit.
            if (frameStarted) {
                flush();
            }
        } catch (Throwable t) {
            fail("flush", t);
        } finally {
            frameStarted = false;

            if (pool != null) {
                pool.endFrame();
            }

            if (hostStateCaptured) {
                hostStateCaptured = false;
                restoreMinecraftState();
            }
        }
    }

    // ------------------------------------------------------------------
    // World pass
    // ------------------------------------------------------------------

    /**
     * Opens the world pass, around the point the dimension has finished drawing.
     *
     * <p>Already wired in {@code EntityRenderer}; drawing code never calls this. Unlike the
     * overlay frame there is no deferring the setup, because the camera matrices this reads
     * are only current here — by the time a module asks to draw, the moment has passed.
     */
    /**
     * Records the world camera. Wired immediately after {@code setupCameraTransform}, which
     * is the only point in the frame where the modelview stack is known to hold it — see
     * {@link WorldDraw#captureCamera()} for what goes wrong when it is read later.
     */
    public static void captureWorldCamera() {
        if (worldFailed) return;

        try {
            world.captureCamera();
        } catch (Throwable t) {
            failWorld("read the camera", t);
        }
    }

    public static void beginWorld() {
        if (worldFailed) return;

        try {
            world.begin();
        } catch (Throwable t) {
            failWorld("initialise", t);

            // begin() captures the state to restore partway through, so it may already be
            // owed back even though the pass never opened.
            try {
                world.end();
            } catch (Throwable ignored) {
                // Nothing useful left to do; the original failure is the one that matters.
            }
        }
    }

    /** Submits anything the world pass queued and hands GL back to Minecraft. */
    public static void endWorld() {
        if (worldFailed) return;

        try {
            world.end();
        } catch (Throwable t) {
            failWorld("submit", t);
        }
    }

    /** Disables the world pass after an error, leaving the overlay running. */
    private static void failWorld(String stage, Throwable t) {
        worldFailed = true;
        worldFailure = stage + ": " + rootMessage(t);

        // Through the client logger as well as stderr. A pass that has switched itself off
        // draws nothing and says nothing, which is indistinguishable from a module that
        // never asked to draw — and stderr alone is easy to be unaware of.
        Logger.log("Cortex: world pass disabled after failing to " + stage);
        System.err.println("Cortex: world pass disabled after failing to " + stage);
        t.printStackTrace();
    }

    /** The deepest message in the chain, which is the one that says what actually broke. */
    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        return message == null ? root.getClass().getSimpleName() : message;
    }

    /**
     * What the world pass did last frame, or why it is not running — for the debug module.
     *
     * <p>Reads as one of: the failure that disabled it, {@code "pass never opened"} if the
     * bracket is not being reached at all, or a draw-call and vertex count.
     */
    public static String worldStatus() {
        if (worldFailed) return "FAILED " + worldFailure;
        return world.status();
    }

    /**
     * Whether the camera matrix the world pass is drawing with is usable.
     *
     * <p>Separate from {@link #worldStatus()} because a bad matrix is the one failure that
     * leaves every other indicator healthy — geometry queued, draw calls issued, nothing
     * thrown — while collapsing every vertex onto the same point.
     */
    public static String worldMatrix() {
        return world.matrixVerdict();
    }

    /** One row of the view-projection matrix, for when the verdict alone is not enough. */
    public static String worldMatrixRow(int row) {
        return world.matrixRow(row);
    }

    /**
     * Binds the framebuffer Minecraft is currently rendering into.
     *
     * <p>Deliberately re-resolved on every call: 1.8.9 draws into its own named FBO rather
     * than FBO 0, and reallocates that object on resize, so a cached id would eventually
     * point at nothing and the whole overlay would land on the raw window surface instead.
     */
    private static void bindHostFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, hostFramebuffer());
        GL11.glViewport(0, 0, frameWidthPx, frameHeightPx);
    }

    /**
     * The framebuffer object Minecraft is drawing into, or {@code 0} for the window itself.
     *
     * <p>Falling back to 0 matters when the game is not using a framebuffer at all — with
     * the FBO setting off, or under a mod that renders straight to the back buffer. Binding
     * a stale id there would send the whole overlay somewhere invisible.
     */
    private static int hostFramebuffer() {
        Framebuffer framebuffer = mc.getFramebuffer();
        return framebuffer == null || framebuffer.framebufferObject < 0
                ? 0 : framebuffer.framebufferObject;
    }

    /**
     * The texture holding the frame behind the overlay, or {@code 0} if there isn't one.
     *
     * <p>Backdrop effects need to read what has already been drawn, which is only possible
     * when the game renders through a framebuffer. Without one they are skipped rather than
     * sampling an invalid texture.
     */
    private static int hostColorTexture() {
        Framebuffer framebuffer = mc.getFramebuffer();
        return framebuffer == null || framebuffer.framebufferObject < 0
                ? 0 : framebuffer.framebufferTexture;
    }

    /** Sets the known-good state every Cortex draw expects. */
    private static void applyDrawState() {
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glColorMask(true, true, true, true);
    }

    /**
     * Hands GL back to Minecraft exactly as it was found.
     *
     * <p>See {@link GlStateSnapshot} for why this restores the captured state rather than
     * setting defaults — in short, GlStateManager's cache silently swallows any "repair"
     * call that agrees with what it already believes.
     */
    private static void restoreMinecraftState() {
        hostState.restore();

        // Putting the real state back is not quite enough on its own. Minecraft's font
        // renderer runs mid-frame on the fallback text path and drives GlStateManager
        // directly, leaving its cache describing the font's state rather than the state
        // just restored above. Pushing the restored values back through GlStateManager
        // realigns it — and because reality already holds these values, the call is a
        // harmless no-op whenever the cache was right all along.
        GlStateManager.bindTexture(hostState.boundTexture());

        if (hostState.alphaTest()) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }
    }

    /**
     * Disables the engine after an error rather than throwing every frame. One stack trace
     * is useful; sixty a second buries the log and tanks the frame rate.
     *
     * <p>Deliberately leaves {@code inFrame} alone. Clearing it here would make
     * {@link #endFrame} treat the frame as never opened and skip the state hand-back, so a
     * single failed draw would leave Minecraft rendering with the engine's state — depth
     * test and culling off — for the rest of the session. {@link #canDraw} already stops
     * further draws via {@code failed}, which is all that needs to happen immediately.
     */
    private static void fail(String stage, Throwable t) {
        failed = true;
        System.err.println("Cortex: disabled after failing to " + stage);
        t.printStackTrace();
    }

    // ------------------------------------------------------------------
    // Batch control
    // ------------------------------------------------------------------

    /**
     * Switches the open batch, submitting the previous one first.
     *
     * <p>This is what keeps draw order intact: geometry is only ever merged with geometry
     * that shares its shader and texture, and anything else forces a flush.
     */
    private static void begin(Batch kind, int texture, BakedFont font) {
        if (batch == kind && batchTexture == texture) return;

        flush();

        if (kind == Batch.VANILLA_TEXT) {
            // Minecraft's font renderer draws through the fixed-function pipeline and its
            // own state cache, so GL has to be handed back before it runs. Doing that here
            // rather than per string means a column of labels pays for the swap once.
            restoreMinecraftState();
            GlStateManager.enableTexture2D();
        }

        batch = kind;
        batchTexture = texture;
        batchFont = font;
    }

    /** Submits whatever is currently batched. */
    private static void flush() {
        switch (batch) {
            case SHAPE -> {
                if (!shapes.isEmpty()) {
                    shapeProgram.bind();
                    shapeProgram.set("u_Viewport", viewportWidth, viewportHeight);
                    shapes.flush();
                }
            }
            case IMAGE -> {
                if (!quads.isEmpty()) {
                    imageProgram.bind();
                    imageProgram.set("u_Viewport", viewportWidth, viewportHeight);
                    imageProgram.set("u_Texture", 0);
                    bindTexture(batchTexture);
                    quads.flush();
                }
            }
            case TEXT -> {
                if (!quads.isEmpty()) {
                    msdfProgram.bind();
                    msdfProgram.set("u_Viewport", viewportWidth, viewportHeight);
                    msdfProgram.set("u_Atlas", 0);
                    msdfProgram.set("u_AtlasSize", batchFont.atlasWidth(), batchFont.atlasHeight());
                    msdfProgram.set("u_PxRange", batchFont.pxRange());
                    bindTexture(batchTexture);
                    quads.flush();
                }
            }
            case VANILLA_TEXT -> {
                // Nothing to submit — the font renderer already drew. Just take GL back.
                bindHostFramebuffer();
                applyDrawState();
            }
            case NONE -> {
                // Nothing open.
            }
        }
        batch = Batch.NONE;
    }

    private static void bindTexture(int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    /**
     * Gate on every public draw method. Also performs the frame setup deferred by
     * {@link #beginFrame}, so the first draw pays for it and a silent frame pays nothing.
     */
    private static boolean canDraw() {
        return inFrame && !failed && startFrame();
    }

    /** {@link Color#getRGB()} is already packed ARGB, which is what the batchers store. */
    private static int argb(Color color) {
        return color == null ? 0 : color.getRGB();
    }

    // ------------------------------------------------------------------
    // Shapes
    // ------------------------------------------------------------------
    //
    // Every colour is a java.awt.Color. Packed-ARGB overloads sit alongside them for
    // interop with the client's existing int-based helpers such as Hud.getColor and
    // ColorUtil.setAlpha.

    public static void rect(Region region, Color color) {
        roundedRect(region, Corners.NONE, color);
    }

    public static void rect(Region region, int argb) {
        roundedRect(region, Corners.NONE, argb);
    }

    public static void rect(float x, float y, float w, float h, Color color) {
        roundedRect(Region.of(x, y, w, h), Corners.NONE, color);
    }

    public static void rect(float x, float y, float w, float h, int argb) {
        roundedRect(Region.of(x, y, w, h), Corners.NONE, argb);
    }

    public static void roundedRect(Region region, Corners corners, Color color) {
        roundedRect(region, corners, argb(color));
    }

    public static void roundedRect(Region region, Corners corners, int argb) {
        box(region, corners, ShapeBatcher.FILL, 0f, argb, null);
    }

    /** Fills the region with a two-stop gradient. */
    public static void gradientRect(Region region, Gradient gradient) {
        gradientRect(region, Corners.NONE, gradient);
    }

    public static void gradientRect(Region region, Corners corners, Gradient gradient) {
        box(region, corners, ShapeBatcher.FILL, 0f, gradient.start, gradient);
    }

    public static void outlineRect(Region region, float thickness, Color color) {
        outlineRoundedRect(region, Corners.NONE, thickness, argb(color));
    }

    public static void outlineRect(Region region, float thickness, int argb) {
        outlineRoundedRect(region, Corners.NONE, thickness, argb);
    }

    public static void outlineRoundedRect(Region region, Corners corners, float thickness, Color color) {
        outlineRoundedRect(region, corners, thickness, argb(color));
    }

    public static void outlineRoundedRect(Region region, Corners corners, float thickness, int argb) {
        box(region, corners, Math.max(thickness, 0.01f), 0f, argb, null);
    }

    public static void gradientOutline(Region region, Corners corners, float thickness, Gradient gradient) {
        box(region, corners, Math.max(thickness, 0.01f), 0f, gradient.start, gradient);
    }

    /**
     * Draws a soft halo around the region. Emit this before the shape it belongs to — the
     * glow covers only the area outside the boundary.
     *
     * @param radius how far the halo reaches beyond the edge, in logical pixels
     */
    public static void glow(Region region, Corners corners, float radius, Color color) {
        glow(region, corners, radius, argb(color));
    }

    public static void glow(Region region, Corners corners, float radius, int argb) {
        box(region, corners, ShapeBatcher.GLOW_ONLY, Math.max(radius, 0.01f), argb, null);
    }

    private static void box(Region region, Corners corners, float thickness, float glow,
                            int argb, Gradient gradient) {
        if (!canDraw() || region.w <= 0f || region.h <= 0f) return;

        begin(Batch.SHAPE, 0, null);

        // Clamped inline rather than through Corners.clampTo, which would allocate a
        // throwaway object for every shape in every frame.
        float limit = Corners.limitFor(region.w, region.h);
        shapes.box(region.centerX(), region.centerY(), region.w * 0.5f, region.h * 0.5f,
                Math.min(corners.tl, limit), Math.min(corners.tr, limit),
                Math.min(corners.br, limit), Math.min(corners.bl, limit),
                thickness, glow, argb, gradient);
    }

    public static void circle(float cx, float cy, float radius, Color color) {
        ellipse(cx, cy, radius, radius, argb(color));
    }

    public static void circle(float cx, float cy, float radius, int argb) {
        ellipse(cx, cy, radius, radius, argb);
    }

    public static void outlineCircle(float cx, float cy, float radius, float thickness, Color color) {
        outlineCircle(cx, cy, radius, thickness, argb(color));
    }

    public static void outlineCircle(float cx, float cy, float radius, float thickness, int argb) {
        if (!canDraw()) return;
        begin(Batch.SHAPE, 0, null);
        shapes.ellipse(cx, cy, radius, radius, Math.max(thickness, 0.01f), 0f, argb, null);
    }

    public static void ellipse(float cx, float cy, float rx, float ry, Color color) {
        ellipse(cx, cy, rx, ry, argb(color));
    }

    public static void ellipse(float cx, float cy, float rx, float ry, int argb) {
        if (!canDraw()) return;
        begin(Batch.SHAPE, 0, null);
        shapes.ellipse(cx, cy, rx, ry, ShapeBatcher.FILL, 0f, argb, null);
    }

    public static void gradientCircle(float cx, float cy, float radius, Gradient gradient) {
        if (!canDraw()) return;
        begin(Batch.SHAPE, 0, null);
        shapes.ellipse(cx, cy, radius, radius, ShapeBatcher.FILL, 0f, gradient.start, gradient);
    }

    public static void glowCircle(float cx, float cy, float radius, float glowRadius, Color color) {
        glowCircle(cx, cy, radius, glowRadius, argb(color));
    }

    public static void glowCircle(float cx, float cy, float radius, float glowRadius, int argb) {
        if (!canDraw()) return;
        begin(Batch.SHAPE, 0, null);
        shapes.ellipse(cx, cy, radius, radius, ShapeBatcher.GLOW_ONLY,
                Math.max(glowRadius, 0.01f), argb, null);
    }

    /** Draws a round-capped line of the given thickness. */
    public static void line(float x0, float y0, float x1, float y1, float thickness, Color color) {
        line(x0, y0, x1, y1, thickness, argb(color), null);
    }

    public static void line(float x0, float y0, float x1, float y1, float thickness, int argb) {
        line(x0, y0, x1, y1, thickness, argb, null);
    }

    public static void gradientLine(float x0, float y0, float x1, float y1,
                                    float thickness, Gradient gradient) {
        line(x0, y0, x1, y1, thickness, gradient.start, gradient);
    }

    private static void line(float x0, float y0, float x1, float y1,
                             float thickness, int argb, Gradient gradient) {
        if (!canDraw()) return;

        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0f) return;

        begin(Batch.SHAPE, 0, null);

        float half = Math.max(thickness, 0.01f) * 0.5f;

        // A capsule is a rotated box whose corner radii equal half its thickness, so the
        // shared rounded-box distance function already draws it.
        shapes.rotatedBox((x0 + x1) * 0.5f, (y0 + y1) * 0.5f,
                length * 0.5f + half, half, half,
                (float) Math.atan2(dy, dx),
                ShapeBatcher.FILL, 0f, argb, gradient);
    }

    public static void triangle(float x0, float y0, float x1, float y1, float x2, float y2, Color color) {
        triangle(x0, y0, x1, y1, x2, y2, argb(color));
    }

    public static void triangle(float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
        if (!canDraw()) return;
        begin(Batch.SHAPE, 0, null);
        shapes.triangle(x0, y0, x1, y1, x2, y2, ShapeBatcher.FILL, 0f, argb, null);
    }

    public static void outlineTriangle(float x0, float y0, float x1, float y1, float x2, float y2,
                                       float thickness, Color color) {
        outlineTriangle(x0, y0, x1, y1, x2, y2, thickness, argb(color));
    }

    public static void outlineTriangle(float x0, float y0, float x1, float y1, float x2, float y2,
                                       float thickness, int argb) {
        if (!canDraw()) return;
        begin(Batch.SHAPE, 0, null);
        shapes.triangle(x0, y0, x1, y1, x2, y2, Math.max(thickness, 0.01f), 0f, argb, null);
    }

    // ------------------------------------------------------------------
    // Images
    // ------------------------------------------------------------------

    /**
     * Draws an image by path, loading and caching it on first use.
     *
     * <p>The path may be written the way it looks on disk — {@code "gothaj/img/logo.png"} —
     * or relative to this client's assets, or as an absolute file path.
     */
    public static void image(String path, Region region) {
        image(images.load(path), region, 0xFFFFFFFF);
    }

    public static void image(String path, Region region, Color tint) {
        image(images.load(path), region, argb(tint));
    }

    public static void image(String path, Region region, int tint) {
        image(images.load(path), region, tint);
    }

    public static void image(Image image, Region region) {
        image(image, region, 0xFFFFFFFF);
    }

    public static void image(Image image, Region region, Color tint) {
        image(image, region, argb(tint));
    }

    /**
     * Draws an image, tinted by {@code argb}. A white image takes the tint as its colour;
     * pass {@code 0xFFFFFFFF} to leave it untouched.
     */
    public static void image(Image image, Region region, int argb) {
        if (!canDraw() || image == null) return;

        begin(Batch.IMAGE, image.texture, null);
        quads.quad(region.x, region.y, region.w, region.h, 0f, 0f, 1f, 1f, argb);
    }

    /** Draws an image with a gradient laid over it, animated if the gradient is. */
    public static void gradientImage(Image image, Region region, Gradient gradient) {
        if (!canDraw() || image == null) return;

        begin(Batch.IMAGE, image.texture, null);

        float animation = gradient.offset();
        quads.quad(region.x, region.y, region.w, region.h, 0f, 0f, 1f, 1f,
                gradient.start, gradient.end, animation, 1f + animation,
                gradient.axis == Gradient.Axis.VERTICAL);
    }

    /** Draws a sub-rectangle of an image, for sprite sheets. */
    public static void image(Image image, Region region, float u0, float v0, float u1, float v1, int argb) {
        if (!canDraw() || image == null) return;

        begin(Batch.IMAGE, image.texture, null);
        quads.quad(region.x, region.y, region.w, region.h, u0, v0, u1, v1, argb);
    }

    /** Draws the frame of an animation that should be showing now. */
    public static void image(AnimatedImage animation, Region region) {
        image(animation, region, 0xFFFFFFFF);
    }

    public static void image(AnimatedImage animation, Region region, int argb) {
        if (animation == null) return;
        image(animation.currentFrame(), region, argb);
    }

    /**
     * Draws an animation by path, loading and caching it on first use. Which frame shows is
     * derived from the wall clock, so nothing has to be ticked.
     */
    public static void animation(String path, Region region) {
        image(images.loadAnimated(path), region, 0xFFFFFFFF);
    }

    public static void animation(String path, Region region, Color tint) {
        image(images.loadAnimated(path), region, argb(tint));
    }

    public static void animation(String path, Region region, int argb) {
        image(images.loadAnimated(path), region, argb);
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    /**
     * Draws a run of text.
     *
     * <p>Falls back to Minecraft's bitmap font whenever the requested font has not finished
     * baking, so the first frames after registering a font still render.
     */
    public static void text(float x, float y, String text, TextStyle style) {
        if (!canDraw() || text == null || text.isEmpty()) return;

        BakedFont font = fonts.get(style.font);
        if (font == null) {
            vanillaText(x, y, text, style);
            return;
        }

        // Measuring walks the whole string, so it is skipped unless something needs it:
        // left-aligned text with no horizontal gradient never does.
        boolean needsWidth = style.align != TextStyle.Align.LEFT
                || (style.gradient != null && style.gradient.axis == Gradient.Axis.HORIZONTAL);

        float width = needsWidth
                ? font.width(text, style.size) + style.letterSpacing * (text.length() - 1)
                : 0f;

        float penX = switch (style.align) {
            case LEFT -> x;
            case CENTER -> x - width * 0.5f;
            case RIGHT -> x - width;
        };

        float baselineY = switch (style.baseline) {
            case ALPHABETIC -> y;
            case TOP -> y + font.ascent * style.size;
            case MIDDLE -> y + (font.ascent - font.lineHeight * 0.5f) * style.size;
        };

        if (style.shadow) {
            emitRun(penX + style.shadowOffset, baselineY + style.shadowOffset,
                    text, style, font, width, style.shadowColor, style.shadowColor);
        }

        int start = style.gradient != null ? style.gradient.start : style.color;
        int end = style.gradient != null ? style.gradient.end : style.color;

        emitRun(penX, baselineY, text, style, font, width, start, end);
    }

    /**
     * Lays out one run of glyphs.
     *
     * <p>A gradient is applied as a mask over the whole run rather than per glyph: the
     * horizontal position is measured against the run's total width so the ramp spans the
     * string, and a vertical one spans each glyph's own quad.
     */
    private static void emitRun(float penX, float baselineY, String text, TextStyle style,
                                BakedFont font, float runWidth, int startColor, int endColor) {

        begin(Batch.TEXT, font.texture(), font);

        boolean vertical = style.gradient != null
                && style.gradient.axis == Gradient.Axis.VERTICAL;
        float runStart = penX;

        // Sampled once for the whole run rather than per glyph: reading the clock inside
        // the loop would stagger the glyphs against each other by a few microseconds.
        float animation = style.gradient == null ? 0f : style.gradient.offset();

        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = font.glyph(text.charAt(i));

            if (glyph.hasInk()) {
                float x0 = penX + glyph.left * style.size;
                float y0 = baselineY + glyph.top * style.size;
                float w = (glyph.right - glyph.left) * style.size;
                float h = (glyph.bottom - glyph.top) * style.size;

                float gradStart;
                float gradEnd;
                if (style.gradient == null) {
                    gradStart = gradEnd = 0f;
                } else if (vertical) {
                    // Runs down each glyph's own quad.
                    gradStart = animation;
                    gradEnd = 1f + animation;
                } else {
                    // Measured against the whole run, so the ramp spans the string rather
                    // than restarting at every glyph.
                    gradStart = (runWidth <= 0f ? 0f : (x0 - runStart) / runWidth) + animation;
                    gradEnd = (runWidth <= 0f ? 0f : (x0 + w - runStart) / runWidth) + animation;
                }

                quads.quad(x0, y0, w, h, glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        startColor, endColor, gradStart, gradEnd, vertical);
            }

            penX += glyph.advance * style.size + style.letterSpacing;
        }
    }

    /** Convenience overloads using Minecraft's own font. */
    public static void text(float x, float y, String text, Color color) {
        text(x, y, text, argb(color), false);
    }

    public static void text(float x, float y, String text, Color color, boolean shadow) {
        text(x, y, text, argb(color), shadow);
    }

    public static void text(float x, float y, String text, int argb) {
        text(x, y, text, argb, false);
    }

    public static void text(float x, float y, String text, int argb, boolean shadow) {
        if (!canDraw() || text == null || text.isEmpty()) return;
        drawVanilla(x, y, text, argb, shadow);
    }

    private static void vanillaText(float x, float y, String text, TextStyle style) {
        float width = mc.fontRendererObj.getStringWidth(text);
        float drawX = switch (style.align) {
            case LEFT -> x;
            case CENTER -> x - width * 0.5f;
            case RIGHT -> x - width;
        };
        drawVanilla(drawX, y, text, style.color, style.shadow);
    }

    /**
     * Draws through Minecraft's own font renderer.
     *
     * <p>Consecutive calls share one hand-off of GL state — see {@link #begin} — so a block
     * of labels costs a single swap rather than one each. Still dearer than a batched MSDF
     * run, which is why this only ever serves as the fallback.
     */
    private static void drawVanilla(float x, float y, String text, int argb, boolean shadow) {
        try {
            begin(Batch.VANILLA_TEXT, 0, null);
            mc.fontRendererObj.drawString(text, x, y, argb, shadow);
        } catch (Throwable t) {
            fail("draw text", t);
        }
    }

    /** Width of {@code text} in logical pixels, using whichever font is actually in use. */
    public static float textWidth(String text, TextStyle style) {
        if (text == null || text.isEmpty()) return 0f;

        BakedFont font = fonts.get(style.font);
        if (font == null) {
            return mc.fontRendererObj.getStringWidth(text);
        }
        return font.width(text, style.size) + style.letterSpacing * (text.length() - 1);
    }

    public static float textHeight(TextStyle style) {
        BakedFont font = fonts.get(style.font);
        return font == null ? mc.fontRendererObj.FONT_HEIGHT : font.lineHeight * style.size;
    }

    public static int textWidth(String text) {
        return text == null ? 0 : mc.fontRendererObj.getStringWidth(text);
    }

    public static int textHeight() {
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    // ------------------------------------------------------------------
    // Backdrop effects
    // ------------------------------------------------------------------

    public static void blur(Region region, Corners corners, float radius) {
        blur(region, corners, radius, 0);
    }

    /**
     * Frosted-glass panel with a tint. The tint's alpha controls how far the backdrop is
     * pulled toward it, so a translucent black darkens and a fully transparent one leaves
     * the blur untouched.
     */
    public static void blur(Region region, Corners corners, float radius, Color tint) {
        blur(region, corners, radius, argb(tint));
    }

    /**
     * Fills the region with a blurred copy of the frame behind it — a frosted-glass panel.
     *
     * @param radius blur strength in logical pixels
     * @param tint   packed ARGB; its alpha controls how far the backdrop is pulled toward
     *               that colour, so {@code 0x50000000} darkens and {@code 0} leaves it alone
     */
    public static void blur(Region region, Corners corners, float radius, int tint) {
        if (!canDraw() || region.w <= 0f || region.h <= 0f) return;

        // Nothing to blur without a framebuffer to read the frame back out of.
        if (hostColorTexture() == 0) return;

        try {
            // Shapes queued so far must land before the panel does, or the panel would
            // composite over geometry that has not been drawn yet.
            flush();

            // Radius is authored in logical pixels but the blur runs on the real
            // framebuffer, so it has to be scaled up to device pixels first.
            int iterations = KawaseBlur.iterationsFor(radius * scaleFactor);

            // Only the panel's own corner of the screen is blurred, plus a margin wide
            // enough for the kernel to reach into. Blurring the whole screen for a small
            // panel costs an order of magnitude more fill rate for pixels nothing samples.
            if (!captureBackdrop(region, blurMargin(iterations))) return;

            RenderTarget blurred = blurCapture(backdrop.target, iterations);

            compositeBackdrop(region, corners, blurred.texture,
                    backdrop.uLeft, backdrop.vTop, backdrop.uRight, backdrop.vBottom, tint);
        } catch (Throwable t) {
            fail("blur", t);
        }
    }

    /**
     * How far outside a panel the blur has to read.
     *
     * <p>Each halving step doubles the reach of the kernel in source pixels, so the margin
     * grows with the iteration count. Without it the chain would clamp at the crop's edge
     * and leave a visible seam around the panel.
     */
    private static int blurMargin(int iterations) {
        return Math.max(8, 1 << (iterations + 1));
    }

    /** Where a captured crop sits, and where the panel sits inside it. Reused every call. */
    private static final class Backdrop {
        RenderTarget target;
        float uLeft, vTop, uRight, vBottom;
    }

    private static final Backdrop backdrop = new Backdrop();

    /**
     * Copies the frame behind {@code region} into a pooled target, leaving the result and
     * the panel's texture coordinates within it in {@link #backdrop}.
     *
     * <p>Effects must read a copy rather than the game's own colour buffer: they draw back
     * into that same buffer, and a shader that samples the texture it is writing to has no
     * defined result.
     *
     * @param margin extra device pixels around the region, for kernels that reach outward
     * @return whether anything on screen was captured
     */
    private static boolean captureBackdrop(Region region, int margin) {
        int left = (int) Math.floor(region.x * scaleFactor) - margin;
        int right = (int) Math.ceil(region.right() * scaleFactor) + margin;
        // Framebuffer rows count from the bottom, GUI coordinates from the top.
        int bottom = frameHeightPx - (int) Math.ceil(region.bottom() * scaleFactor) - margin;
        int top = frameHeightPx - (int) Math.floor(region.y * scaleFactor) + margin;

        int cropX = Math.max(0, left);
        int cropY = Math.max(0, bottom);
        int cropW = Math.min(frameWidthPx, right) - cropX;
        int cropH = Math.min(frameHeightPx, top) - cropY;

        if (cropW <= 0 || cropH <= 0) return false;

        RenderTarget crop = pool.acquire(cropW, cropH);

        // The copy below reads from whatever framebuffer is bound, and acquiring can leave
        // a different one bound: creating a target binds its own FBO to attach the texture
        // and unbinds to 0 afterwards. Without re-binding here, the first frame at any new
        // crop size would capture the window's back buffer instead of the frame Minecraft
        // just drew — which under the game's usual FBO path holds nothing useful at all.
        bindHostFramebuffer();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, crop.texture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, cropX, cropY, cropW, cropH);

        // Where the panel itself sits inside the crop. Clamping above can bite a piece off
        // the margin at a screen edge, so this is measured against the crop that was
        // actually taken rather than the one that was asked for.
        //
        // The vertical pair is named by which edge of the panel it belongs to, not by which
        // is numerically larger: the panel's top edge has the *higher* texture coordinate,
        // because framebuffer rows count upward from the bottom.
        backdrop.target = crop;
        backdrop.uLeft = (region.x * scaleFactor - cropX) / cropW;
        backdrop.uRight = (region.right() * scaleFactor - cropX) / cropW;
        backdrop.vTop = (frameHeightPx - region.y * scaleFactor - cropY) / cropH;
        backdrop.vBottom = (frameHeightPx - region.bottom() * scaleFactor - cropY) / cropH;

        return true;
    }

    /**
     * Draws a blurred backdrop over the region, masked to its rounded corners.
     *
     * <p>The source rectangle is given as the coordinates of the panel's own edges, in the
     * order the vertex shader consumes them: left, top, right, bottom.
     */
    private static void compositeBackdrop(Region region, Corners corners, int texture,
                                          float uLeft, float vTop, float uRight, float vBottom,
                                          int tint) {
        float limit = Corners.limitFor(region.w, region.h);

        bindHostFramebuffer();
        blitProgram.bind();
        blitProgram.set("u_Texture", 0);
        blitProgram.set("u_Viewport", viewportWidth, viewportHeight);
        blitProgram.set("u_Region", region.x, region.y, region.w, region.h);
        blitProgram.set("u_HalfSize", region.w * 0.5f, region.h * 0.5f);
        blitProgram.set("u_Radii",
                Math.min(corners.tl, limit), Math.min(corners.tr, limit),
                Math.min(corners.br, limit), Math.min(corners.bl, limit));
        blitProgram.set("u_UvRect", uLeft, vTop, uRight, vBottom);
        blitProgram.setColor("u_Tint", tint);

        drawRegionQuad(texture);
    }

    /**
     * Runs a registered custom effect over the region, reading the frame behind it.
     *
     * @param name     the name it was registered under with {@link #shaders()}
     * @param uniforms extra values for the effect, or {@code null}
     */
    public static void shader(Region region, Corners corners, String name, UniformBlock uniforms) {
        if (!canDraw() || region.w <= 0f || region.h <= 0f) return;

        ShaderProgram program = customShaders.get(name);
        if (program == null || hostColorTexture() == 0) return;

        try {
            flush();

            // The effect samples this copy, not the live colour buffer it is about to draw
            // into. The margin gives displacement effects somewhere real to read from when
            // they sample outside their own panel.
            if (!captureBackdrop(region, EFFECT_MARGIN_PX)) return;

            Corners clamped = corners.clampTo(region.w, region.h);

            bindHostFramebuffer();
            program.bind();
            program.set("u_Texture", 0);
            program.set("u_Viewport", viewportWidth, viewportHeight);
            program.set("u_Region", region.x, region.y, region.w, region.h);
            program.set("u_HalfSize", region.w * 0.5f, region.h * 0.5f);
            program.set("u_Radii", clamped.tl, clamped.tr, clamped.br, clamped.bl);
            program.set("u_Resolution", viewportWidth, viewportHeight);
            program.set("u_Time", frameTimeSeconds);
            program.set("u_UvRect", backdrop.uLeft, backdrop.vTop,
                    backdrop.uRight, backdrop.vBottom);
            CustomShaders.apply(program, uniforms);

            drawRegionQuad(backdrop.target.texture);
        } catch (Throwable t) {
            fail("run effect '" + name + "'", t);
        }
    }

    /** How far past its panel a custom effect can sample, in device pixels. */
    private static final int EFFECT_MARGIN_PX = 32;

    /** Draws the four-vertex region quad that backdrop passes share. */
    private static void drawRegionQuad(int texture) {
        bindTexture(texture);

        GL30.glBindVertexArray(effectVao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /** Runs the blur chain over an already-captured crop. */
    private static RenderTarget blurCapture(RenderTarget crop, int iterations) {
        GL30.glBindVertexArray(effectVao);

        // Every pass covers its whole target and is meant to replace it outright. Blending
        // has to be off for that: targets come from a pool and still hold the previous
        // frame's pixels, which would otherwise show through anywhere the scene's own alpha
        // is below one.
        GL11.glDisable(GL11.GL_BLEND);

        RenderTarget blurred = kawase.blur(crop.texture, crop.width, crop.height, iterations);

        GL11.glEnable(GL11.GL_BLEND);
        GL30.glBindVertexArray(0);
        return blurred;
    }

    // ------------------------------------------------------------------
    // Teardown
    // ------------------------------------------------------------------

    /** Full teardown: the renderer's GL objects and the font and image registries with it. */
    public static void dispose() {
        fonts.dispose();
        images.dispose();
        disposeRenderResources();
    }

    /**
     * Releases everything {@link #createResources} built, leaving the registries alone.
     *
     * <p>Split out from {@link #dispose()} for {@link #init()}, which has to drop the old GL
     * objects but must not take the registries with them: disposing the font manager stops
     * its bake executor for good, and the fonts registered right after a re-init would be
     * rejected outright.
     */
    private static void disposeRenderResources() {
        inFrame = false;
        frameStarted = false;
        hostStateCaptured = false;
        batch = Batch.NONE;

        // Outside the `initialised` guard below: the world pass builds its GL objects on its
        // own schedule, so it can hold resources on a run where the overlay never drew. Both
        // its dispose paths no-op when there is nothing to release.
        world.dispose();
        world = new WorldDraw();
        worldFailed = false;
        worldFailure = null;

        if (!initialised) return;

        shapes.dispose();
        quads.dispose();
        pool.dispose();
        kawase.dispose();
        customShaders.dispose();
        shapeProgram.dispose();
        blitProgram.dispose();
        imageProgram.dispose();
        msdfProgram.dispose();
        GL30.glDeleteVertexArrays(effectVao);

        initialised = false;
    }
}
