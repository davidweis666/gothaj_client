package team.gothaj.client.impl.module.ghost;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.NormalUpdateEvent;
import team.gothaj.eventbus.Listen;

public class BlockHit extends Module {

    private final NumberProperty chance = new NumberProperty("Chance", 20, 0, 100, 1);

    private final BooleanProperty predict = new BooleanProperty("Predict", true);

    private boolean block = false;
    private int blockTicks = 0;

    public BlockHit() {
        super("BlockHit", "Blocks after hits or when incoming knockback is predicted", ModuleCategory.GHOST);
        registerProperties(chance, predict);
    }

    @Override
    public void onEnable() {
        block = false;
        blockTicks = 0;
        super.onEnable();
    }

    @Listen
    public void onClick(MouseClickEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (!holdingSword()) return;
        if (mc.thePlayer.isUsingItem()) return;

        if (!Mouse.isButtonDown(0)) {
            block = false;
            blockTicks = 0;
            return;
        }

        if (block) {
            block = false;
            blockTicks = 0;
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
            return;
        }

        if (Math.random() <= chance.getProperty() / 100.0) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
        }
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
        if (!(event.getPacket() instanceof S12PacketEntityVelocity packet)) return;

        if (packet.getEntityID() == mc.thePlayer.getEntityId() && predict.getProperty() && holdingSword()) {
            block = true;
            blockTicks = 0;
        }
    }

    @Listen
    public void onUpdate(NormalUpdateEvent event) {
        if (block && ++blockTicks > 10) {
            block = false;
            blockTicks = 0;
        }
    }

    private boolean holdingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }
}
