package team.gothaj.client.api.util.render.gif;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.api.util.other.FileUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GifUtil {
    private final List<ResourceLocation> frameTextures = new ArrayList<>();
    private final List<Integer> frameDelays = new ArrayList<>();
    private int currentFrame = 0;
    private long lastFrameTime = 0;
    @Getter
    private boolean loaded = false;

    public void load(File file, String baseName, Function<BufferedImage, BufferedImage> frameProcessor) {
        new Thread(() -> {
            try {
                InputStream in = new FileInputStream(file);
                GifDecoder.GifImage gifImage = GifDecoder.read(in);
                in.close();

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    dispose();

                    int frameCount = gifImage.getFrameCount();
                    for (int i = 0; i < frameCount; i++) {
                        BufferedImage frame = gifImage.getFrame(i);
                        int delay = gifImage.getDelay(i);
                        int msDelay = (delay > 0 ? delay * 10 : 100);

                        BufferedImage finalFrame = (frameProcessor != null) ? frameProcessor.apply(frame) : frame;

                        ResourceLocation frameTex = FileUtil.uploadBufferedImageAsTexture(
                                finalFrame, baseName + "_" + i + "_" + System.currentTimeMillis());
                        
                        frameTextures.add(frameTex);
                        frameDelays.add(msDelay);
                    }

                    currentFrame = 0;
                    lastFrameTime = System.currentTimeMillis();
                    loaded = !frameTextures.isEmpty();
                });

            } catch (Exception e) {
                e.printStackTrace(System.err);
                loaded = false;
            }
        }).start();
    }

    public void update() {
        if (!loaded || frameTextures.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= frameDelays.get(currentFrame)) {
            lastFrameTime = now;
            currentFrame = (currentFrame + 1) % frameTextures.size();
        }
    }

    public ResourceLocation getCurrentTexture() {
        if (!loaded || frameTextures.isEmpty()) return null;
        return frameTextures.get(currentFrame);
    }

    public void dispose() {
        for (ResourceLocation tex : frameTextures) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(tex);
        }
        frameTextures.clear();
        frameDelays.clear();
        loaded = false;
    }

}
