package team.gothaj.client.impl.module.misc;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.module.movement.NoSlow;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

public class PacketModifier extends Module {

    private final BooleanProperty c0cOnTick = new BooleanProperty("C0COnTick", false);
    private final BooleanProperty noSprint = new BooleanProperty("No Sprint", false);

    public PacketModifier() {
        super("PacketModifier", "modifies packets", ModuleCategory.MISC);
        registerProperties(c0cOnTick, noSprint);
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (c0cOnTick.getProperty()) {
            if (mc.thePlayer != null) {
                PacketUtil.send(new C0CPacketInput(mc.thePlayer.movementInput.moveStrafe,
                        mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.jump,
                        mc.thePlayer.movementInput.sneak));

            }
        }
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (noSprint.getProperty()) {
            if (event.getDirection() == PacketEvent.Direction.SEND) {
                if (event.getPacket() instanceof C0BPacketEntityAction p) {
                    if (p.getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING || p.getAction() == C0BPacketEntityAction.Action.START_SPRINTING) {
                        event.setCancelled(true);
                    }
                }
            }
        }
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
