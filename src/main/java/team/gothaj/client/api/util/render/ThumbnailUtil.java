package team.gothaj.client.api.util.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import team.gothaj.client.api.util.music.request.SoundCloudTrack;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class ThumbnailUtil {
    
    private static final String DEFAULT_THUMBNAIL = "https://forums.gothaj.top/mp3/not_found.png";
    
    private static final Map<String, Integer> thumbnailCache = new HashMap<>();
    
    /**
     * Downloads and loads thumbnail texture for a given SoundCloud track
     * Falls back to default thumbnail if track thumbnail doesn't exist
     */
    public static int loadThumbnailTexture(SoundCloudTrack track) {
        if (track == null || track.getThumbnail() == null || track.getThumbnail().isEmpty()) {
            return loadDefaultThumbnail();
        }
        
        // Check cache first
        String cacheKey = "thumbnail_" + extractThumbnailId(track.getThumbnail());
        if (thumbnailCache.containsKey(cacheKey)) {
            return thumbnailCache.get(cacheKey);
        }
        
        int textureId = tryLoadTrackThumbnail(track.getThumbnail());
        if (textureId == -1) {
            // Fallback to default thumbnail
            textureId = loadDefaultThumbnail();
        }
        
        thumbnailCache.put(cacheKey, textureId);
        return textureId;
    }
    
    /**
     * Asynchronously load thumbnail texture
     */
    public static void loadThumbnailTextureAsync(SoundCloudTrack track) {
        CompletableFuture.supplyAsync(() -> loadThumbnailTexture(track));
    }
    
    /**
     * Load thumbnail from URL string
     */
    public static int loadThumbnailTexture(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            return loadDefaultThumbnail();
        }
        
        // Check cache first
        String cacheKey = "thumbnail_" + extractThumbnailId(thumbnailUrl);
        if (thumbnailCache.containsKey(cacheKey)) {
            return thumbnailCache.get(cacheKey);
        }
        
        int textureId = tryLoadTrackThumbnail(thumbnailUrl);
        if (textureId == -1) {
            textureId = loadDefaultThumbnail();
        }
        
        thumbnailCache.put(cacheKey, textureId);
        return textureId;
    }
    
    private static String extractThumbnailId(String thumbnailUrl) {
        // Extract artwork ID from SoundCloud thumbnail URL
        // e.g., https://i1.sndcdn.com/artworks-DQXyQEPw5ycX-0-original.jpg -> DQXyQEPw5ycX
        if (thumbnailUrl.contains("artworks-")) {
            try {
                String[] parts = thumbnailUrl.split("artworks-")[1].split("-");
                return parts[0];
            } catch (Exception e) {
                return thumbnailUrl.hashCode() + "";
            }
        }
        return thumbnailUrl.hashCode() + "";
    }
    
    private static int tryLoadTrackThumbnail(String thumbnailUrl) {
        try {
            BufferedImage image = ImageIO.read(new URL(thumbnailUrl));
            if (image != null) {
                return createTextureFromImage(image);
            }
        } catch (IOException e) {
            // Thumbnail failed to load
            return -1;
        }
        return -1;
    }
    
    private static int loadDefaultThumbnail() {
        String cacheKey = "default_thumbnail";
        if (thumbnailCache.containsKey(cacheKey)) {
            return thumbnailCache.get(cacheKey);
        }
        
        try {
            BufferedImage image = ImageIO.read(new URL(DEFAULT_THUMBNAIL));
            int textureId = createTextureFromImage(image);
            thumbnailCache.put(cacheKey, textureId);
            return textureId;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return -1;
        }
    }
    
    private static int createTextureFromImage(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        
        buffer.flip();
        
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 
                     0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        
        return textureId;
    }

    /**
     * Draw SoundCloud track thumbnail
     */
    public static void drawTrackThumbnail(SoundCloudTrack track, float x, float y, float width, float height,
                                         float radius, float alpha) {
        int thumbnailTexture = loadThumbnailTexture(track);

        if (thumbnailTexture != -1) {
            glBindTexture(GL_TEXTURE_2D, thumbnailTexture);
            RoundedUtil.drawRoundTextured(x, y, width, height, radius, alpha);
        }
    }

    /**
     * Draw thumbnail from URL
     */
    public static void drawThumbnail(String thumbnailUrl, float x, float y, float width, float height,
                                    float radius, float alpha) {
        int thumbnailTexture = loadThumbnailTexture(thumbnailUrl);

        if (thumbnailTexture != -1) {
            glBindTexture(GL_TEXTURE_2D, thumbnailTexture);
            RoundedUtil.drawRoundTextured(x, y, width, height, radius, alpha);
        }
    }

    /**
     * Clear thumbnail cache to free memory
     */
    public static void clearCache() {
        for (int textureId : thumbnailCache.values()) {
            if (textureId != -1) {
                glDeleteTextures(textureId);
            }
        }
        thumbnailCache.clear();
    }
    
    /**
     * Remove specific thumbnail from cache
     */
    public static void removeThumbnailFromCache(SoundCloudTrack track) {
        String cacheKey = "thumbnail_" + extractThumbnailId(track.getThumbnail());
        Integer textureId = thumbnailCache.remove(cacheKey);
        if (textureId != null && textureId != -1) {
            glDeleteTextures(textureId);
        }
    }
}
