package com.cortex.legacy.render.batch;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import com.cortex.legacy.draw.Gradient;

import java.nio.ByteBuffer;

/**
 * Accumulates every shape drawn this frame into one interleaved vertex stream and
 * submits it as a single indexed draw call.
 *
 * <p>All shapes — rectangles, rounded rectangles, ellipses, lines and triangles — share
 * one shader and one vertex layout. The shape's identity travels per-vertex in the
 * {@code kind} channel and is resolved by a signed-distance branch in the fragment shader,
 * so a HUD made of hundreds of mixed primitives still costs one draw call.
 *
 * <p><b>Slot reuse.</b> {@code halfSize} and {@code radii} carry different data per kind:
 * <ul>
 *   <li>{@link #KIND_BOX} — {@code halfSize} is the half extent, {@code radii} the four
 *       corner radii (top-left, top-right, bottom-right, bottom-left). Lines are boxes
 *       rotated CPU-side with every radius set to half the thickness, which makes the
 *       same SDF produce a capsule.</li>
 *   <li>{@link #KIND_ELLIPSE} — {@code halfSize} is the pair of radii; {@code radii} unused.</li>
 *   <li>{@link #KIND_TRIANGLE} — {@code halfSize} is the first point and {@code radii} packs
 *       the other two, all in the shape's local space.</li>
 * </ul>
 */
public final class ShapeBatcher {

    public static final int KIND_BOX = 0;
    public static final int KIND_ELLIPSE = 1;
    public static final int KIND_TRIANGLE = 2;

    /** Fills the shape's interior. */
    public static final float FILL = 0f;

    /**
     * Draws only the glow halo, leaving the interior untouched. A glow pass is emitted
     * before the shape it belongs to, so a translucent fill on top shows the world behind
     * rather than a bright core bleeding through.
     */
    public static final float GLOW_ONLY = -1f;

    /** position(2) + local(2) + halfSize(2) + radii(4) + meta(4), then two packed RGBA colours. */
    private static final int FLOATS_PER_VERTEX = 14;
    private static final int BYTES_PER_VERTEX = FLOATS_PER_VERTEX * Float.BYTES + 8;

    private static final int INITIAL_VERTEX_CAPACITY = 4096;

    /** Extra margin around every quad so the fragment shader has room to fade edges out. */
    private static final float AA_PADDING = 2f;

    private ByteBuffer vertices;
    private ByteBuffer indices;
    private int vertexCount;
    private int indexCount;

    private int vao = -1;
    private int vbo;
    private int ibo;

