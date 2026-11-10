package com.cortex.legacy.world;

import com.cortex.legacy.image.Image;
import com.cortex.legacy.render.batch.WorldBatcher;
import com.cortex.legacy.render.gl.GlStateSnapshot;
import com.cortex.legacy.render.gl.ShaderProgram;

import net.minecraft.client.Minecraft;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.awt.Color;
import java.nio.FloatBuffer;

/**
 * Cortex's world pass: geometry drawn among the blocks rather than on top of the screen.
 *
 * <h2>Using it</h2>
 * Draw from a {@code RenderDimensionEvent} handler. Coordinates are absolute world
 * coordinates — the same numbers {@code entity.posX} and {@code BlockPos} carry — and
 * colours are packed ARGB, matching the 2D side:
 * <pre>{@code
 * Cortex.world().box(Box.ofEntity(player, partialTicks), 0x4000A2FF, 0xFF00A2FF);
 * Cortex.world().tracer(target.posX, target.posY, target.posZ, 0xFFFF4444);
 * }</pre>
 *
 * <h2>How it fits together</h2>
 * The pass reads Minecraft's own modelview and projection back out of GL and multiplies
 * them into one matrix, so this geometry lands in exactly the space the world was drawn in
 * — no camera maths of its own to drift out of agreement with the game's.
 *
 * <p>Draws accumulate per {@linkplain DepthMode depth mode}, texture and line width; a
 * change to any of those submits what is queued. Within one batch, triangles are always
 * submitted before lines, so a box's outline lands on top of its own fill no matter which
 * order the two were asked for.
 *
 * <h2>Translucency</h2>
 * Nothing is depth-sorted, and translucent geometry does not write depth. Two overlapping
 * translucent boxes therefore blend in submission order rather than by distance. For ESP
 * volumes that is invisible in practice; it is worth knowing before building anything that
 * leans on correct see-through layering.
 */
public final class WorldDraw {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** How far in front of the camera a tracer begins, in blocks. */
    private static final float TRACER_ORIGIN_DISTANCE = 0.35f;

    private static final float DEFAULT_LINE_WIDTH = 1.5f;

    private final WorldBatcher triangles = new WorldBatcher(GL11.GL_TRIANGLES);
    private final WorldBatcher lines = new WorldBatcher(GL11.GL_LINES);

    private ShaderProgram program;

    /** The state the game was in when the pass began, restored when it ends. */
    private final GlStateSnapshot hostState = new GlStateSnapshot();

    private final FloatBuffer matrixScratch = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer viewProjBuffer = BufferUtils.createFloatBuffer(16);

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f viewProj = new Matrix4f();

    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();
    private final Vector3f cameraForward = new Vector3f();

    private double camX, camY, camZ;

    private boolean inPass;
    private boolean stateCaptured;

    // Diagnostics. The pass has no visible output of its own when something is wrong — it
    // simply draws nothing — so these are the difference between "it is broken" and knowing
    // which half is broken. Surfaced through Cortex.worldStatus().
    private boolean everBegan;
    private boolean cameraCaptured;
    private int frameDrawCalls;
    private int frameVertices;
    private int lastDrawCalls;
    private int lastVertices;

    private DepthMode depthMode = DepthMode.THROUGH_WALLS;
    private int texture;
    private float lineWidth = DEFAULT_LINE_WIDTH;

    public WorldDraw() {
    }

    // ------------------------------------------------------------------
    // Lifecycle — driven by Cortex, never by drawing code
    // ------------------------------------------------------------------

