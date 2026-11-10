package team.gothaj.client.impl.module.client;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.other.FileUtil;
import team.gothaj.client.api.util.other.SoundUtil;

public class ToggleSounds extends Module {

    private final NumberProperty toggleVolume = new NumberProperty("Volume", 0.82f, 0.0f, 1.0f, 0.01f);

    public ToggleSounds() {
        super("ToggleSounds", "Plays a sound when toggling module", ModuleCategory.CLIENT);
        registerProperties(toggleVolume);
    }


    public void playSound(boolean toggle) {
        ResourceLocation rs = new ResourceLocation("gothaj",  (toggle ? "enable" : "disable"));
        SoundUtil.play(rs, toggleVolume.getProperty());
    }
}
