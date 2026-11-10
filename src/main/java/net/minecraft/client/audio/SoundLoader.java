package net.minecraft.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

public class SoundLoader {

	private static final Map<ResourceLocation, Integer> BUFFER_CACHE = new HashMap<>();

	public static int getOpenALBuffer(ResourceLocation location) {
		Integer cached = BUFFER_CACHE.get(location);
		if (cached != null) {
			return cached;
		}

		int buffer = loadSound(location);
		BUFFER_CACHE.put(location, buffer);
		return buffer;
	}

	private static int loadSound(ResourceLocation location) {
		try (MemoryStack stack = MemoryStack.stackPush()) {

			InputStream stream = Minecraft.getMinecraft()
					.getResourceManager()
					.getResource(location)
					.getInputStream();

			byte[] bytes = stream.readAllBytes();

			ByteBuffer vorbisData = MemoryUtil.memAlloc(bytes.length);
			vorbisData.put(bytes).flip();

			IntBuffer channels = stack.mallocInt(1);
			IntBuffer sampleRate = stack.mallocInt(1);

			ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(vorbisData, channels, sampleRate);
			MemoryUtil.memFree(vorbisData);

			if (pcm == null) {
				throw new RuntimeException("Failed to decode sound: " + location);
			}

			int format = channels.get(0) == 1
					? AL10.AL_FORMAT_MONO16
					: AL10.AL_FORMAT_STEREO16;

			int buffer = AL10.alGenBuffers();
			AL10.alBufferData(buffer, format, pcm, sampleRate.get(0));

			return buffer;

		} catch (Exception e) {
			throw new RuntimeException("Failed to load sound: " + location, e);
		}
	}

	public static void cleanup() {
		for (int buffer : BUFFER_CACHE.values()) {
			AL10.alDeleteBuffers(buffer);
		}
		BUFFER_CACHE.clear();
	}
}