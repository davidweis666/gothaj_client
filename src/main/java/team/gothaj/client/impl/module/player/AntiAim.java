package team.gothaj.client.impl.module.player;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.world.Breaker;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

public class AntiAim extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", "Jitter", "Custom", "Spin", "Backwards", "Jitter");

    public final NumberProperty speed = new NumberProperty("Speed", 15, 1, 100, 1).setVisible(() -> mode.is("Spin") || mode.is("Jitter"));

    public final NumberProperty yaw = new NumberProperty("Yaw", 0, 0, 180, 1).setVisible(() -> mode.is("Custom"));
    public final NumberProperty pitch = new NumberProperty("Pitch", 90, -90, 90, 1);

    float customYaw = 0;
    float customPitch = 0;
    int jitterTicks = 0;

    public AntiAim() {
        super("AntiAim", "Alters your player rotation to confuse opponents", ModuleCategory.PLAYER);
        registerProperties(mode, speed, yaw, pitch);
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        switch (mode.getProperty()) {
            case "Spin" -> {
                customYaw += speed.getProperty();
                if (customYaw >= 360) customYaw = 0;
                customPitch = pitch.getProperty();
            }

            case "Backwards" -> {
                customYaw = mc.thePlayer.rotationYaw - 180;
                customPitch = pitch.getProperty();
            }

            case "Jitter" -> {
                jitterTicks++;
                if (jitterTicks >= 100 - speed.getProperty()) {
                    jitterTicks = 0;
                    customYaw = customYaw == 90 ? -90 : 90;
                }
                customPitch = pitch.getProperty();
            }

            case "Custom" -> {
                customYaw = mc.thePlayer.rotationYaw + yaw.getProperty();
                customPitch = pitch.getProperty();
            }
        }

        RotationComponent.setRotations(customYaw, customPitch, 180, 180, true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {

        if (mc.thePlayer == null) return;

        if (!getCastedModule(KillAura.class).isToggled() || !getCastedModule(Scaffold.class).isToggled() || (!getCastedModule(Breaker.class).isToggled() || (getCastedModule(Breaker.class).isToggled() && Breaker.targetBlockPos == null))) {
            RotationComponent.setRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, MathUtil.random(15, 45), MathUtil.random(15, 45), true);
        }

        super.onDisable();
    }
}
