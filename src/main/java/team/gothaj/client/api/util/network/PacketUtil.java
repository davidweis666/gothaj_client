package team.gothaj.client.api.util.network;

import lombok.AllArgsConstructor;
import lombok.Setter;
import net.minecraft.network.Packet;
import team.gothaj.client.api.util.Utils;

/**
 * @author Kawase
 * @since 30.08.2025
 */
public class PacketUtil implements Utils {
    public static void send(Packet<?> packet) {
        mc.thePlayer.sendQueue.addToSendQueue(packet);
    }

    public static void sendSilent(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().sendPacketSilentEvent(packet);
    }

    public static void receiveNoEvent(final Packet<?> packet) {
        mc.getNetHandler().addToReceiveQueueUnregistered(packet);
    }

     public record DelayedPacket(Packet<?> packet, long delay) {
    }
}
