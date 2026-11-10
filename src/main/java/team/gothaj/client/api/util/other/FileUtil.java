package team.gothaj.client.api.util.other;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.main.GothajClient;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static team.gothaj.client.api.util.Utils.mc;

public class FileUtil {

    public static String readInputStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input stream", e);
        }
    }

    public static ResourceLocation uploadBufferedImageAsTexture(BufferedImage img, String name) {
        DynamicTexture dynTex = new DynamicTexture(img);
        ResourceLocation resLoc = new ResourceLocation("gothaj_gif", name);
        Minecraft.getMinecraft().getTextureManager().loadTexture(resLoc, dynTex);
        return resLoc;
    }

    public static String hashBuildDate(String buildDate) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(buildDate.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * @deprecated use SoundUtil dont use this shit
     */
    @Deprecated(forRemoval = true)
    public static void playSound(ResourceLocation location, float volume) {
        /*
        GothajClient.scheduledExecutorService.execute(() -> {
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(mc.getResourceManager().getResource(location).getInputStream());
                AudioInputStream rawStream = AudioSystem.getAudioInputStream(bufferedInputStream);

                AudioFormat baseFormat = rawStream.getFormat();
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED ? 44100 : baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels() == AudioSystem.NOT_SPECIFIED ? 2 : baseFormat.getChannels(),
                        baseFormat.getChannels() == AudioSystem.NOT_SPECIFIED ? 4 : baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED ? 44100 : baseFormat.getSampleRate(),
                        false);

                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, rawStream);

                DataLine.Info info = new DataLine.Info(Clip.class, targetFormat);
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(convertedStream);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float range = gainControl.getMaximum() - gainControl.getMinimum();
                    float gain = (range * volume) + gainControl.getMinimum();
                    gainControl.setValue(gain);
                }

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.drain();
                        clip.close();
                    }
                });

                clip.start();

            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace(System.err);
            }
        });

         */
    }

    public static File openFileChooser(File initFile, FileChooser.ExtensionFilter... filters) {
        AtomicReference<File> result = new AtomicReference<>();

        runAndWait(() -> {
            FileChooser fc = new FileChooser();

            Optional.ofNullable(initFile).ifPresent(file -> {
                Optional.ofNullable(file.getParentFile())
                        .filter(File::isDirectory)
                        .ifPresent(fc::setInitialDirectory);
                fc.setInitialFileName(file.getName());
            });

            if (filters != null && filters.length > 0) {
                fc.getExtensionFilters().addAll(filters);
            }

            result.set(fc.showOpenDialog(null));
        });

        return result.get();
    }

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void runAndWait(Runnable action) {
        if (action == null)
            throw new NullPointerException("action");

        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        if (initialized.compareAndSet(false, true))
            new JFXPanel();

        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }



}
