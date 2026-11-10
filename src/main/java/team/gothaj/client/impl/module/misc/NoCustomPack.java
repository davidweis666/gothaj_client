package team.gothaj.client.impl.module.misc;

import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class NoCustomPack extends Module {
    public NoCustomPack() {
        super("NoCustomPack", "", ModuleCategory.MISC);

    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
            if (event.getPacket() instanceof S48PacketResourcePackSend packet) {
                String hash = packet.getHash();
                PacketUtil.send(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                PacketUtil.send(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
                event.setCancelled(true);
            }
        }
    }
}