    public ShapeBatcher() {
        vertices = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * BYTES_PER_VERTEX);
        indices = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * 6 * Integer.BYTES);
    }

    public boolean isEmpty() {
        return indexCount == 0;
    }

    // ------------------------------------------------------------------
    // Shape submission
    // ------------------------------------------------------------------

    /**
     * Emits an axis-aligned box.
     *
     * @param thickness  stroke width for an outline, {@code 0} to fill, or a negative value
     *                   to draw only the glow (see {@link #GLOW_ONLY})
     * @param glowRadius how far the glow reaches beyond the shape; read only when
     *                   {@code thickness} is negative
     * @param gradient   {@code null} for a flat fill
     */
    public void box(float cx, float cy, float halfW, float halfH, float tl, float tr, float br, float bl,
                    float thickness, float glowRadius, int argb, Gradient gradient) {

        quad(cx, cy, halfW, halfH, pad(thickness, glowRadius), 0f,
                KIND_BOX, halfW, halfH, tl, tr, br, bl, thickness, glowRadius, argb, gradient);
    }

    /** Emits a box rotated about its centre — used for lines, which are capsule-radius boxes. */
    public void rotatedBox(float cx, float cy, float halfW, float halfH, float radius, float angleRad,
                           float thickness, float glowRadius, int argb, Gradient gradient) {

        quad(cx, cy, halfW, halfH, pad(thickness, glowRadius), angleRad,
                KIND_BOX, halfW, halfH, radius, radius, radius, radius,
                thickness, glowRadius, argb, gradient);
    }

    public void ellipse(float cx, float cy, float rx, float ry,
                        float thickness, float glowRadius, int argb, Gradient gradient) {

        quad(cx, cy, rx, ry, pad(thickness, glowRadius), 0f,
                KIND_ELLIPSE, rx, ry, 0f, 0f, 0f, 0f, thickness, glowRadius, argb, gradient);
    }

    public void triangle(float x0, float y0, float x1, float y1, float x2, float y2,
                         float thickness, float glowRadius, int argb, Gradient gradient) {

        float cx = (x0 + x1 + x2) / 3f;
        float cy = (y0 + y1 + y2) / 3f;

        // The covering quad is the triangle's bounding box; the SDF clips it to the real shape.
        float halfW = Math.max(Math.max(Math.abs(x0 - cx), Math.abs(x1 - cx)), Math.abs(x2 - cx));
        float halfH = Math.max(Math.max(Math.abs(y0 - cy), Math.abs(y1 - cy)), Math.abs(y2 - cy));

        quad(cx, cy, halfW, halfH, pad(thickness, glowRadius), 0f,
                KIND_TRIANGLE,
                x0 - cx, y0 - cy,
                x1 - cx, y1 - cy, x2 - cx, y2 - cy,
                thickness, glowRadius, argb, gradient);
    }

    private static float pad(float thickness, float glowRadius) {
        return AA_PADDING + glowRadius + Math.max(thickness, 0f) * 0.5f;
    }

    /**
     * Emits the four vertices and six indices covering one shape.
     *
     * <p>{@code angleRad} rotates the quad's screen positions while leaving the local
     * coordinates axis-aligned, so the fragment shader always evaluates an upright SDF and
     * rotation costs nothing extra.
     */
    private void quad(float cx, float cy, float halfW, float halfH, float pad, float angleRad,
                      int kind, float sizeX, float sizeY, float r0, float r1, float r2, float r3,
                      float thickness, float glowRadius, int argb, Gradient gradient) {

        ensureCapacity();

        float ex = halfW + pad;
        float ey = halfH + pad;

        float cos = angleRad == 0f ? 1f : (float) Math.cos(angleRad);
        float sin = angleRad == 0f ? 0f : (float) Math.sin(angleRad);

        // With no gradient both stops are the same colour, so the shader's blend is a no-op
        // and no branch is needed there.
        int startColor = gradient == null ? argb : gradient.start;
        int endColor = gradient == null ? argb : gradient.end;
        boolean horizontal = gradient != null && gradient.axis == Gradient.Axis.HORIZONTAL;

        // Sliding the whole ramp is what animates it; the shader mirrors the position as it
        // wraps so the motion has no seam.
        float animation = gradient == null ? 0f : gradient.offset();

        int base = vertexCount;

        for (int corner = 0; corner < 4; corner++) {
            float localX = (corner == 1 || corner == 2) ? ex : -ex;
            float localY = (corner >= 2) ? ey : -ey;

            // Runs 0..1 across the shape proper; the AA padding pushes it slightly past
            // either end, which the shader clamps away.
            float gradT;
            if (gradient == null) {
                gradT = 0f;
            } else if (horizontal) {
                gradT = (halfW <= 0f ? 0f : (localX / halfW) * 0.5f + 0.5f) + animation;
            } else {
                gradT = (halfH <= 0f ? 0f : (localY / halfH) * 0.5f + 0.5f) + animation;
            }

            emit(cx, cy, localX, localY, cos, sin, kind, sizeX, sizeY,
                    r0, r1, r2, r3, thickness, glowRadius, gradT, startColor, endColor);
        }

        indices.putInt(base).putInt(base + 1).putInt(base + 2);
        indices.putInt(base).putInt(base + 2).putInt(base + 3);
        indexCount += 6;
    }

    private void emit(float cx, float cy, float localX, float localY, float cos, float sin,
                      int kind, float sizeX, float sizeY, float r0, float r1, float r2, float r3,
                      float thickness, float glowRadius, float gradT, int startColor, int endColor) {

        vertices.putFloat(cx + localX * cos - localY * sin);
        vertices.putFloat(cy + localX * sin + localY * cos);
        vertices.putFloat(localX);
        vertices.putFloat(localY);
        vertices.putFloat(sizeX);
        vertices.putFloat(sizeY);
        vertices.putFloat(r0);
        vertices.putFloat(r1);
        vertices.putFloat(r2);
        vertices.putFloat(r3);
        vertices.putFloat(kind);
        vertices.putFloat(thickness);
        vertices.putFloat(glowRadius);
        vertices.putFloat(gradT);

        putColor(startColor);
        putColor(endColor);

        vertexCount++;
    }

    /** Straight RGBA bytes, normalised by the vertex attribute setup. */
    private void putColor(int argb) {
        vertices.put((byte) ((argb >> 16) & 0xFF));
        vertices.put((byte) ((argb >> 8) & 0xFF));
        vertices.put((byte) (argb & 0xFF));
        vertices.put((byte) ((argb >>> 24) & 0xFF));
    }

    private void ensureCapacity() {
        if (vertices.remaining() < 4 * BYTES_PER_VERTEX) {
            vertices = grow(vertices);
        }
        if (indices.remaining() < 6 * Integer.BYTES) {
            indices = grow(indices);
        }
    }

    private static ByteBuffer grow(ByteBuffer buffer) {
        ByteBuffer larger = MemoryUtil.memAlloc(buffer.capacity() * 2);
        buffer.flip();
        larger.put(buffer);
        MemoryUtil.memFree(buffer);
        return larger;
    }

    // ------------------------------------------------------------------
    // Submission
    // ------------------------------------------------------------------

    /**
     * Uploads and draws everything accumulated so far, then resets.
     *
     * <p>The caller is responsible for having bound the shape program and set its uniforms.
     */
    public void flush() {
        if (indexCount == 0) return;

        if (vao == -1) {
            createBuffers();
        }

        vertices.flip();
        indices.flip();

        // The VBO is bound before the VAO deliberately: the attribute pointers recorded in the
        // VAO reference whichever buffer was bound when they were set up, and re-binding in this
        // order keeps that association intact across re-uploads.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STREAM_DRAW);

        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0L);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        reset();
    }

    /** Discards accumulated geometry without drawing it. */
    public void reset() {
        vertices.clear();
        indices.clear();
        vertexCount = 0;
        indexCount = 0;
    }

    private void createBuffers() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        ibo = GL15.glGenBuffers();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL30.glBindVertexArray(vao);

        int stride = BYTES_PER_VERTEX;
        int offset = 0;

        offset = floatAttribute(0, 2, stride, offset); // a_Pos
        offset = floatAttribute(1, 2, stride, offset); // a_Local
        offset = floatAttribute(2, 2, stride, offset); // a_HalfSize
        offset = floatAttribute(3, 4, stride, offset); // a_Radii
        offset = floatAttribute(4, 4, stride, offset); // a_Meta
        offset = colorAttribute(5, stride, offset);    // a_Color
        colorAttribute(6, stride, offset);             // a_Color2

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static int floatAttribute(int index, int size, int stride, int offset) {
        GL20.glEnableVertexAttribArray(index);
        GL20.glVertexAttribPointer(index, size, GL11.GL_FLOAT, false, stride, offset);
        return offset + size * Float.BYTES;
    }

    private static int colorAttribute(int index, int stride, int offset) {
        GL20.glEnableVertexAttribArray(index);
        GL20.glVertexAttribPointer(index, 4, GL11.GL_UNSIGNED_BYTE, true, stride, offset);
        return offset + 4;
    }

    /**
     * Releases the GL objects and the off-heap buffers. Safe to call more than once —
     * freeing native memory twice corrupts the allocator rather than throwing, so the
     * pointers are cleared as they go.
     */
    public void dispose() {
        if (vao != -1) {
            GL30.glDeleteVertexArrays(vao);
            GL15.glDeleteBuffers(vbo);
            GL15.glDeleteBuffers(ibo);
            vao = -1;
        }

        if (vertices != null) {
            MemoryUtil.memFree(vertices);
            vertices = null;
        }
        if (indices != null) {
            MemoryUtil.memFree(indices);
            indices = null;
        }
    }
}
