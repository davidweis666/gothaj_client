package team.gothaj.client.impl.module.combat;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

public class AutoBlock extends Module {

    private final BooleanProperty onAttack = new BooleanProperty("On attack", true);
    private final BooleanProperty onUse = new BooleanProperty("On use", false);
    private final BooleanProperty attack = new BooleanProperty("Attack", true);
    private final NumberProperty tickRange = new NumberProperty("Tick Range", 3, 0, 10, 1);

    public AutoBlock() {
        super("AutoBlock", "Automatically block", ModuleCategory.COMBAT);
        registerProperties(onAttack, onUse, attack, tickRange);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class).stopBlinkingOutbound();
    }


    public boolean shouldBlink() {
        return mc.thePlayer.ticksExisted % tickRange.getProperty().intValue() * 2 >= mc.thePlayer.ticksExisted % tickRange.getProperty().intValue();
    }


    @Listen
    public void onInput(MouseClickEvent event) {
        if (onAttack.getProperty() && !GameSettings.isKeyDown(mc.gameSettings.keyBindAttack)) return;
        if (onUse.getProperty() && !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) return;
        if (mc.thePlayer.hurtTime == 0) {
            mc.gameSettings.keyBindUseItem.pressed = mc.thePlayer.ticksExisted % 4 == 0;
        } else {
            mc.gameSettings.keyBindUseItem.pressed = false;
        }
        if (mc.gameSettings.keyBindUseItem.isPressed() && mc.thePlayer.hurtTime == 0) {
            if (shouldBlink()) {
                GothajClient.getInstance().getComponentManager().getAndCast(
                        BlinkComponent.class
                ).stopBlinkingOutbound();
            }
        } else {
            if (shouldBlink()) {
                GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class).startBlinkingOutbound();
            }
        }
        if (mc.thePlayer.ticksExisted % 3 == 2) {
            if (shouldBlink()) {
                GothajClient.getInstance().getComponentManager().getAndCast(
                        BlinkComponent.class
                ).stopBlinkingOutbound();
            }
            if (attack.getProperty()) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            }
            if (mc.thePlayer.hurtTime == 0) {
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                mc.gameSettings.keyBindUseItem.pressed = true;
            }
        }

    }

}
