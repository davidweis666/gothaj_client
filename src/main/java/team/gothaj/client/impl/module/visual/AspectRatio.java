package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;

/**
 * @author vajzii
 * @since 2025
 */
public class AspectRatio extends Module {

    public static final NumberProperty ratio = new NumberProperty("Ratio", (float) mc.displayWidth / mc.displayHeight, 0.1f, 20.0f, 0.1f);

    public AspectRatio() {
        super("AspectRatio", "", ModuleCategory.VISUAL);
        registerProperties(ratio);
    }



}
