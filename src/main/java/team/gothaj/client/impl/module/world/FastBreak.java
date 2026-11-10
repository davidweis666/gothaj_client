package team.gothaj.client.impl.module.world;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.impl.event.player.block.DamageBlockEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class FastBreak extends Module {

    private final NumberProperty damage = new NumberProperty("Damage", 1.0f, 0.0f, 1.0f, 0.01f);

    private final NumberProperty delay = new NumberProperty("Break Delay", 5, 0, 5, 1);

    public final BooleanProperty onlyWithTool = new BooleanProperty("Only With Tool", true);
    public final BooleanProperty onlySneak = new BooleanProperty("Only Sneak", true);

    public FastBreak() {
        super("FastBreak", "", ModuleCategory.WORLD);
        registerProperties(damage, delay, onlyWithTool, onlySneak);
    }

    @Listen
    public void onDamageBlock(DamageBlockEvent event) {
        if (onlySneak.getProperty() && !mc.thePlayer.isSneaking()) return;
        if (onlyWithTool.getProperty() && !InventoryUtil.hasTool()) return;
        event.setDamage(damage.getProperty());
        event.setDelay(delay.getProperty().intValue());
    }
}
