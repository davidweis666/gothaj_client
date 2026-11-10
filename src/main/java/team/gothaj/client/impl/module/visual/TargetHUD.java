package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class TargetHUD extends Module {


    public final ModeProperty mode = new ModeProperty("Mode", "Gothaj", "Gothaj", "Novoline", "Exhibition");

    public final BooleanProperty follow = new BooleanProperty("Follow", false);


    public TargetHUD() {
        super("TargetHUD", "Renders current target info", ModuleCategory.VISUAL);
        registerProperties(mode, follow);
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (w.getLabel().equalsIgnoreCase(getName())) {
                w.onRender(event.getScaledResolution());
            }
        });
    }

    @Listen
    public void onShader(ShaderOverlayEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (w.getLabel().equalsIgnoreCase(getName())) {
                w.onShader(event);
            }
        });
    }

}
