package com.cortex.legacy.draw;

import java.awt.Color;

/**
 * A two-stop linear gradient, optionally animated.
 *
 * <p>Gradients cost nothing extra to draw: every vertex already carries both stops and its
 * own position along the axis, so the blend happens in the same pass as the shape.
 *
 * <pre>{@code
 * Gradient.horizontal(Color.RED, Color.BLUE)             // static
 * Gradient.horizontal(Color.RED, Color.BLUE).animate(1f) // scrolls, one cycle a second
 * }</pre>
 *
 * <p>Animation slides the ramp along the axis rather than cross-fading the two stops —
 * cross-fading would pass through a flat colour halfway through every cycle. The shader
 * mirrors the ramp as it wraps, so the motion is seamless in both directions.
 */
public final class Gradient {

    public enum Axis {
        /** Left to right. */
        HORIZONTAL,
        /** Top to bottom. */
        VERTICAL
    }

    public final int start;
    public final int end;
    public final Axis axis;

    /** Cycles per second; {@code 0} holds the gradient still. */
    public final float speed;

    /**
     * Seconds since the engine started, sampled once per frame.
     *
     * <p>Shared rather than read per gradient for two reasons: it keeps a clock lookup off
     * a path that runs for every shape, and it guarantees that a gradient reused across
     * several draws in one frame lands on exactly the same phase in all of them.
     */
    private static float frameTime;

    /** Set by the engine at the start of each frame. */
    public static void setFrameTime(float seconds) {
        frameTime = seconds;
    }

    public Gradient(int start, int end, Axis axis, float speed) {
        this.start = start;
        this.end = end;
        this.axis = axis;
        this.speed = speed;
    }

    public static Gradient horizontal(Color start, Color end) {
        return new Gradient(start.getRGB(), end.getRGB(), Axis.HORIZONTAL, 0f);
    }

    public static Gradient vertical(Color start, Color end) {
        return new Gradient(start.getRGB(), end.getRGB(), Axis.VERTICAL, 0f);
    }

    /** Packed-ARGB overload, for the client's existing int-based colour helpers. */
    public static Gradient horizontal(int start, int end) {
        return new Gradient(start, end, Axis.HORIZONTAL, 0f);
    }

    public static Gradient vertical(int start, int end) {
        return new Gradient(start, end, Axis.VERTICAL, 0f);
    }

    /**
     * Makes the gradient scroll along its axis.
     *
     * @param cyclesPerSecond how many full sweeps happen each second
     */
    public Gradient animate(float cyclesPerSecond) {
        return new Gradient(start, end, axis, cyclesPerSecond);
    }

    public Gradient axis(Axis axis) {
        return new Gradient(start, end, axis, speed);
    }

    /**
     * How far the ramp has slid this frame, in gradient-position units.
     *
     * <p>Wrapped to one wave period so it stays precise no matter how long the client has
     * been running.
     */
    public float offset() {
        if (speed == 0f) return 0f;

        // The shader mirrors every 2 units, so anything beyond that repeats.
        return (frameTime * speed * 2f) % 2f;
    }

    public static int lerpArgb(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return (int) (from + (to - from) * t + 0.5f);
    }
}
