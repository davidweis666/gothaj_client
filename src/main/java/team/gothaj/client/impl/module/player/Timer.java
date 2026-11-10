package team.gothaj.client.impl.module.player;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;

public class Timer extends Module {

    public final NumberProperty timerSpeed = new NumberProperty("Timer Speed", 1.0f, 0.0f, 5.0f, 0.1f);

    public Timer() {
        super("Timer", "Changes the game timer speed.", ModuleCategory.PLAYER);
        registerProperties(timerSpeed);
    }

     @Override
     public void onEnable() {
        mc.timer.timerSpeed = timerSpeed.getProperty();
        super.onEnable();
     }

     @Override
     public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        super.onDisable();
     }
}
