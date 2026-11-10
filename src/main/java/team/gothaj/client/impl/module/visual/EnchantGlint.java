package team.gothaj.client.impl.module.visual;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;

/**
 * @author vajzii
 * @since 2025
 */
public class EnchantGlint extends Module {

    public final BooleanProperty itemGlint = new BooleanProperty("Item Glint", true);

    public final BooleanProperty armorGlint = new BooleanProperty("Armor Glint", true);

    public EnchantGlint() {
        super("EnchantGlint", "Modify your glint color", ModuleCategory.VISUAL);
        registerProperties(itemGlint, armorGlint);
    }
}
