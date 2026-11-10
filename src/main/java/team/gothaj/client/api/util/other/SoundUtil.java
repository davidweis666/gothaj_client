package team.gothaj.client.api.util.other;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import static team.gothaj.client.api.util.Utils.mc;

public class SoundUtil {

	public static void play(ResourceLocation rsl, float volume) {
		mc.getSoundHandler().playSound(PositionedSoundRecord.create(rsl, 1f, volume));
	}

}
