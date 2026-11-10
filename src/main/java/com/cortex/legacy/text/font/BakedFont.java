package com.cortex.legacy.text.font;

import org.lwjgl.system.MemoryUtil;
import com.cortex.legacy.image.Image;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A baked font: the atlas plus every glyph's placement, all in em units.
 *
 * <p>Created as pure data by {@link FontBaker}; {@link #upload()} is the only part that
 * needs the render thread.
 */
public final class BakedFont {

    /** Dropped once uploaded — see {@link #upload()}. */
    private byte[] atlasRgba;

    private final int atlasWidth;
    private final int atlasHeight;
    private final float pxRange;

    private final Map<Character, Glyph> glyphs;

    public final float ascent;
    public final float descent;
    public final float lineHeight;

    private Image atlas;

    BakedFont(byte[] atlasRgba, int atlasWidth, int atlasHeight, float pxRange,
              Map<Character, Glyph> glyphs, float ascent, float descent, float lineHeight) {
        this.atlasRgba = atlasRgba;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.pxRange = pxRange;
        this.glyphs = glyphs;
        this.ascent = ascent;
        this.descent = descent;
        this.lineHeight = lineHeight;
    }

    /**
     * Uploads the atlas. Must run on the render thread; repeat calls are no-ops.
     *
     * <p>The CPU copy is released once the GPU has it. A full ASCII atlas is several
     * megabytes of RGBA and nothing reads it again, so holding it for the lifetime of the
     * font would be pure waste. The consequence is that a font cannot be re-uploaded after
     * {@link #dispose()} — the atlas would have to be baked again — which is why disposal
     * only ever happens at teardown.
     */
    public void upload() {
        if (atlas != null || atlasRgba == null) return;

        ByteBuffer buffer = MemoryUtil.memAlloc(atlasRgba.length);
        buffer.put(atlasRgba).flip();
        try {
            // Explicitly unmipmapped: averaging texels between mip levels destroys the
            // distance field, and the shader's own screen-space ramp already keeps glyphs
            // crisp at every size.
            atlas = Image.upload(buffer, atlasWidth, atlasHeight, false);
            atlasRgba = null;
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    public int texture() {
        return atlas == null ? 0 : atlas.texture;
    }

    public int atlasWidth() {
        return atlasWidth;
    }

    public int atlasHeight() {
        return atlasHeight;
    }

    public float pxRange() {
        return pxRange;
    }

    /** Falls back to a space's metrics for anything outside the baked range. */
    public Glyph glyph(char c) {
        Glyph glyph = glyphs.get(c);
        return glyph != null ? glyph : glyphs.get(' ');
    }

    public float width(String text, float sizePx) {
        float total = 0f;
        for (int i = 0; i < text.length(); i++) {
            total += glyph(text.charAt(i)).advance;
        }
        return total * sizePx;
    }

    public void dispose() {
        if (atlas != null) {
            atlas.dispose();
            atlas = null;
        }
    }
}