    /**
     * Opens the pass, capturing the camera and the state to give back.
     *
     * <p>Unlike the 2D frame this does its setup up front rather than on first draw: the
     * matrices have to be read while the world's are still current, and by the time a
     * module asks to draw there is no cheaper moment to do it.
     */
    /**
     * Records the camera, from the one point in the frame where it is trustworthy.
     *
     * <p>Called immediately after {@code setupCameraTransform}, and deliberately not later.
     * By the time the dimension event fires, {@code renderWorldPass} has popped the modelview
     * stack once more than it pushed, leaving the top at whatever sat below the camera
     * transform — in practice the identity-plus-{@code translate(0, 0, -2000)} that
     * {@code setupOverlayRendering} left behind on the previous frame. Reading the stack
     * there puts every vertex 2000 blocks behind the viewer, past the far plane, where it is
     * clipped away: geometry submitted, draw calls issued, nothing on screen and nothing in
     * the log.
     *
     * <p>The projection is taken here too. Cloud rendering rewrites it mid-frame with a
     * quadrupled far plane and restores something slightly different again, and the value
     * that belongs with this modelview is the one set alongside it.
     */
    public void captureCamera() {
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, matrixScratch);
        view.set(matrixScratch);

        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, matrixScratch);
        viewProj.set(matrixScratch).mul(view);
        viewProj.get(viewProjBuffer);

        // The world-space directions that map to the camera's own axes. Billboards are
        // spanned by the first two, and tracers start along the third.
        view.positiveX(cameraRight);
        view.positiveY(cameraUp);
        view.positiveZ(cameraForward).negate();

        cameraCaptured = true;
    }

    public void begin() {
        program = programOrThrow();

        // Sampled here rather than alongside the matrices: RenderManager caches the viewer
        // position during entity rendering, which happens after the camera is set up, so
        // reading it there would be a frame behind and everything would trail the camera.
        camX = mc.getRenderManager().viewerPosX;
        camY = mc.getRenderManager().viewerPosY;
        camZ = mc.getRenderManager().viewerPosZ;

        hostState.capture();
        stateCaptured = true;

        applyDrawState();

        // Reset every frame, all three: a module that changed one and returned should not
        // be able to leave the next frame's first draw inheriting it.
        depthMode = DepthMode.THROUGH_WALLS;
        texture = 0;
        lineWidth = DEFAULT_LINE_WIDTH;

        frameDrawCalls = 0;
        frameVertices = 0;

        everBegan = true;
        inPass = true;
    }

    /**
     * A one-line summary of what the pass did on the last frame, for the debug module.
     *
     * <p>Zero draw calls with the pass open means geometry never reached it — the module is
     * drawing outside the bracket, or its listener is not firing. Draw calls with nothing on
     * screen means the geometry is being submitted and lost somewhere downstream, which
     * points at the matrix rather than at the caller.
     */
    public String status() {
        if (!everBegan) return "pass never opened";

        return lastDrawCalls + " calls, " + lastVertices + " verts";
    }

    /** The view-projection matrix in use, formatted row by row. */
    public String matrixRow(int row) {
        return String.format("%.2f %.2f %.2f %.2f",
                viewProj.get(0, row), viewProj.get(1, row),
                viewProj.get(2, row), viewProj.get(3, row));
    }

    /**
     * A verdict on the view-projection matrix.
     *
     * <p>Worth its own readout because a bad matrix is the one failure with no symptom to
     * follow: geometry is submitted, draw calls run, nothing throws, and every vertex lands
     * on top of itself at the origin. It looks exactly like the pass not running at all.
     *
     * <p>An all-zero matrix means {@code glGetFloatv} wrote nothing — the fixed-function
     * matrix stacks do not exist in a core-profile context, and the call is a silent
     * {@code GL_INVALID_ENUM} rather than an error anyone sees.
     */
    public String matrixVerdict() {
        if (!cameraCaptured) return "NEVER CAPTURED - hook missing";
        if (!everBegan) return "not read yet";

        // The w row of a perspective matrix ends in zero. Anything else means a translation
        // survived into it, which is what a stale modelview looks like from here.
        float w = viewProj.get(3, 3);
        if (Math.abs(w) > 1e-3f) {
            return String.format("STALE MODELVIEW - w row ends %.1f, expected 0", w);
        }

        boolean zero = true;

        for (int column = 0; column < 4 && zero; column++) {
            for (int row = 0; row < 4; row++) {
                if (viewProj.get(column, row) != 0f) {
                    zero = false;
                    break;
                }
            }
        }

        if (zero) return "ZERO - glGetFloatv wrote nothing";
        if (viewProj.equals(IDENTITY, 1e-6f)) return "IDENTITY - stacks not set";

        return "ok";
    }

    private static final Matrix4f IDENTITY = new Matrix4f();

    /** Submits anything still queued and hands GL back to Minecraft. */
    public void end() {
        if (!inPass && !stateCaptured) return;

        inPass = false;

        try {
            flush();
        } finally {
            lastDrawCalls = frameDrawCalls;
            lastVertices = frameVertices;

            if (stateCaptured) {
                stateCaptured = false;
                hostState.restore();
            }
        }
    }

    private void applyDrawState() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Off so a box stays visible from the inside — standing within an ESP volume is
        // the normal case, not an edge case.
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Overlays are translucent, and translucent geometry that writes depth occludes
        // whatever is drawn after it, including the rest of the overlay.
        GL11.glDepthMask(false);

        GL11.glDisable(GL11.GL_ALPHA_TEST);

        // GL_TEXTURE_2D is deliberately left alone. It gates the fixed-function pipeline,
        // which a bound program bypasses entirely, so touching it would buy nothing and
        // leave one more piece of state owed back to the game.
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }

    private ShaderProgram programOrThrow() {
        if (program == null) {
            program = ShaderProgram.load("world");
        }
        return program;
    }

    // ------------------------------------------------------------------
    // Batch control
    // ------------------------------------------------------------------

    /**
     * Switches the batch, submitting the previous one first.
     *
     * @return whether drawing is possible at all
     */
    private boolean select(int wantedTexture) {
        if (!inPass) return false;

        if (texture != wantedTexture) {
            flush();
            texture = wantedTexture;
        }
        return true;
    }

    /** Submits both streams, fills before outlines. */
    private void flush() {
        if (triangles.isEmpty() && lines.isEmpty()) return;

        frameDrawCalls++;
        frameVertices += triangles.vertexCount() + lines.vertexCount();

        program.bind();
        program.setMatrix("u_ViewProj", viewProjBuffer);
        program.set("u_Textured", texture != 0 ? 1 : 0);

        if (texture != 0) {
            program.set("u_Texture", 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }

        if (depthMode == DepthMode.THROUGH_WALLS) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }

        triangles.flush();

        GL11.glLineWidth(lineWidth);
        lines.flush();
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    /**
     * Sets whether following draws are hidden by terrain. Submits what is queued, so group
     * draws by mode rather than alternating.
     */
    public WorldDraw depth(DepthMode mode) {
        if (inPass && mode != depthMode) {
            flush();
            depthMode = mode;
        }
        return this;
    }

    /** Sets the width of following lines, in pixels. Submits what is queued. */
    public WorldDraw lineWidth(float width) {
        float clamped = Math.max(0.5f, width);

        if (inPass && clamped != lineWidth) {
            flush();
            lineWidth = clamped;
        }
        return this;
    }

    // ------------------------------------------------------------------
    // Boxes
    // ------------------------------------------------------------------

    /** Fills a box. */
    public void box(Box box, Color color) {
        box(box, argb(color));
    }

    public void box(Box box, int argb) {
        if (!select(0)) return;
        emitBoxFill(box, argb);
    }

    /** Draws a box's twelve edges. */
    public void boxOutline(Box box, Color color) {
        boxOutline(box, argb(color));
    }

    public void boxOutline(Box box, int argb) {
        if (!select(0)) return;
        emitBoxEdges(box, argb);
    }

    /**
     * Fills a box and outlines it — the shape ESP and ChestESP want, in one call and one
     * pair of batches.
     */
    public void box(Box box, Color fill, Color outline) {
        box(box, argb(fill), argb(outline));
    }

    public void box(Box box, int fillArgb, int outlineArgb) {
        if (!select(0)) return;

        emitBoxFill(box, fillArgb);
        emitBoxEdges(box, outlineArgb);
    }

    /**
     * A box filled from its floor up to {@code progress} of its height, outlined at full
     * height — a block-breaking indicator.
     *
     * @param progress 0..1; values outside are clamped
     */
    public void progressBox(Box box, float progress, int fillArgb, int outlineArgb) {
        if (!select(0)) return;

        emitBoxFill(box.toProgress(progress), fillArgb);
        emitBoxEdges(box, outlineArgb);
    }

    private void emitBoxFill(Box box, int argb) {
        float x0 = (float) (box.minX - camX);
        float y0 = (float) (box.minY - camY);
        float z0 = (float) (box.minZ - camZ);
        float x1 = (float) (box.maxX - camX);
        float y1 = (float) (box.maxY - camY);
        float z1 = (float) (box.maxZ - camZ);

        int a = triangles.vertex(x0, y0, z0, argb);
        int b = triangles.vertex(x1, y0, z0, argb);
        int c = triangles.vertex(x1, y0, z1, argb);
        int d = triangles.vertex(x0, y0, z1, argb);
        int e = triangles.vertex(x0, y1, z0, argb);
        int f = triangles.vertex(x1, y1, z0, argb);
        int g = triangles.vertex(x1, y1, z1, argb);
        int h = triangles.vertex(x0, y1, z1, argb);

        triangles.quad(a, b, c, d); // bottom
        triangles.quad(e, f, g, h); // top
        triangles.quad(a, b, f, e); // -z
        triangles.quad(d, c, g, h); // +z
        triangles.quad(a, d, h, e); // -x
        triangles.quad(b, c, g, f); // +x
    }

    private void emitBoxEdges(Box box, int argb) {
        float x0 = (float) (box.minX - camX);
        float y0 = (float) (box.minY - camY);
        float z0 = (float) (box.minZ - camZ);
        float x1 = (float) (box.maxX - camX);
        float y1 = (float) (box.maxY - camY);
        float z1 = (float) (box.maxZ - camZ);

        int a = lines.vertex(x0, y0, z0, argb);
        int b = lines.vertex(x1, y0, z0, argb);
        int c = lines.vertex(x1, y0, z1, argb);
        int d = lines.vertex(x0, y0, z1, argb);
        int e = lines.vertex(x0, y1, z0, argb);
        int f = lines.vertex(x1, y1, z0, argb);
        int g = lines.vertex(x1, y1, z1, argb);
        int h = lines.vertex(x0, y1, z1, argb);

        lines.edge(a, b);
        lines.edge(b, c);
        lines.edge(c, d);
        lines.edge(d, a);

        lines.edge(e, f);
        lines.edge(f, g);
        lines.edge(g, h);
        lines.edge(h, e);

        lines.edge(a, e);
        lines.edge(b, f);
        lines.edge(c, g);
        lines.edge(d, h);
    }

    // ------------------------------------------------------------------
    // Lines
    // ------------------------------------------------------------------

    /** Draws a line between two world positions. */
    public void line(double x0, double y0, double z0, double x1, double y1, double z1, int argb) {
        if (!select(0)) return;

        int a = lines.vertex((float) (x0 - camX), (float) (y0 - camY), (float) (z0 - camZ), argb);
        int b = lines.vertex((float) (x1 - camX), (float) (y1 - camY), (float) (z1 - camZ), argb);
        lines.edge(a, b);
    }

    /**
     * Draws a line from just in front of the camera to a world position.
     *
     * <p>Starting slightly ahead of the near plane rather than at the camera itself is what
     * keeps the line from being clipped away the moment it is drawn.
     */
    public void tracer(double x, double y, double z, Color color) {
        tracer(x, y, z, argb(color));
    }

    public void tracer(double x, double y, double z, int argb) {
        if (!select(0)) return;

        int a = lines.vertex(
                cameraForward.x * TRACER_ORIGIN_DISTANCE,
                cameraForward.y * TRACER_ORIGIN_DISTANCE,
                cameraForward.z * TRACER_ORIGIN_DISTANCE,
                argb);
        int b = lines.vertex((float) (x - camX), (float) (y - camY), (float) (z - camZ), argb);

        lines.edge(a, b);
    }

    /** Draws a tracer to the centre of a box's base — where an entity stands. */
    public void tracer(Box box, int argb) {
        tracer(box.centerX(), box.minY, box.centerZ(), argb);
    }

    // ------------------------------------------------------------------
    // Billboards and particles
    // ------------------------------------------------------------------

    /**
     * Draws a camera-facing square at a world position — the primitive a particle is made
     * of.
     *
     * <p>The engine draws them; it does not simulate them. Spawning, moving and expiring
     * particles belongs to the module that owns their behaviour, which then draws each
     * survivor here once a frame.
     *
     * @param size the square's edge length, in blocks
     */
    public void billboard(double x, double y, double z, float size, int argb) {
        if (!select(0)) return;
        emitBillboard(x, y, z, size, 0f, 0f, 1f, 1f, argb);
    }

    /** Draws a camera-facing sprite. */
    public void billboard(double x, double y, double z, float size, Image image, int tint) {
        if (image == null || !select(image.texture)) return;
        emitBillboard(x, y, z, size, 0f, 0f, 1f, 1f, tint);
    }

    /** Draws a sub-rectangle of a sprite sheet as a camera-facing quad. */
    public void billboard(double x, double y, double z, float size, Image image,
                          float u0, float v0, float u1, float v1, int tint) {
        if (image == null || !select(image.texture)) return;
        emitBillboard(x, y, z, size, u0, v0, u1, v1, tint);
    }

    private void emitBillboard(double x, double y, double z, float size,
                               float u0, float v0, float u1, float v1, int argb) {
        float cx = (float) (x - camX);
        float cy = (float) (y - camY);
        float cz = (float) (z - camZ);

        float half = size * 0.5f;

        float rx = cameraRight.x * half;
        float ry = cameraRight.y * half;
        float rz = cameraRight.z * half;

        float ux = cameraUp.x * half;
        float uy = cameraUp.y * half;
        float uz = cameraUp.z * half;

        int a = triangles.vertex(cx - rx - ux, cy - ry - uy, cz - rz - uz, u0, v1, argb);
        int b = triangles.vertex(cx + rx - ux, cy + ry - uy, cz + rz - uz, u1, v1, argb);
        int c = triangles.vertex(cx + rx + ux, cy + ry + uy, cz + rz + uz, u1, v0, argb);
        int d = triangles.vertex(cx - rx + ux, cy - ry + uy, cz - rz + uz, u0, v0, argb);

        triangles.quad(a, b, c, d);
    }

    // ------------------------------------------------------------------
    // Teardown
    // ------------------------------------------------------------------

    public void dispose() {
        triangles.dispose();
        lines.dispose();

        if (program != null) {
            program.dispose();
            program = null;
        }

        inPass = false;
        stateCaptured = false;
    }

    /** {@link Color#getRGB()} is already packed ARGB, which is what the batchers store. */
    private static int argb(Color color) {
        return color == null ? 0 : color.getRGB();
    }
}
