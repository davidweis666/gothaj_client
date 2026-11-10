package net.minecraft.client.audio;

import com.google.common.collect.*;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;

import java.util.*;

public class SoundManager {

    private final SoundHandler sndHandler;
    private final GameSettings options;

    private boolean loaded;
    private int playTime = 0;

    private long device;
    private long context;

    private final Map<String, Integer> sourceMap = new HashMap<>();

    private final Map<String, ISound> playingSounds = HashBiMap.create();
    private final Map<ISound, String> invPlayingSounds = ((BiMap<String, ISound>) playingSounds).inverse();

    private final Map<ISound, SoundPoolEntry> playingSoundPoolEntries = Maps.newHashMap();
    private final Multimap<SoundCategory, String> categorySounds = HashMultimap.create();
    private final List<ITickableSound> tickableSounds = Lists.newArrayList();
    private final Map<ISound, Integer> delayedSounds = Maps.newHashMap();
    private final Map<String, Integer> playingSoundsStopTime = Maps.newHashMap();

    private final Set<String> pausedSources = new HashSet<>();
    private final Set<ISound> soundsToStop = new HashSet<>();

    public SoundManager(SoundHandler handler, GameSettings settings) {
        this.sndHandler = handler;
        this.options = settings;
    }

    public void reloadSoundSystem() {
        unloadSoundSystem();

        SoundLoader.cleanup();

        loadSoundSystem();
    }

    private synchronized void loadSoundSystem() {
        if (loaded) return;

        device = ALC10.alcOpenDevice((java.nio.ByteBuffer) null);

        context = ALC10.alcCreateContext(device, (int[]) null);
        ALC10.alcMakeContextCurrent(context);

        AL.createCapabilities(ALC.createCapabilities(device));

        loaded = true;

        setMasterVolume(options.getSoundLevel(SoundCategory.MASTER));
    }

    public void unloadSoundSystem() {

        if (!loaded) return;

        stopAllSounds();

        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);

