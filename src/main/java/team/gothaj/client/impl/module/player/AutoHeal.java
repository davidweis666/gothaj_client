package team.gothaj.client.impl.module.player;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.module.ModuleManager;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Action;
import team.gothaj.client.api.util.ActionManager;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

public class AutoHeal extends Module {

    public final NumberProperty minHealth = new NumberProperty("Min Health", 5f, 1f, 20f, 0.5f);

    public AutoHeal() {
        super("AutoHeal", "Automatically use healing items", ModuleCategory.PLAYER);
        registerProperties(minHealth);
    }

    private int tick = 0;
    private int lastSlot = -1;

    @Override
    public void onEnable() {
        tick = 0;
        lastSlot = mc.thePlayer == null ? 0 : mc.thePlayer.inventory.currentItem;
        super.onEnable();
    }

    @Listen
    public void onUpdate(TickEvent event) {
        if (mc.thePlayer == null) return;

        tick++;
        if (tick >= 0) tick = 0;
        KillAura ka = getCastedModule(KillAura.class);
        if ((ka != null && ka.isAttacking) || KillAura.target != null) return;
        if (mc.currentScreen != null) return;

        lastSlot = mc.thePlayer.inventory.currentItem;
        if (mc.thePlayer.getHealth() <= minHealth.getProperty() && tick == 0) {
            for (int i = 0; i < 9; i++) {
                var item = mc.thePlayer.inventory.mainInventory[i];
                if (item == null || !(item.getItem() instanceof ItemSoup)) continue;

                mc.thePlayer.inventory.currentItem = i;
                mc.playerController.syncCurrentPlayItem();
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());

                tick = -3;
                GothajClient.getInstance().getActionManager().queue(
                        new Action(() -> {
                            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(
                                    C07PacketPlayerDigging.Action.DROP_ALL_ITEMS,
                                    BlockPos.ORIGIN,
                                    EnumFacing.DOWN
                            ));
                            mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem] = null;
                            mc.thePlayer.inventory.currentItem = lastSlot;
                            mc.playerController.syncCurrentPlayItem();
                        }, 1)
                );
                return;
            }
        }
    }

}
