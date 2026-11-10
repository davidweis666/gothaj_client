package team.gothaj.client.impl.module.movement;

import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

public class AutoSprint extends Module {

    private final BooleanProperty omniSprint = new BooleanProperty("OmniSprint", false);

    public static boolean allowSprint = true;

    public AutoSprint() {
        super("AutoSprint", "Automatically sprints for you", ModuleCategory.MOVEMENT);
        registerProperties(omniSprint);
    }

    @Listen
    public void onLivingUpdate(TickEvent event) {
        boolean canSprint = allowSprint
        		&& mc.thePlayer.movementInput.moveForward >= 0.8F
                && !mc.thePlayer.isCollidedHorizontally
                && (!mc.thePlayer.isUsingItem() || getCastedModule(NoSlow.class).isToggled())
                && !getCastedModule(Scaffold.class).isToggled()
                && (mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying)
                && !InventoryUtil.isInventoryOpen;

        mc.thePlayer.omniSprint = omniSprint.getProperty();
        
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), canSprint);
        if(!canSprint && mc.thePlayer.isSprinting())mc.thePlayer.setSprinting(false);

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
