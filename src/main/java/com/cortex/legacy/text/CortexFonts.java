package com.cortex.legacy.text;

import com.cortex.legacy.Cortex;

/** Where the client's fonts are registered, once, at start-up. */
public final class CortexFonts {

    private CortexFonts() {
    }

    public static void loadFonts() {
        Cortex.fonts().registerSystem("cortex-demo", "Arial");
    }
}
