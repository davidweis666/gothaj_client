package team.gothaj.client.api.util.other.colorthief;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ColorPaletteService {

    public record ColorPalette(Color primary, Color secondary, Color tertiary, Color accent) {
    }

    private static final Map<String, ColorPalette> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile boolean imageIoCacheDisabled = false;

    public interface Callback {
        void onResult(String trackId, ColorPalette palette);
        void onFail(String trackId, Throwable error);
    }

    public static ColorPalette getCached(String trackId) {
        // Add null check to prevent NPE
        if (trackId == null || trackId.isEmpty()) {
            return null;
        }
        return CACHE.get(trackId);
    }

    public static void clearCache() {
        CACHE.clear();
        LOADING.clear();
    }

    public static void requestPalette(String trackId, String thumbnailUrl, Callback callback) {
        if (trackId == null || trackId.isEmpty() || thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            if (callback != null) callback.onFail(trackId, new IllegalArgumentException("Invalid trackId or thumbnailUrl"));
            return;
        }

        ColorPalette cached = CACHE.get(trackId);
        if (cached != null) {
            if (callback != null) callback.onResult(trackId, cached);
            return;
        }

        if (!LOADING.add(trackId)) return;

        if (!imageIoCacheDisabled) {
            ImageIO.setUseCache(false);
            imageIoCacheDisabled = true;
        }

        Thread worker = new Thread(() -> {
            try {
                BufferedImage img = readImageFast(thumbnailUrl);
                if (img == null) throw new RuntimeException("null image");
                img = resizeFast(img, 32);
                int[][] pal = ColorThief.getPalette(img, 4);

                assert pal != null;
                Color primary = new Color(pal[0][0], pal[0][1], pal[0][2]);
                Color secondary = pal.length > 1 ? new Color(pal[1][0], pal[1][1], pal[1][2]) : adjustBrightness(primary, 0.8f);
                Color tertiary = pal.length > 2 ? new Color(pal[2][0], pal[2][1], pal[2][2]) : adjustBrightness(primary, 0.6f);
                Color accent = pal.length > 3 ? new Color(pal[3][0], pal[3][1], pal[3][2]) : adjustBrightness(primary, 1.2f);

                ColorPalette cp = new ColorPalette(primary, secondary, tertiary, accent);


                if (trackId != null && !trackId.isEmpty()) {
                    CACHE.put(trackId, cp);
                }
                LOADING.remove(trackId);
                if (callback != null) callback.onResult(trackId, cp);
            } catch (Throwable t) {
                LOADING.remove(trackId);
                if (callback != null) callback.onFail(trackId, t);
            }
        }, "PaletteWorker-" + trackId);
        worker.setDaemon(true);
        worker.start();
    }

    private static BufferedImage readImageFast(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            c.setInstanceFollowRedirects(true);
            c.addRequestProperty("User-Agent", "GothajClient/1.0");
            try (InputStream in = new java.io.BufferedInputStream(c.getInputStream(), 32768)) {
                return ImageIO.read(in);
            } finally {
                c.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage resizeFast(BufferedImage src, int max) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= max && h <= max) return src;
        double s = Math.min((double) max / w, (double) max / h);
        int nw = Math.max(1, (int) (w * s));
        int nh = Math.max(1, (int) (h * s));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static Color adjustBrightness(Color color, float factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1], Math.min(1.0f, hsb[2] * factor));
    }
}
