package com.cortex.legacy.image;

/**
 * A decoded animation: every frame already uploaded, plus how long each one is shown.
 *
 * <p>Frames are uploaded once at load time rather than streamed, because an animation's
 * content never changes — only which frame is current does.
 */
public final class AnimatedImage {

    private final Image[] frames;
    private final int[] delaysMs;
    private final int totalDurationMs;

    private final long createdAt = System.currentTimeMillis();

    AnimatedImage(Image[] frames, int[] delaysMs) {
        this.frames = frames;
        this.delaysMs = delaysMs;

        int total = 0;
        for (int delay : delaysMs) {
            total += delay;
        }
        this.totalDurationMs = Math.max(1, total);
    }

    public int frameCount() {
        return frames.length;
    }

    /** How long one full loop takes, in milliseconds. */
    public int totalDurationMs() {
        return totalDurationMs;
    }

    /** How long frame {@code index} is shown, in milliseconds. */
    public int delayMs(int index) {
        return delaysMs[index];
    }

    public int width() {
        return frames.length == 0 ? 0 : frames[0].width;
    }

    public int height() {
        return frames.length == 0 ? 0 : frames[0].height;
    }

    /** The frame that should be showing right now. */
    public Image currentFrame() {
        return frameAt(System.currentTimeMillis() - createdAt);
    }

    /** The frame showing {@code elapsedMs} into the animation, looping. */
    public Image frameAt(long elapsedMs) {
        if (frames.length == 1) return frames[0];

        long position = Math.floorMod(elapsedMs, totalDurationMs);

        for (int i = 0; i < frames.length; i++) {
            position -= delaysMs[i];
            if (position < 0) return frames[i];
        }
        return frames[frames.length - 1];
    }

    public void dispose() {
        for (Image frame : frames) {
            frame.dispose();
        }
    }
}
