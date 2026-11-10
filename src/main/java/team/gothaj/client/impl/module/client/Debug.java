package team.gothaj.client.impl.module.client;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.rotation.RotationDebugger;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class Debug extends Module {

    public Debug() {
        super("Debug", "", ModuleCategory.CLIENT);
    }

    @Listen
    public void onRender3D(RenderDimensionEvent event) {
        RotationDebugger.render3D();
    }

    @Override
    public void onEnable() {
        if (GothajClient.getInstance().getCurrentPlayer().getRank().getPermissionLevel() <= 10) {
            setToggled(false);
            return;
        }

        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
