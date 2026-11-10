package team.gothaj.cloud.share.packet;

@FunctionalInterface
public interface PacketHandler {
    void handle(Packet packet, PacketResponder responder) throws Exception;
}
