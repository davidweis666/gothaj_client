package com.cortex.legacy.render.batch;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Batches textured quads — images and glyphs alike.
 *
 * <p>Images and MSDF text differ only in how their fragment shader interprets the sampled
 * texel, so they share this vertex layout and this buffer. A run of glyphs from one atlas,
 * or a row of icons from one sheet, collapses into a single draw call; the caller flushes
 * whenever the bound texture or shader has to change.
 */
public final class QuadBatcher {

    /** position(2) + uv(2) + gradient position(1), then two packed RGBA colours. */
    private static final int FLOATS_PER_VERTEX = 5;
    private static final int BYTES_PER_VERTEX = FLOATS_PER_VERTEX * Float.BYTES + 8;

    private static final int INITIAL_QUAD_CAPACITY = 1024;

    private ByteBuffer vertices;
    private ByteBuffer indices;
    private int vertexCount;
    private int indexCount;

    private int vao = -1;
    private int vbo;
    private int ibo;

    public QuadBatcher() {
        vertices = MemoryUtil.memAlloc(INITIAL_QUAD_CAPACITY * 4 * BYTES_PER_VERTEX);
        indices = MemoryUtil.memAlloc(INITIAL_QUAD_CAPACITY * 6 * Integer.BYTES);
    }

    public boolean isEmpty() {
        return indexCount == 0;
    }

    /**
     * Adds one quad.
     *
     * <p>Gradient positions are taken as given and are <em>not</em> normalised: callers
     * drawing an animated gradient must have already folded in
     * {@link com.cortex.legacy.draw.Gradient#offset()}, since sliding those positions is
     * what animates it. The shader mirrors anything outside 0..1, so values beyond that
     * range are meaningful rather than clamped.
     *
     * @param gradStart gradient position at the quad's left/top edge
     * @param gradEnd   gradient position at its right/bottom edge
     * @param vertical  whether the gradient runs down the quad rather than across it
     */
    public void quad(float x, float y, float w, float h,
                     float u0, float v0, float u1, float v1,
                     int startColor, int endColor,
                     float gradStart, float gradEnd, boolean vertical) {

        ensureCapacity();

        int base = vertexCount;

        // A vertical gradient advances down the quad, a horizontal one across it, so the
        // two corners that share the leading edge both take the starting position.
        float topRight = vertical ? gradStart : gradEnd;
        float bottomLeft = vertical ? gradEnd : gradStart;

        emit(x, y, u0, v0, gradStart, startColor, endColor);
        emit(x + w, y, u1, v0, topRight, startColor, endColor);
        emit(x + w, y + h, u1, v1, gradEnd, startColor, endColor);
        emit(x, y + h, u0, v1, bottomLeft, startColor, endColor);

        indices.putInt(base).putInt(base + 1).putInt(base + 2);
        indices.putInt(base).putInt(base + 2).putInt(base + 3);
        indexCount += 6;
    }

    /** Adds a quad with no gradient. */
    public void quad(float x, float y, float w, float h,
                     float u0, float v0, float u1, float v1, int argb) {
        quad(x, y, w, h, u0, v0, u1, v1, argb, argb, 0f, 0f, false);
    }

    private void emit(float x, float y, float u, float v, float gradT, int startColor, int endColor) {
        vertices.putFloat(x);
        vertices.putFloat(y);
        vertices.putFloat(u);
        vertices.putFloat(v);
        vertices.putFloat(gradT);

        putColor(startColor);
        putColor(endColor);

        vertexCount++;
    }

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

    /** Uploads and draws everything accumulated so far. The caller binds program and texture. */
    public void flush() {
        if (indexCount == 0) return;

        if (vao == -1) {
            createBuffers();
        }

        vertices.flip();
        indices.flip();

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

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, offset);
        offset += 2 * Float.BYTES;

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, offset);
        offset += 2 * Float.BYTES;

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 1, GL11.GL_FLOAT, false, stride, offset);
        offset += Float.BYTES;

        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, stride, offset);
        offset += 4;

        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 4, GL11.GL_UNSIGNED_BYTE, true, stride, offset);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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
