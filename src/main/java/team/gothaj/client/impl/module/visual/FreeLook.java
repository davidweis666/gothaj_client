package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.impl.event.input.InputEvent;
import team.gothaj.client.impl.event.player.RotationEvent;
import team.gothaj.client.impl.event.render.PreOrientCameraEvent;
import team.gothaj.eventbus.Listen;

public class FreeLook extends Module {

    private int lastPerspective = 0;
    private float yaw = 0.0f;
    private float prevYaw = 0.0f;
    private float pitch = 0.0f;
    private float prevPitch = 0.0f;

    private final BooleanProperty resetPerspective = new BooleanProperty("Reset Perspective", true);

    public FreeLook() {
        super("FreeLook", "Allows you look around 360* freely", ModuleCategory.VISUAL);
        registerProperties(resetPerspective);
    }

    @Listen
    public void onOrientCamera(PreOrientCameraEvent event) {
        prevYaw = yaw;
        prevPitch = pitch;
        event.setYaw(yaw);
        event.setPrevYaw(prevYaw);
        event.setPitch(pitch);
        event.setPrevPitch(prevPitch);
    }

    @Listen
    public void onKey(InputEvent event) {
        mc.gameSettings.thirdPersonView = 1;
        mc.gameSettings.keyBindTogglePerspective.unpressKey();
    }

    @Listen
    public void onMouse(RotationEvent event) {
        event.setCancelled(true);
        mc.mouseHelper.mouseXYChange();

        var sensitivity = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        var speed = sensitivity * sensitivity * sensitivity * 8.0f;

        var deltaX = mc.mouseHelper.deltaX * speed;
        var deltaY = mc.mouseHelper.deltaY * speed;

        yaw += deltaX * 0.15f;
        pitch -= deltaY * 0.15f;

        if (pitch > 90f) pitch = 90f;
        if (pitch < -90f) pitch = -90f;

        mc.renderGlobal.setDisplayListEntitiesDirty();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.thePlayer == null) return;
        lastPerspective = mc.gameSettings.thirdPersonView;
        yaw = mc.thePlayer.rotationYaw;
        pitch = mc.thePlayer.rotationPitch;
        mc.gameSettings.thirdPersonView = 1;
        mc.gameSettings.keyBindTogglePerspective.unpressKey();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (resetPerspective.getProperty())
            mc.gameSettings.thirdPersonView = lastPerspective;
    }
}
