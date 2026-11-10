package com.cortex.legacy.draw;

/**
 * An immutable axis-aligned rectangle in logical (GUI-scaled) pixels, matching the
 * coordinate space {@link net.minecraft.client.gui.ScaledResolution} reports.
 */
public final class Region {

    public final float x, y, w, h;

    public Region(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static Region of(float x, float y, float w, float h) {
        return new Region(x, y, w, h);
    }

    public static Region ofCorners(float x0, float y0, float x1, float y1) {
        return new Region(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0));
    }

    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public float centerX() {
        return x + w * 0.5f;
    }

    public float centerY() {
        return y + h * 0.5f;
    }

    /** Grows the region by {@code amount} on every side (negative shrinks it). */
    public Region inflate(float amount) {
        return new Region(x - amount, y - amount, w + amount * 2f, h + amount * 2f);
    }

    public Region offset(float dx, float dy) {
        return new Region(x + dx, y + dy, w, h);
    }

    @Override
    public String toString() {
        return "Region[" + x + ", " + y + ", " + w + " x " + h + "]";
    }
}
