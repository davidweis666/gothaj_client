package com.cortex.legacy.effect.shader;

import java.util.ArrayList;
import java.util.List;

/**
 * Named values passed to a custom shader.
 *
 * <p>A small builder rather than a raw map so uniform types are fixed at the point of
 * writing, which is where a mismatch against the GLSL declaration is actually visible.
 */
public final class UniformBlock {

    public enum Kind {
        FLOAT1, FLOAT2, FLOAT3, FLOAT4, INT1
    }

    public record Entry(String name, Kind kind, float[] values, int intValue) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public static UniformBlock create() {
        return new UniformBlock();
    }

    /**
     * Drops every value so the block can be filled in again.
     *
     * <p>Lets a caller keep one block as a field and refill it each frame instead of
     * building a new one — the values change, the shape of the block rarely does.
     */
    public UniformBlock clear() {
        entries.clear();
        return this;
    }

    public UniformBlock set(String name, float value) {
        entries.add(new Entry(name, Kind.FLOAT1, new float[]{value}, 0));
        return this;
    }

    public UniformBlock set(String name, float x, float y) {
        entries.add(new Entry(name, Kind.FLOAT2, new float[]{x, y}, 0));
        return this;
    }

    public UniformBlock set(String name, float x, float y, float z) {
        entries.add(new Entry(name, Kind.FLOAT3, new float[]{x, y, z}, 0));
        return this;
    }

    public UniformBlock set(String name, float x, float y, float z, float w) {
        entries.add(new Entry(name, Kind.FLOAT4, new float[]{x, y, z, w}, 0));
        return this;
    }

    public UniformBlock set(String name, int value) {
        entries.add(new Entry(name, Kind.INT1, null, value));
        return this;
    }

    /** Passes a colour as a straight-alpha vec4. */
    public UniformBlock setColor(String name, java.awt.Color color) {
        return setColor(name, color.getRGB());
    }

    /** Unpacks a packed ARGB colour into a straight-alpha vec4. */
    public UniformBlock setColor(String name, int argb) {
        return set(name,
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
    }

    public List<Entry> entries() {
        return entries;
    }
}
