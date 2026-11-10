package team.gothaj.client.api.util.music.client.volume;

import javazoom.jl.player.AudioDevice;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javax.sound.sampled.*;

public class MusicVolume implements AudioDevice {
    private SourceDataLine line;
    private AudioFormat audioFormat;
    private FloatControl gainControl;
    private float currentVolume = 0.7f; // default volume 70%

    @Override
    public void open(Decoder decoder) throws JavaLayerException {
        try {
            // Standard audio format for MP3 (44.1kHz, 16-bit, stereo)
            audioFormat = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);

            // Get volume control if available
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(currentVolume); // Set default volume
            }

            line.start();
        } catch (Exception e) {
            throw new JavaLayerException("Cannot open audio device", e);
        }
    }

    @Override
    public boolean isOpen() {
        return line.isOpen();
    }

    @Override
    public void write(short[] samples, int offs, int len) throws JavaLayerException {
        if (line != null) {
            short[] volumeAdjustedSamples = new short[len];
            for (int i = 0; i < len; i++) {
                volumeAdjustedSamples[i] = (short)(samples[i + offs] * currentVolume);
            }

            // Convert to bytes for SourceDataLine
            byte[] buffer = new byte[len * 2]; // 2 bytes per sample (16-bit)
            for (int i = 0; i < len; i++) {
                buffer[i * 2] = (byte)(volumeAdjustedSamples[i] & 0xFF);
                buffer[i * 2 + 1] = (byte)((volumeAdjustedSamples[i] >> 8) & 0xFF);
            }

            line.write(buffer, 0, buffer.length);
        }
    }

    @Override
    public void flush() {
        if (line != null) line.drain();
    }

    @Override
    public void close() {
        if (line != null) {
            line.close();
            line = null;
        }
    }

    @Override
    public int getPosition() {
        return line != null ? (int)line.getLongFramePosition() : 0;
    }

    /**
     * Set volume level (0.0 to 1.0)
     * @param volume Volume level from 0.0 (mute) to 1.0 (max)
     */
    public void setVolume(float volume) {
        currentVolume = Math.max(0.0f, Math.min(1.0f, volume));

        // Try hardware gain control first
        if (gainControl != null) {
            float gain;
            if (currentVolume == 0.0f) {
                gain = gainControl.getMinimum(); // Mute
            } else {
                // Convert linear volume to logarithmic decibel scale
                gain = (float)(Math.log10(currentVolume) * 20.0);
                // Clamp to valid range
                gain = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), gain));
            }
            gainControl.setValue(gain);
        }
    }

    /**
     * Get current volume level
     * @return Current volume (0.0 to 1.0)
     */
    public float getVolume() {
        return currentVolume;
    }

    /**
     * Mute audio
     */
    public void mute() {
        setVolume(0.0f);
    }

    /**
     * Unmute to default volume
     */
    public void unmute() {
        setVolume(0.5f);
    }

    /**
     * Increase volume by 10%
     */
    public void volumeUp() {
        setVolume(Math.min(1.0f, currentVolume + 0.1f));
    }

    /**
     * Decrease volume by 10%
     */
    public void volumeDown() {
        setVolume(Math.max(0.0f, currentVolume - 0.1f));
    }
}