        loaded = false;
    }

    public void setMasterVolume(float volume) {
        AL10.alListenerf(AL10.AL_GAIN, volume);
    }

    private float getSoundCategoryVolume(SoundCategory category) {
        return category != null && category != SoundCategory.MASTER
                ? options.getSoundLevel(category)
                : 1.0F;
    }

    public void playDelayedSound(ISound sound, int delay) {
        if (!loaded) return;

        delayedSounds.put(sound, playTime + delay);
    }

    public void pauseAllSounds() {
        for (String id : sourceMap.keySet()) {

            if (id == null) continue;

            int source = sourceMap.get(id);

            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);

            if (state == AL10.AL_PLAYING) {
                AL10.alSourcePause(source);
                pausedSources.add(id);
            }
        }
    }

    public void resumeAllSounds() {
        Iterator<String> iter = pausedSources.iterator();

        while (iter.hasNext()) {

            String id = iter.next();

            if (id == null) continue;

            Integer source = sourceMap.get(id);

            if (source != null) {
                AL10.alSourcePlay(source);
            }

            iter.remove();
        }
    }

    public void setSoundCategoryVolume(SoundCategory category, float volume) {
        if (!loaded) return;

        if (category == SoundCategory.MASTER) {
            setMasterVolume(volume);
        } else {
            for (String id : categorySounds.get(category)) {
                ISound sound = playingSounds.get(id);

                if (sound != null) {
                    SoundPoolEntry entry = playingSoundPoolEntries.get(sound);
                    float f = getNormalizedVolume(sound, entry, category);

                    if (f <= 0) {
                        soundsToStop.add(sound);
                    } else {

                        Integer source = sourceMap.get(id);

                        if (source != null) {
                            AL10.alSourcef(source, AL10.AL_GAIN, f);
                        }
                    }
                }
            }
        }
    }

    public boolean isSoundPlaying(ISound sound) {
        if (!loaded) return false;

        String id = invPlayingSounds.get(sound);

        if (id == null) return false;

        if (sourceMap.containsKey(id)) {
            int source = sourceMap.get(id);

            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);

            if (state == AL10.AL_PLAYING) {
                return true;
            }
        }

        return delayedSounds.containsKey(sound) && delayedSounds.get(sound) > playTime;
    }

    public void playSound(ISound sound) {
        if (!loaded) return;

        SoundEventAccessorComposite accessor = sndHandler.getSound(sound.getSoundLocation());

        if (accessor == null) return;

        SoundPoolEntry entry = accessor.cloneEntry();

        float volume = getNormalizedVolume(sound, entry, accessor.getSoundCategory());

        if (volume <= 0) return;

        int source = AL10.alGenSources();
        int buffer = SoundLoader.getOpenALBuffer(entry.getSoundPoolEntryLocation());

        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcef(source, AL10.AL_GAIN, volume);
        AL10.alSourcef(source,
                AL10.AL_PITCH,
                getNormalizedPitch(sound, entry));
        AL10.alSource3f(source,
                AL10.AL_POSITION,
                sound.getXPosF(),
                sound.getYPosF(),
                sound.getZPosF());
        AL10.alSourcePlay(source);
        String id = MathHelper.getRandomUuid(ThreadLocalRandom.current()).toString();
        sourceMap.put(id, source);
        playingSounds.put(id, sound);
        playingSoundPoolEntries.put(sound, entry);
        playingSoundsStopTime.put(id, playTime + 20);

        SoundCategory category = accessor.getSoundCategory();

        if (category != SoundCategory.MASTER) {
            categorySounds.put(category, id);
        }

        if (sound instanceof ITickableSound) {
            tickableSounds.add((ITickableSound) sound);
        }
    }

    public void updateAllSounds() {
        playTime++;

        for (ITickableSound sound : new ArrayList<>(tickableSounds)) {
            sound.update();

            if (sound.isDonePlaying()) {
                soundsToStop.add(sound);
                continue;
            }

            String id = invPlayingSounds.get(sound);
            if (id == null) continue;
            Integer sourceObj = sourceMap.get(id);
            if (sourceObj == null) continue;
            int source = sourceObj;
            SoundEventAccessorComposite accessor = sndHandler.getSound(sound.getSoundLocation());
            if (accessor == null) continue;
            SoundPoolEntry poolEntry = playingSoundPoolEntries.get(sound);
            if (poolEntry == null) continue;
            AL10.alSourcef(source,
                    AL10.AL_GAIN,
                    getNormalizedVolume(
                            sound,
                            poolEntry,
                            accessor.getSoundCategory()
                    ));
            AL10.alSourcef(source,
                    AL10.AL_PITCH,
                    getNormalizedPitch(sound, poolEntry));
            AL10.alSource3f(source,
                    AL10.AL_POSITION,
                    sound.getXPosF(),
                    sound.getYPosF(),
                    sound.getZPosF());
        }

        for (Map.Entry<String, ISound> entry : new ArrayList<>(playingSounds.entrySet())) {
            String id = entry.getKey();
            ISound sound = entry.getValue();

            if (pausedSources.contains(id)) {
                continue;
            }

            Integer sourceObj = sourceMap.get(id);

            if (sourceObj == null) {
                soundsToStop.add(sound);
                continue;
            }

            int source = sourceObj;
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING) {
                int stopTime = playingSoundsStopTime.getOrDefault(id, playTime);

                if (stopTime <= playTime) {

                    if (sound.canRepeat() && sound.getRepeatDelay() > 0) {

                        delayedSounds.put(
                                sound,
                                playTime + sound.getRepeatDelay()
                        );
                    }

                    soundsToStop.add(sound);
                }
            }
        }

        Iterator<Map.Entry<ISound, Integer>> delayIterator =
                delayedSounds.entrySet().iterator();

        while (delayIterator.hasNext()) {
            Map.Entry<ISound, Integer> entry = delayIterator.next();

            if (playTime >= entry.getValue()) {
                playSound(entry.getKey());

                delayIterator.remove();
            }
        }

        if (!soundsToStop.isEmpty()) {
            for (ISound sound : new ArrayList<>(soundsToStop)) {
                stopSoundInternal(sound);
            }

            soundsToStop.clear();
        }
    }

    public void stopSound(ISound sound) {
        stopSoundInternal(sound);
    }

    private void stopSoundInternal(ISound sound) {
        String id = invPlayingSounds.get(sound);
        if (id == null) return;

        Integer sourceObj = sourceMap.get(id);

        if (sourceObj != null) {
            AL10.alSourceStop(sourceObj);
            AL10.alDeleteSources(sourceObj);
        }

        playingSounds.remove(id);
        sourceMap.remove(id);
        playingSoundPoolEntries.remove(sound);
        playingSoundsStopTime.remove(id);
        delayedSounds.remove(sound);
        pausedSources.remove(id);

        if (sound instanceof ITickableSound) {
            tickableSounds.remove(sound);
        }

        for (SoundCategory category : SoundCategory.values()) {
            categorySounds.remove(category, id);
        }
    }

    public void stopAllSounds() {
        for (int source : sourceMap.values()) {
            AL10.alSourceStop(source);
            AL10.alDeleteSources(source);
        }

        sourceMap.clear();
        playingSounds.clear();
        delayedSounds.clear();
        tickableSounds.clear();
        categorySounds.clear();
        playingSoundPoolEntries.clear();
        playingSoundsStopTime.clear();
        pausedSources.clear();
        soundsToStop.clear();
    }

    public void setListener(EntityPlayer player, float partialTicks) {
        if (!loaded || player == null) return;

        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;

        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks + player.getEyeHeight();

        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        float[] orientation = getFloats(player, partialTicks);

        AL10.alListener3f(
                AL10.AL_POSITION,
                (float) x,
                (float) y,
                (float) z
        );

        AL10.alListener3f(
                AL10.AL_VELOCITY,
                (float) player.motionX,
                (float) player.motionY,
                (float) player.motionZ
        );

        AL10.alListenerfv(
                AL10.AL_ORIENTATION,
                orientation
        );
    }

    @NotNull
    private static float[] getFloats(EntityPlayer player, float partialTicks) {
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float yawRad = (float) Math.toRadians(-yaw - 180.0F);
        float pitchRad = (float) Math.toRadians(-pitch);
        float lookX = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);
        float lookY = MathHelper.sin(pitchRad);
        float lookZ = MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
	    return new float[] {
	            lookX,
	            lookY,
	            lookZ,
	            0.0F,
	            1.0F,
	            0.0F
	    };
    }

    private float getNormalizedPitch(ISound sound, SoundPoolEntry entry) {
        return (float) MathHelper.clamp(
                sound.getPitch() * entry.getPitch(),
                0.5D,
                2.0D
        );
    }

    private float getNormalizedVolume(ISound sound, SoundPoolEntry entry, SoundCategory category) {
        return (float) MathHelper.clamp(
                sound.getVolume() * entry.getVolume(),
                0.0D,
                1.0D
        ) * getSoundCategoryVolume(category);
    }
}