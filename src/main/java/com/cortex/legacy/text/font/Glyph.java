package com.cortex.legacy.text.font;

/**
 * One glyph's placement, in units that do not depend on the size it was baked at.
 *
 * <p>Atlas coordinates are UVs; the quad bounds and advance are in em units relative to the
 * pen position, with +y pointing down to match screen space. Multiplying them by the
 * requested pixel size is all it takes to lay out text at any scale.
 */
public final class Glyph {

    public final float u0, v0, u1, v1;
    public final float left, top, right, bottom;
    public final float advance;

    Glyph(float u0, float v0, float u1, float v1,
          float left, float top, float right, float bottom, float advance) {
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.advance = advance;
    }

    /** A glyph with no ink — a space — that only moves the pen along. */
    static Glyph blank(float advance) {
        return new Glyph(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, advance);
    }

    public boolean hasInk() {
        return u1 > u0 && v1 > v0;
    }
}
