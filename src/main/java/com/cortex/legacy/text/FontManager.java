package com.cortex.legacy.text;

import com.cortex.legacy.text.font.BakedFont;
import com.cortex.legacy.text.font.FontBaker;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Registers fonts by name and hands back baked atlases.
 *
 * <p>Baking an ASCII range costs a few hundred milliseconds of pure CPU work, so it runs on
 * a background thread. Until it finishes {@link #get} returns {@code null} and callers fall
 * back to Minecraft's own font — text keeps rendering rather than the frame stalling.
 */
public final class FontManager {

    private final Map<String, Future<BakedFont>> baking = new ConcurrentHashMap<>();
    private final Map<String, BakedFont> ready = new HashMap<>();

    /**
     * Names whose bake or upload failed. Checked before anything else in {@link #get} so a
     * broken font costs one log line rather than one per frame for the rest of the session.
     */
    private final Set<String> failed = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Cortex-FontBaker");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Registers a font file, starting the bake immediately.
     *
     * <p>Registering a name that failed before retries it, which is the only way back from
     * a transient failure such as a missing file that has since been put in place.
     *
     * @param name a handle to pass to {@link #get}
     * @param path a {@code .ttf}/{@code .otf} on disk, or under {@code assets/gothaj/}
     */
    public void register(String name, String path) {
        submit(name, () -> FontBaker.bake(FontBaker.loadFont(path)));
    }

    /** Registers one of the fonts already installed on the system. */
    public void registerSystem(String name, String family) {
        submit(name, () -> FontBaker.bake(new Font(family, Font.PLAIN, 40)));
    }

    /**
     * Queues a bake under {@code name}, unless one is already queued or done.
     *
     * <p>{@code computeIfAbsent} rather than a containsKey/put pair: registration commonly
     * happens while a config loads off the main thread, and the pair would let two callers
     * both decide the name was free and start two bakes of the same font.
     */
    private void submit(String name, Callable<BakedFont> bake) {
        failed.remove(name);

        // A successful bake leaves its future in place, so this also covers names that are
        // already done — no need to consult `ready`, which belongs to the render thread.
        baking.computeIfAbsent(name, key -> executor.submit(bake));
    }

    /**
     * Returns the baked font, or {@code null} while it is still baking or if it failed.
     *
     * <p>Must be called on the render thread: the first successful call uploads the atlas.
     */
    public BakedFont get(String name) {
        BakedFont font = ready.get(name);
        if (font != null) return font;

        if (failed.contains(name)) return null;

        Future<BakedFont> pending = baking.get(name);
        if (pending == null || !pending.isDone()) return null;

        try {
            font = pending.get();
            if (font == null) {
                throw new IllegalStateException("the baker produced no atlas");
            }

            font.upload();
            ready.put(name, font);
            return font;
        } catch (Exception e) {
            markFailed(name, e);
            return null;
        }
    }

    private void markFailed(String name, Exception cause) {
        failed.add(name);
        baking.remove(name);

        // An ExecutionException says nothing on its own; what went wrong is the cause it
        // wraps. Anything thrown here directly is already the interesting one.
        Throwable reported = cause.getCause() != null ? cause.getCause() : cause;
        System.err.println("Cortex: could not bake font '" + name + "': " + reported);
    }

    public boolean isReady(String name) {
        return ready.containsKey(name);
    }

    public void dispose() {
        executor.shutdownNow();
        for (BakedFont font : ready.values()) {
            font.dispose();
        }
        ready.clear();
        baking.clear();
        failed.clear();
    }
}
