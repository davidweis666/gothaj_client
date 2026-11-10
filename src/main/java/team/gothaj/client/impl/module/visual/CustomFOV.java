package team.gothaj.client.impl.module.visual;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.value.ValueChangeEvent;
import team.gothaj.eventbus.Listen;

public class CustomFOV extends Module {

    public final NumberProperty fov = new NumberProperty("FOV", 90, 30, 150, 1);

    public static NumberProperty speedMultiplier = new NumberProperty("Speed Multiplier", 1.0f, 0.1f, 1.5f, 0.1f);

    private float previousFov;

    public CustomFOV() {
        super("CustomFOV", "Allows you to modify your fov into custom value", ModuleCategory.VISUAL);
        registerProperties(fov, speedMultiplier);
    }

    @Listen
    public void onValueChange(ValueChangeEvent event) {
        if (event.getProperty() != fov) return;
        mc.gameSettings.fovSetting = fov.getProperty();
    }

    @Override
    public void onEnable() {
        previousFov = mc.gameSettings.fovSetting;

        if (mc.gameSettings.fovSetting != fov.getProperty())
            mc.gameSettings.fovSetting = fov.getProperty();


        IAttributeInstance iattributeinstance = mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        speedMultiplier.setProperty((float) ((int) (iattributeinstance.getAttributeValue() / (double)mc.thePlayer.capabilities.getWalkSpeed() + 1.0D) / 2.0D));
        super.onEnable();
    }


    @Override
    public void onDisable() {
        if (previousFov == 0) previousFov = 90;
        mc.gameSettings.fovSetting = previousFov;
        super.onDisable();
    }
}
