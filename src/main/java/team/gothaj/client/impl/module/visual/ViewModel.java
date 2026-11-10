package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;

/**
 * @author vajzii
 * @since 2025
 */
public class ViewModel extends Module {

    public final ModeProperty blockMode = new ModeProperty("Animation", "None", "None", "1.7", "Gothaj", "Glitz", "Classic", "Rotation", "Spin");

    public final NumberProperty
            xPos = new NumberProperty("X Pos", 0, -100, 100, 1),
            yPos = new NumberProperty("Y Pos", 0, -100, 100, 1),
            zPos = new NumberProperty("Z Pos", 0, -100, 100, 1);

    public final NumberProperty itemScale = new NumberProperty("Item Scale", 1.0f, 0.1f, 5.0f, 0.05f);

    public final NumberProperty swingSpeed = new NumberProperty("Swing Speed", 1.0f, 0.1f, 2.0f, 0.1f);

    public final NumberProperty bobbingVelocity = new NumberProperty("Bobbing Velocity", 0.5f, 0.1f, 1.0f, 0.05f);

    public ViewModel() {
        super("ViewModel", "Modifies your item in hand", ModuleCategory.VISUAL);
        registerProperties(blockMode, xPos, yPos, zPos, itemScale, bobbingVelocity, swingSpeed);
    }

    @Override
    public String getSuffix() {
        return blockMode.getProperty();
    }
}
