package team.gothaj.client.impl.module.misc;

import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.NormalUpdateEvent;
import team.gothaj.eventbus.Listen;

public class ResourceSpoof extends Module {

    private String pendingHash;
    private int ticksSinceAccept;
    private static final int LOAD_TICKS = 2;

    public ResourceSpoof() {
        super("ResourceSpoof", "Spoofs resource pack acceptance to prevent kicks on ViaVersion servers.",
                ModuleCategory.MISC);
    }

    @Override
    public void onEnable() {
        pendingHash = null;
        ticksSinceAccept = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        pendingHash = null;
        ticksSinceAccept = 0;
        super.onDisable();
    }

    @Listen
    public void onPacketReceive(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
        if (!(event.getPacket() instanceof S48PacketResourcePackSend packet)) return;

        event.setCancelled(true);
        pendingHash = packet.getHash();
        ticksSinceAccept = 0;

        mc.thePlayer.sendQueue.addToSendQueue(
                new C19PacketResourcePackStatus(packet.getHash(),
                        C19PacketResourcePackStatus.Action.ACCEPTED));
    }

    @Listen
    public void onUpdate(NormalUpdateEvent event) {
        if (pendingHash == null) return;

        if (++ticksSinceAccept >= LOAD_TICKS) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C19PacketResourcePackStatus(pendingHash,
                            C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
            pendingHash = null;
        }
    }
}
