package team.gothaj.client.impl.module.misc;

import net.minecraft.network.play.client.C01PacketChatMessage;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

public class ChatBypass extends Module {

    public ChatBypass() {
        super("ChatBypass", "Bypasses chat filters by putting dots between characters", ModuleCategory.MISC);
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.SEND) return;
        if (!(event.getPacket() instanceof C01PacketChatMessage packetChat)) return;

        String message = packetChat.getMessage();
        if (message.isEmpty() || message.startsWith("/")) return;

        String bypassed = addDots(message);
        if (bypassed.length() > 100) return;

        packetChat.setMessage(bypassed);
    }

    private String addDots(String message) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            builder.append(c);

            if (c != ' ' && i < message.length() - 1) {
                builder.append('.');
            }
        }

        return builder.toString();
    }
}
