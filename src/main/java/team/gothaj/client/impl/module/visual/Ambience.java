package team.gothaj.client.impl.module.visual;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.eventbus.Listen;

import java.awt.*;

public class Ambience extends Module {


    public final BooleanProperty fullBright  = new BooleanProperty("FullBright", true);

    public final ModeProperty fullBrightMode  = new ModeProperty("FB Mode", "Potion",  "Potion", "Gamma").setVisible(fullBright::getProperty);

    public final ModeProperty worldTime = new ModeProperty("World Time", "Night", "Day", "Night", "Midnight", "Morning");

    public final BooleanProperty customWorld = new BooleanProperty("World Color", false);

    public final BooleanProperty customFog = new BooleanProperty("Custom Fog", false).addChangeListener((property, oldValue, newValue) -> {;
        if (newValue) {
            mc.gameSettings.ofFogType = 2;
        } else {
            mc.gameSettings.ofFogType = 0;
        }
        return newValue;
    });

    public final BooleanProperty customSky = new BooleanProperty("Custom Sky", true);

    public final BooleanProperty matchHud = new BooleanProperty("Match HUD", true).setVisible(customSky::getProperty);

    public final ColorProperty worldColor = new ColorProperty("Custom World Color", Color.magenta).setVisible(customWorld::getProperty);

    public final NumberProperty fogDistance = new NumberProperty("Fog Distance", 0.8f, -1f, 0.95f, 0.05f).setVisible(customFog::getProperty);

    public final ColorProperty fogColor = new ColorProperty("Fog Color", Color.magenta).setVisible(customFog::getProperty);

    public final BooleanProperty gradientSky = new BooleanProperty("Gradient Sky", true).setVisible(customSky::getProperty);

    public final ColorProperty skyPrimaryColor = new ColorProperty("Sky Primary Color", Color.magenta).setVisible(() -> !matchHud.getProperty() && customSky.getProperty());

    public final ColorProperty secondarySkyColor = new ColorProperty("Sky Secondary Color", Color.pink).setVisible(() -> !matchHud.getProperty() && customSky.getProperty() && gradientSky.getProperty());


    private float oldBrightness;


    public Ambience() {
        super("Ambience", "Changes your world colors", ModuleCategory.VISUAL);
        registerProperties(fullBright, fullBrightMode, worldTime, customWorld, worldColor, customFog, fogDistance, fogColor, customSky, gradientSky, matchHud, skyPrimaryColor, secondarySkyColor);
    }

    @Override
    public String getSuffix() {
        return worldTime.getProperty();
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        if (fullBright.getProperty()) {
            switch (fullBrightMode.getProperty()) {
                case "Potion" -> {
                    mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.getId(), 999999999));
                }
                case "Gamma" -> {
                    if (mc.thePlayer.isPotionActive(Potion.nightVision))
                        mc.thePlayer.removePotionEffect(Potion.nightVision.getId());

                    mc.gameSettings.gammaSetting = 10000.0f;
                }
            }
        } else {
            if (mc.thePlayer.isPotionActive(Potion.nightVision))
                mc.thePlayer.removePotionEffect(Potion.nightVision.getId());

            mc.gameSettings.gammaSetting = oldBrightness;
        }
    }

    public int getWorldTime() {
        switch (worldTime.getProperty()) {
            case "Day" -> {
                return 1000;
            }
            case "Night" -> {
                return 15000;
            }
            case "Midnight" -> {
                return 18000;
            }
            case "Morning" -> {
                return 23000;
            }
        }
        return 0;
    }

    @Override
    public void onEnable() {
        oldBrightness = mc.gameSettings.gammaSetting;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.isPotionActive(Potion.nightVision))
            mc.thePlayer.removePotionEffect(Potion.nightVision.getId());

        mc.gameSettings.gammaSetting = oldBrightness;
        super.onDisable();
    }
}
