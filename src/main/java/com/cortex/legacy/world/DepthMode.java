package com.cortex.legacy.world;

/**
 * Whether world geometry is hidden by the blocks in front of it.
 *
 * <p>This is a batch key: geometry only merges with geometry that shares its depth mode, so
 * switching costs a draw call. Drawing everything through-walls first and everything
 * occluded second keeps that to one switch per frame rather than one per shape.
 */
public enum DepthMode {

    /**
     * Visible through terrain — the usual choice for ESP and tracers, whose whole purpose
     * is to show what cannot be seen.
     */
    THROUGH_WALLS,

    /**
     * Hidden behind whatever is in front of it, so the geometry sits in the world the way
     * an ordinary block would. Wanted for markers meant to read as part of the scene.
     */
    OCCLUDED
}
