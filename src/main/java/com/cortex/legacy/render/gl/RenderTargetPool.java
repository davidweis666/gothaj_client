package com.cortex.legacy.render.gl;

import java.util.ArrayList;
import java.util.List;

/**
 * Reuses offscreen targets across frames.
 *
 * <p>A blur chain acquires several differently-sized targets every frame; creating fresh
 * textures each time would thrash VRAM badly enough to matter. Targets handed out during a
 * frame are returned in bulk by {@link #endFrame()}, so effect code never has to pair up
 * its own releases.
 *
 * <p>Free targets live in a flat list searched linearly rather than in a map keyed by size.
 * A map would have to box a {@code long} key on every acquire and every release — hundreds
 * of throwaway objects a second — to search a collection that never holds more than a
 * couple of dozen entries, which a linear scan walks faster anyway.
 *
 * <p>Free targets are also evicted once they go unused for {@link #IDLE_FRAMES_BEFORE_EVICT}
 * frames. A HUD whose panels hold still asks for the same handful of sizes forever and never
 * loses one, but a panel that animates its width asks for a different size on every frame of
 * the animation, and each of those is a full-height RGBA8 texture. Without eviction those
 * accumulate for the lifetime of the session.
 */
public final class RenderTargetPool {

    /**
     * How long an unused target is kept before being deleted, in frames.
     *
     * <p>Comfortably longer than any gap between two frames that use the same size — a
     * panel drawn only while a menu is open still finds its targets on reopening — and
     * short enough that a size produced by an animation is gone within a few seconds.
     */
    private static final int IDLE_FRAMES_BEFORE_EVICT = 600;

    private final List<RenderTarget> free = new ArrayList<>();
    private final List<RenderTarget> leased = new ArrayList<>();

    private int frame;

    public RenderTarget acquire(int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);

        // Searched back to front: the same sizes recur every frame in the same order, so
        // the match is usually among the most recently returned.
        for (int i = free.size() - 1; i >= 0; i--) {
            RenderTarget candidate = free.get(i);

            if (candidate.width == width && candidate.height == height) {
                free.remove(i);
                leased.add(candidate);
                return candidate;
            }
        }

        RenderTarget created = RenderTarget.create(width, height);
        leased.add(created);
        return created;
    }

    /** Returns every target leased this frame to the free list, and evicts stale ones. */
    public void endFrame() {
        frame++;

        // Indexed rather than enhanced-for: the latter allocates an iterator every frame.
        for (int i = 0; i < leased.size(); i++) {
            RenderTarget target = leased.get(i);
            target.lastUsedFrame = frame;
            free.add(target);
        }
        leased.clear();

        evictStale();
    }

    /**
     * Deletes free targets that have not been wanted in a long time.
     *
     * <p>Walks backwards so removing an entry cannot skip the next one.
     */
    private void evictStale() {
        for (int i = free.size() - 1; i >= 0; i--) {
            RenderTarget target = free.get(i);

            if (frame - target.lastUsedFrame > IDLE_FRAMES_BEFORE_EVICT) {
                free.remove(i);
                target.dispose();
            }
        }
    }

    /**
     * Drops every pooled target. Called on resize, where the old sizes will never be
     * requested again and would otherwise sit in the free list holding VRAM.
     */
    public void dispose() {
        for (int i = 0; i < leased.size(); i++) {
            leased.get(i).dispose();
        }
        leased.clear();

        for (int i = 0; i < free.size(); i++) {
            free.get(i).dispose();
        }
        free.clear();
    }
}
