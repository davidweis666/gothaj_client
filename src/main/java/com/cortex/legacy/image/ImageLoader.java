package com.cortex.legacy.image;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decodes images and animations, caching by source path.
 *
 * <p>Failed loads are cached too. Without that, a missing asset would be retried — and
 * logged — every single frame by whatever module asked for it.
 */
public final class ImageLoader {

    private final Map<String, Image> images = new HashMap<>();
    private final Map<String, AnimatedImage> animations = new HashMap<>();

    /**
     * Paths that could not be loaded, tracked per kind.
     *
     * <p>Kept apart because the two paths can genuinely disagree: a format with no
     * animation reader still decodes as a still, and a file that fails as one is not
     * thereby known to fail as the other. Sharing one set let a failure on either side
     * silently disable the other for the same path.
     */
    private final Set<String> unavailableImages = new HashSet<>();
    private final Set<String> unavailableAnimations = new HashSet<>();

    /** Loads from {@code assets/gothaj/<path>}, or returns {@code null} if it cannot be read. */
    public Image load(String path) {
        Image cached = images.get(path);
        if (cached != null) return cached;
        if (unavailableImages.contains(path)) return null;

        try (InputStream in = open(path)) {
            BufferedImage decoded = ImageIO.read(in);
            if (decoded == null) {
                throw new IOException("no decoder accepted the data");
            }

            Image image = Image.upload(decoded);
            images.put(path, image);
            return image;
        } catch (Exception e) {
            markUnavailable(unavailableImages, path, e);
            return null;
        }
    }

