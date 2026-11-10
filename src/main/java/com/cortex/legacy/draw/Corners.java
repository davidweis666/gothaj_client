package com.cortex.legacy.draw;

/**
 * Immutable per-corner radii, in logical pixels, ordered top-left, top-right,
 * bottom-right, bottom-left.
 *
 * <p>Unlike most rounded-rect helpers in this codebase, the Cortex shape shader
 * evaluates each corner independently, so mixed radii render correctly.
 */
public final class Corners {

    public static final Corners NONE = new Corners(0f, 0f, 0f, 0f);

    public final float tl, tr, br, bl;

    public Corners(float tl, float tr, float br, float bl) {
        this.tl = tl;
        this.tr = tr;
        this.br = br;
        this.bl = bl;
    }

    public static Corners all(float radius) {
        return new Corners(radius, radius, radius, radius);
    }

    public static Corners of(float tl, float tr, float br, float bl) {
        return new Corners(tl, tr, br, bl);
    }

    public static Corners top(float radius) {
        return new Corners(radius, radius, 0f, 0f);
    }

    public static Corners bottom(float radius) {
        return new Corners(0f, 0f, radius, radius);
    }

    public float max() {
        return Math.max(Math.max(tl, tr), Math.max(br, bl));
    }

    /**
     * Clamps every radius so no corner can exceed half of the smaller region axis.
     *
     * <p>Allocates, so it is not for the draw path — see {@link #limitFor} for the variant
     * hot code uses.
     */
    public Corners clampTo(float width, float height) {
        float limit = limitFor(width, height);
        return new Corners(
                Math.min(tl, limit),
                Math.min(tr, limit),
                Math.min(br, limit),
                Math.min(bl, limit));
    }

    /**
     * The largest radius a region of this size can take.
     *
     * <p>Draw calls clamp against this inline rather than building a clamped copy: a
     * {@code Corners} per shape per frame is thousands of short-lived objects a second on
     * a busy HUD, for a value each caller uses once and discards.
     */
    public static float limitFor(float width, float height) {
        return Math.max(0f, Math.min(width, height) * 0.5f);
    }

    public boolean isZero() {
        return tl == 0f && tr == 0f && br == 0f && bl == 0f;
    }
}
