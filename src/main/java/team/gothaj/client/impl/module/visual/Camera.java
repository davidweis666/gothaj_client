package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;

/**
 * @author vajzii
 * @since 2025
 */
public class Camera extends Module {

    public static BooleanProperty yawBody = new BooleanProperty("Rotate Body", true);

    public static BooleanProperty noHurtCam = new BooleanProperty("No Hurt Cam", true);

    public static BooleanProperty betterHurtCam = new BooleanProperty("Better Hurt Cam", true);

    public static BooleanProperty healthIndicator = new BooleanProperty("Health Indicator", false);

    public BooleanProperty noFire = new BooleanProperty("No Fire Overlay", false);

    public BooleanProperty noWater = new BooleanProperty("No Water Overlay", false);


    public Camera() {
        super("Camera", "", ModuleCategory.VISUAL);
        registerProperties(yawBody, noHurtCam, betterHurtCam, healthIndicator, noFire, noWater);
    }

}
