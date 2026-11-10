package team.gothaj.client.impl.module.misc;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.other.FileUtil;
import team.gothaj.client.api.util.other.SoundUtil;
import team.gothaj.client.impl.event.input.EarlyInputEvent;
import team.gothaj.client.impl.event.input.InputEvent;
import team.gothaj.eventbus.Listen;

import java.util.Random;

public class InputSounds extends Module {

    private final BooleanProperty mouse = new BooleanProperty("Mouse", false);
    private final BooleanProperty keyboard = new BooleanProperty("Keyboard", false);

    private final NumberProperty mouseVolume = new NumberProperty("Mouse Volume", 0.82f, 0.0f, 1.0f, 0.01f);
    private final NumberProperty keyboardVolume = new NumberProperty("Keyboard Volume", 0.82f, 0.0f, 1.0f, 0.01f);

    private final Random random = new Random();

    public InputSounds() {
        super("InputSounds", "Simulates input sounds", ModuleCategory.MISC);
        registerProperties(mouse, mouseVolume, keyboard, keyboardVolume);
    }

    @Listen
    public void onKeyPress(InputEvent event) {
        if (!keyboard.getProperty()) return;
        if (!event.isKey()) return;

        final int code = event.getCode();

        //Tf is key 288
        if (code != 288)
            playKeySound(code);
    }

    private void playKeySound(int code) {
        String keyName = Keyboard.getKeyName(code).toLowerCase();
        ResourceLocation rs = new ResourceLocation("gothaj", "keyboard/" + keyName + ".ogg");

        boolean exists = resourceExists(rs);

        if (!exists) {
            rs = new ResourceLocation("gothaj", "keyboard/x.ogg");
        }

        SoundUtil.play(rs, keyboardVolume.getProperty());
    }

    private boolean resourceExists(ResourceLocation rl) {
        try {
            mc.getResourceManager().getResource(rl);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Listen
    public void onMousePress(EarlyInputEvent event) {
        if (!mouse.getProperty()) return;
        if (!event.isKey()) {
            int button = Mouse.getEventButton();
            boolean pressed = Mouse.getEventButtonState();

            if (button != -1) {
                playMouseSound(button, pressed);
            }
        }
    }

    public void playMouseSound(int button, boolean pressed) {
        if (button == 3) return;
        String[] clickSounds = {"click1.ogg", "click2.ogg"};
        String[] releaseSounds = {"release1.ogg", "release2.ogg"};

        String chosenSound;
        if (pressed) {
            chosenSound = clickSounds[random.nextInt(clickSounds.length)];
        } else {
            chosenSound = releaseSounds[random.nextInt(releaseSounds.length)];
        }

        SoundUtil.play(new ResourceLocation("gothaj", "mouse/" + chosenSound), mouseVolume.getProperty());
    }

}
