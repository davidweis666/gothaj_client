package com.cortex.legacy.text.font;

import com.cortex.legacy.text.msdf.MsdfGenerator;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Turns an AWT font into a baked MSDF atlas.
 *
 * <p>Baking happens once at a fixed em size and the results are stored in em units, because
 * a distance field is resolution-independent — the same atlas serves 8px labels and 72px
 * headings equally well.
 *
 * <p>Produces plain data and touches no GL state, so it is safe to run off the render thread.
 */
public final class FontBaker {

    /** Em size the outlines are rasterised at. Larger costs bake time, not quality at draw time. */
    private static final int EM_PIXELS = 40;

    /** Width of the usable distance ramp, in cell pixels. Bounds how far a glow or outline can reach. */
    private static final int PX_RANGE = 6;

    /** Keeps one enormous glyph from blowing the atlas up for everyone else. */
    private static final int MAX_CELL = 160;

    private static final char FIRST_CHAR = 32;
    private static final char LAST_CHAR = 126;

    private FontBaker() {
    }

    /** Loads a {@code .ttf}/{@code .otf} from disk or from the client's assets. */
    public static Font loadFont(String path) throws IOException, FontFormatException {
        File file = new File(path);
        if (file.isFile()) {
            return Font.createFont(Font.TRUETYPE_FONT, file);
        }

        try (InputStream in = net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                .getResource(new net.minecraft.util.ResourceLocation("gothaj", path)).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        }
    }

    /** Bakes the printable ASCII range of {@code font} into an atlas. */
    public static BakedFont bake(Font font) {
        Font sized = font.deriveFont(Font.PLAIN, (float) EM_PIXELS);
        FontRenderContext context = new FontRenderContext(new AffineTransform(), true, true);

        // Measure every glyph first: the atlas cell has to fit the widest one, plus room
        // for the distance ramp to fall off on both sides.
        Map<Character, Rectangle2D> bounds = new HashMap<>();
        Map<Character, Float> advances = new HashMap<>();

        double widest = 1;
        for (char c = FIRST_CHAR; c <= LAST_CHAR; c++) {
            GlyphVector vector = sized.createGlyphVector(context, String.valueOf(c));
            Rectangle2D ink = vector.getOutline().getBounds2D();

            bounds.put(c, ink);
            advances.put(c, (float) vector.getGlyphMetrics(0).getAdvanceX() / EM_PIXELS);

            if (!ink.isEmpty()) {
                widest = Math.max(widest, Math.max(ink.getWidth(), ink.getHeight()));
            }
        }

        int cell = (int) Math.min(MAX_CELL, Math.ceil(widest) + PX_RANGE * 2 + 2);

        int glyphCount = LAST_CHAR - FIRST_CHAR + 1;
        int columns = (int) Math.ceil(Math.sqrt(glyphCount));
        int rows = (int) Math.ceil(glyphCount / (double) columns);

        int atlasWidth = columns * cell;
        int atlasHeight = rows * cell;
        byte[] atlas = new byte[atlasWidth * atlasHeight * 4];

        Map<Character, Glyph> glyphs = new HashMap<>();

        int index = 0;
        for (char c = FIRST_CHAR; c <= LAST_CHAR; c++, index++) {
            float advance = advances.get(c);
            Rectangle2D ink = bounds.get(c);

            if (ink.isEmpty()) {
                glyphs.put(c, Glyph.blank(advance));
                continue;
            }

            int column = index % columns;
            int row = index / columns;

            // Position the outline inside its cell, leaving a PX_RANGE margin so the
            // distance ramp has somewhere to go before it hits the cell edge.
            AffineTransform placement = AffineTransform.getTranslateInstance(
                    PX_RANGE + 1 - ink.getX(),
                    PX_RANGE + 1 - ink.getY());

            GlyphVector vector = sized.createGlyphVector(context, String.valueOf(c));
            byte[] field = MsdfGenerator.generate(
                    placement.createTransformedShape(vector.getOutline()), cell, PX_RANGE);

            blit(field, atlas, cell, column * cell, row * cell, atlasWidth);

            float originX = column * cell;
            float originY = row * cell;

            glyphs.put(c, new Glyph(
                    originX / atlasWidth,
                    originY / atlasHeight,
                    (originX + cell) / atlasWidth,
                    (originY + cell) / atlasHeight,
                    // The quad covers the whole cell, offset so the glyph's ink lands where
                    // its own bounds say it should relative to the pen.
                    (float) (ink.getX() - PX_RANGE - 1) / EM_PIXELS,
                    (float) (ink.getY() - PX_RANGE - 1) / EM_PIXELS,
                    (float) (ink.getX() - PX_RANGE - 1 + cell) / EM_PIXELS,
                    (float) (ink.getY() - PX_RANGE - 1 + cell) / EM_PIXELS,
                    advance));
        }

        // Line metrics straight off the font, rather than via a throwaway BufferedImage and
        // the Graphics it hands out — that Graphics is a native resource that has to be
        // disposed, and this path needs no drawing surface at all.
        LineMetrics metrics = sized.getLineMetrics("Hxy", context);

        return new BakedFont(
                atlas, atlasWidth, atlasHeight, PX_RANGE, glyphs,
                metrics.getAscent() / EM_PIXELS,
                metrics.getDescent() / EM_PIXELS,
                metrics.getHeight() / EM_PIXELS);
    }

    /** Copies one glyph's RGB field into the atlas, filling alpha with 255. */
    private static void blit(byte[] field, byte[] atlas, int cell,
                             int destX, int destY, int atlasWidth) {
        for (int y = 0; y < cell; y++) {
            int source = y * cell * 3;
            int dest = ((destY + y) * atlasWidth + destX) * 4;

            for (int x = 0; x < cell; x++) {
                atlas[dest] = field[source];
                atlas[dest + 1] = field[source + 1];
                atlas[dest + 2] = field[source + 2];
                atlas[dest + 3] = (byte) 255;
                source += 3;
                dest += 4;
            }
        }
    }
}