    /**
     * Loads an animation. Non-animated formats come back as a single-frame animation, so
     * callers do not have to care which they were given.
     */
    public AnimatedImage loadAnimated(String path) {
        AnimatedImage cached = animations.get(path);
        if (cached != null) return cached;
        if (unavailableAnimations.contains(path)) return null;

        try (InputStream in = open(path);
             ImageInputStream stream = ImageIO.createImageInputStream(in)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IOException("no decoder accepted the data");
            }

            ImageReader reader = readers.next();
            AnimatedImage animation;
            try {
                reader.setInput(stream);
                animation = readFrames(reader);
            } finally {
                reader.dispose();
            }

            animations.put(path, animation);
            return animation;
        } catch (Exception e) {
            markUnavailable(unavailableAnimations, path, e);
            return null;
        }
    }

    private static AnimatedImage readFrames(ImageReader reader) throws IOException {
        int count = reader.getNumImages(true);

        List<Image> frames = new ArrayList<>(count);
        List<Integer> delays = new ArrayList<>(count);

        // GIF frames may be partial updates layered onto what came before, so each one is
        // composited onto a running canvas rather than uploaded on its own.
        Dimension screen = logicalScreenSize(reader, count);
        BufferedImage canvas = new BufferedImage(screen.width, screen.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();

        try {
            for (int i = 0; i < count; i++) {
                BufferedImage frame = reader.read(i);
                IIOMetadataNode meta = frameMetadata(reader, i);

                // A GIF frame is a patch, not a full picture: it carries its own position on
                // the logical screen and only covers the region that actually changed.
                int left = intAttribute(meta, "ImageDescriptor", "imageLeftPosition", 0);
                int top = intAttribute(meta, "ImageDescriptor", "imageTopPosition", 0);
                String disposal = stringAttribute(meta, "GraphicControlExtension",
                        "disposalMethod", "none");

                // "restoreToPrevious" rewinds the canvas after this frame, so it has to be
                // captured before the patch lands on it.
                BufferedImage restore = "restoreToPrevious".equals(disposal) ? copyOf(canvas) : null;

                graphics.drawImage(frame, left, top, null);

                frames.add(Image.upload(canvas));
                delays.add(frameDelayMs(meta));

                if ("restoreToBackgroundColor".equals(disposal)) {
                    // Clear only this frame's rectangle back to transparent. Skipping this is
                    // what leaves trails smeared behind moving subjects.
                    graphics.setComposite(AlphaComposite.Clear);
                    graphics.fillRect(left, top, frame.getWidth(), frame.getHeight());
                    graphics.setComposite(AlphaComposite.SrcOver);
                } else if (restore != null) {
                    graphics.dispose();
                    canvas = restore;
                    graphics = canvas.createGraphics();
                }
            }

            if (frames.isEmpty()) {
                throw new IOException("image contained no frames");
            }
        } catch (IOException | RuntimeException e) {
            // Each frame decoded so far is already a live GL texture, and nothing else has
            // a handle on them: a GIF that turns out to be truncated halfway through would
            // otherwise strand one texture per frame it managed to read.
            for (int i = 0; i < frames.size(); i++) {
                frames.get(i).dispose();
            }
            throw e;
        } finally {
            graphics.dispose();
        }

        int[] delayArray = new int[delays.size()];
        for (int i = 0; i < delayArray.length; i++) {
            delayArray[i] = delays.get(i);
        }
        return new AnimatedImage(frames.toArray(new Image[0]), delayArray);
    }

    /**
     * The canvas every frame composites onto.
     *
     * <p>Taken from the GIF's logical screen rather than the first frame's size — the first
     * frame is often smaller than the animation as a whole, and sizing to it would clip
     * everything that follows.
     */
    private static Dimension logicalScreenSize(ImageReader reader, int frameCount) throws IOException {
        try {
            IIOMetadata metadata = reader.getStreamMetadata();
            if (metadata != null) {
                IIOMetadataNode root =
                        (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                IIOMetadataNode descriptor = firstChild(root, "LogicalScreenDescriptor");

                if (descriptor != null) {
                    int width = Integer.parseInt(descriptor.getAttribute("logicalScreenWidth"));
                    int height = Integer.parseInt(descriptor.getAttribute("logicalScreenHeight"));
                    if (width > 0 && height > 0) {
                        return new Dimension(width, height);
                    }
                }
            }
        } catch (Exception ignored) {
            // Not a GIF, or metadata unavailable — fall back to measuring the frames.
        }

        int width = 1;
        int height = 1;
        for (int i = 0; i < frameCount; i++) {
            width = Math.max(width, reader.getWidth(i));
            height = Math.max(height, reader.getHeight(i));
        }
        return new Dimension(width, height);
    }

    private static BufferedImage copyOf(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static IIOMetadataNode frameMetadata(ImageReader reader, int frameIndex) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(frameIndex);
            return (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
        } catch (Exception e) {
            return null;
        }
    }

    private static IIOMetadataNode firstChild(IIOMetadataNode parent, String name) {
        if (parent == null) return null;
        NodeList matches = parent.getElementsByTagName(name);
        return matches.getLength() == 0 ? null : (IIOMetadataNode) matches.item(0);
    }

    private static String stringAttribute(IIOMetadataNode root, String element,
                                          String attribute, String fallback) {
        IIOMetadataNode node = firstChild(root, element);
        if (node == null) return fallback;

        String value = node.getAttribute(attribute);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static int intAttribute(IIOMetadataNode root, String element,
                                    String attribute, int fallback) {
        try {
            return Integer.parseInt(stringAttribute(root, element, attribute,
                    String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Reads a frame's delay, in milliseconds. */
    private static int frameDelayMs(IIOMetadataNode meta) {
        int centiseconds = intAttribute(meta, "GraphicControlExtension", "delayTime", 0);

        // Encoders routinely write 0 or 1 to mean "as fast as possible". Browsers treat
        // anything that short as 100ms, and matching them is what makes a GIF play at the
        // speed it was authored to rather than several times too fast.
        if (centiseconds <= 1) {
            return 100;
        }
        return centiseconds * 10;
    }

    /**
     * Opens an image by path, accepting every form that reads naturally:
     *
     * <ul>
     *   <li>{@code gothaj/img/logo.png} — the asset path as it appears on disk</li>
     *   <li>{@code img/logo.png} — relative to this client's own assets</li>
     *   <li>{@code minecraft:textures/...} — an explicit namespace</li>
     *   <li>{@code C:/somewhere/logo.png} — a file outside the jar</li>
     * </ul>
     */
    private static InputStream open(String path) throws IOException {
        File file = new File(path);
        if (file.isFile()) {
            return new FileInputStream(file);
        }

        if (path.indexOf(':') >= 0) {
            return openResource(new ResourceLocation(path));
        }

        // A leading directory may be either an asset namespace or just the first folder
        // inside this client's assets. Try it as a namespace, then fall back — which means
        // both spellings work without the caller having to know the difference.
        int slash = path.indexOf('/');
        if (slash > 0) {
            try {
                return openResource(new ResourceLocation(
                        path.substring(0, slash), path.substring(slash + 1)));
            } catch (Exception ignored) {
                // Not a namespace; treat the whole string as a gothaj-relative path.
            }
        }

        return openResource(new ResourceLocation("gothaj", path));
    }

    private static InputStream openResource(ResourceLocation location) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
    }

    private static void markUnavailable(Set<String> unavailable, String path, Exception cause) {
        unavailable.add(path);
        System.err.println("Cortex: could not load image '" + path + "': " + cause);
    }

    public void dispose() {
        for (Image image : images.values()) {
            image.dispose();
        }
        for (AnimatedImage animation : animations.values()) {
            animation.dispose();
        }
        images.clear();
        animations.clear();
        unavailableImages.clear();
        unavailableAnimations.clear();
    }
}
