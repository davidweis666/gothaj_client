package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.render.OrientCameraEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class ThirdPerson extends Module {

    public final BooleanProperty viewClip = new BooleanProperty("View Clip", true);

    public final NumberProperty distance = new NumberProperty("View Distance", 7.0f, 1.0f, 20.0f, 0.5f);

    public ThirdPerson() {
        super("ThirdPerson", "", ModuleCategory.VISUAL);
        registerProperties(viewClip, distance);
    }

    @Listen
    public void onOrientCamera(OrientCameraEvent event) {
        event.setDistance(distance.getProperty());
        event.setCancelled(viewClip.getProperty());
    }
}
