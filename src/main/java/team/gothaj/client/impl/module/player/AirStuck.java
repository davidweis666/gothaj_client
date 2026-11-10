package team.gothaj.client.impl.module.player;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.player.NormalUpdateEvent;
import team.gothaj.eventbus.Listen;

public class AirStuck extends Module {

    public AirStuck() {
        super("AirStuck", "Hang in the air", ModuleCategory.MOVEMENT);
    }


    @Listen
    public void onNormalUpdate(NormalUpdateEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
