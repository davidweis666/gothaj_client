package com.cortex.legacy.render.batch;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Batches geometry drawn in the world — ESP boxes, tracers, markers, particles.
 *
 * <p>The same class serves triangles and lines; which one is fixed at construction, because
 * the two cannot share a draw call and keeping them in separate buffers means a frame's
 * worth of filled boxes and a frame's worth of their outlines each collapse into one.
 *
 * <p>Unlike {@link ShapeBatcher}, vertices are built by the caller in two steps —
 * {@link #vertex} returns an index that {@link #tri} and {@link #edge} then refer to. A box
 * is eight vertices shared between twelve triangles or twelve lines, so letting shapes name
 * their corners once is worth the slightly wordier call site.
 */
public final class WorldBatcher {

    /** position(3) + uv(2), then one packed RGBA colour. */
    private static final int FLOATS_PER_VERTEX = 5;
    private static final int BYTES_PER_VERTEX = FLOATS_PER_VERTEX * Float.BYTES + 4;

    private static final int INITIAL_VERTEX_CAPACITY = 4096;

    /** {@link GL11#GL_TRIANGLES} or {@link GL11#GL_LINES}. */
    private final int primitive;

    private ByteBuffer vertices;
    private ByteBuffer indices;
    private int vertexCount;
    private int indexCount;

    private int vao = -1;
    private int vbo;
    private int ibo;

    public WorldBatcher(int primitive) {
        this.primitive = primitive;
        this.vertices = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * BYTES_PER_VERTEX);
        this.indices = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * 2 * Integer.BYTES);
    }

    public boolean isEmpty() {
        return indexCount == 0;
    }

    /** How many vertices are queued. For diagnostics; see {@code Cortex.worldStatus()}. */
    public int vertexCount() {
        return vertexCount;
    }

    // ------------------------------------------------------------------
    // Submission
    // ------------------------------------------------------------------

    /**
     * Adds a vertex and returns the index to refer to it by.
     *
     * <p>Coordinates are camera-relative — see {@code world.vert} for why the subtraction
     * happens on the CPU in double precision rather than here.
     */
    public int vertex(float x, float y, float z, float u, float v, int argb) {
        if (vertices.remaining() < BYTES_PER_VERTEX) {
            vertices = grow(vertices);
        }

        vertices.putFloat(x);
        vertices.putFloat(y);
        vertices.putFloat(z);
        vertices.putFloat(u);
        vertices.putFloat(v);

        vertices.put((byte) ((argb >> 16) & 0xFF));
        vertices.put((byte) ((argb >> 8) & 0xFF));
        vertices.put((byte) (argb & 0xFF));
        vertices.put((byte) ((argb >>> 24) & 0xFF));

        return vertexCount++;
    }

    /** Adds an untextured vertex. */
    public int vertex(float x, float y, float z, int argb) {
        return vertex(x, y, z, 0f, 0f, argb);
    }

    /** Joins three vertices into a triangle. Only meaningful on a triangle batcher. */
    public void tri(int a, int b, int c) {
        ensureIndexCapacity(3);

        indices.putInt(a).putInt(b).putInt(c);
        indexCount += 3;
    }

    /** Joins four vertices into a quad, wound as two triangles. */
    public void quad(int a, int b, int c, int d) {
        tri(a, b, c);
        tri(a, c, d);
    }

    /** Joins two vertices into a line segment. Only meaningful on a line batcher. */
    public void edge(int a, int b) {
        ensureIndexCapacity(2);

        indices.putInt(a).putInt(b);
        indexCount += 2;
    }

    private void ensureIndexCapacity(int count) {
        if (indices.remaining() < count * Integer.BYTES) {
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
    // Drawing
    // ------------------------------------------------------------------

    /**
     * Uploads and draws everything accumulated so far, then resets.
     *
     * <p>The caller is responsible for having bound the world program, its matrix and any
     * texture, and for having set the depth and blend state the batch expects.
     */
    public void flush() {
        if (indexCount == 0) return;

        if (vao == -1) {
            createBuffers();
        }

        vertices.flip();
        indices.flip();

        // Bound before the VAO for the same reason as the other batchers: the attribute
        // pointers recorded in the VAO refer to whichever buffer was bound when they were
        // set up, and this order keeps that association intact across re-uploads.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STREAM_DRAW);

        GL11.glDrawElements(primitive, indexCount, GL11.GL_UNSIGNED_INT, 0L);

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

        GL20.glEnableVertexAttribArray(0);                                          // a_Pos
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, offset);
        offset += 3 * Float.BYTES;

        GL20.glEnableVertexAttribArray(1);                                          // a_UV
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, offset);
        offset += 2 * Float.BYTES;

        GL20.glEnableVertexAttribArray(2);                                          // a_Color
        GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, stride, offset);

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
