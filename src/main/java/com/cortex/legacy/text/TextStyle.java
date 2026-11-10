package com.cortex.legacy.text;

import com.cortex.legacy.draw.Gradient;

/**
 * How a run of text should be drawn. Immutable; every setter returns a copy, so a style can
 * be declared once as a constant and varied per call.
 */
public final class TextStyle {

    public enum Align {
        LEFT, CENTER, RIGHT
    }

    /** Where the y coordinate passed to a draw call sits on the glyphs. */
    public enum Baseline {
        /** y is the top of the line box. */
        TOP,
        /** y is the vertical centre of the line box. */
        MIDDLE,
        /** y is the text baseline itself. */
        ALPHABETIC
    }

    public final String font;
    public final float size;
    public final int color;
    public final Gradient gradient;
    public final Align align;
    public final Baseline baseline;
    public final float letterSpacing;
    public final boolean shadow;
    public final int shadowColor;
    public final float shadowOffset;

    private TextStyle(String font, float size, int color, Gradient gradient, Align align,
                      Baseline baseline, float letterSpacing,
                      boolean shadow, int shadowColor, float shadowOffset) {
        this.font = font;
        this.size = size;
        this.color = color;
        this.gradient = gradient;
        this.align = align;
        this.baseline = baseline;
        this.letterSpacing = letterSpacing;
        this.shadow = shadow;
        this.shadowColor = shadowColor;
        this.shadowOffset = shadowOffset;
    }

    public static TextStyle of(String font, float size, java.awt.Color color) {
        return of(font, size, color.getRGB());
    }

    /** Packed-ARGB overload, for interop with the client's int-based colour helpers. */
    public static TextStyle of(String font, float size, int color) {
        return new TextStyle(font, size, color, null, Align.LEFT, Baseline.TOP, 0f,
                false, 0x80000000, 1f);
    }

    public TextStyle color(java.awt.Color color) {
        return color(color.getRGB());
    }

    public TextStyle color(int argb) {
        return new TextStyle(font, size, argb, null, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    /**
     * Paints the run with a gradient instead of a flat colour. A horizontal gradient runs
     * across the whole string, a vertical one down each glyph's line box.
     */
    public TextStyle gradient(Gradient gradient) {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    public TextStyle size(float size) {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    public TextStyle font(String font) {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    public TextStyle align(Align align) {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    public TextStyle baseline(Baseline baseline) {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    public TextStyle letterSpacing(float letterSpacing) {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                shadow, shadowColor, shadowOffset);
    }

    public TextStyle shadow() {
        return shadow(0x80000000, 1f);
    }

    public TextStyle shadow(java.awt.Color color, float offset) {
        return shadow(color.getRGB(), offset);
    }

    public TextStyle shadow(int color, float offset) {
        return new TextStyle(font, size, this.color, gradient, align, baseline, letterSpacing,
                true, color, offset);
    }

    public TextStyle noShadow() {
        return new TextStyle(font, size, color, gradient, align, baseline, letterSpacing,
                false, shadowColor, shadowOffset);
    }
}
