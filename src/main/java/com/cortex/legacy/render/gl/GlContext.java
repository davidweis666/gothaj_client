package com.cortex.legacy.render.gl;

import org.lwjgl.opengl.GL;

/**
 * Guards the engine's GL entry points against threads that hold no context.
 *
 * <p>LWJGL does not throw when this happens — the native call aborts the JVM outright,
 * taking the game with it and printing a stack trace that indicts the innocent GL call
 * rather than the thread that had no business making it. Checking first turns a fatal
 * process kill into an ordinary exception the engine can report and survive.
 *
 * <p>Worth guarding because the mistake is easy to make: modules are enabled while configs
 * load on a background thread, so anything reachable from {@code onEnable} can arrive here
 * off the render thread.
 */
public final class GlContext {

    private GlContext() {
    }

    /**
     * @param what a short description of the operation, for the error message
     * @throws IllegalStateException if the calling thread has no current GL context
     */
    public static void requireCurrent(String what) {
        try {
            GL.getCapabilities();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Cortex: " + what
                    + " must happen on the render thread, but thread '"
                    + Thread.currentThread().getName() + "' holds no GL context", e);
        }
    }
}
