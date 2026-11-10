package team.gothaj.client.impl.module.world;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class FastPlace extends Module {

    public FastPlace() {
        super("FastPlace", "", ModuleCategory.WORLD);
    }


    @Listen
    public void onMouse(MouseClickEvent event) {
        mc.rightClickDelayTimer = 0;
    }
}
