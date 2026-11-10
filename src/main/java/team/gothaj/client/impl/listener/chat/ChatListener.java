package team.gothaj.client.impl.listener.chat;

import net.minecraft.network.play.client.C01PacketChatMessage;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.listener.Listener;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.cloud.client.Cloud;
import team.gothaj.cloud.share.packet.Packet;
import team.gothaj.eventbus.Listen;

public class ChatListener extends Listener {

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.SEND) {
            if (event.getPacket() instanceof C01PacketChatMessage packetChat) {
                String msg = packetChat.getMessage();

                if (GothajClient.getInstance().getCommandManager().getPrefix() == null) {
                    try {
                        GothajClient.getInstance().getCommandManager().prefix = Cloud.getInstance().sendRequestSync(
                                Packet.builder()
                                        .type("GET_VALUE")
                                        .putString("name", "CMD_PREFIX")
                                        .build(),
                                3000L)
                                .getString("value");
                    } catch (Exception e) {
                        GothajClient.getInstance().getCommandManager().prefix = ".";
                    }
                }

                if (msg.startsWith(GothajClient.getInstance().getCommandManager().getPrefix())) {
                	String[] args = msg.split(" ");
                    String commandStr = args[0].substring(1);

                    Command command = GothajClient.getInstance().getCommandManager().find(commandStr);

                    if (command != null) {
                    	event.setCancelled(true);
                        command.runCommand(args);
                    } else {
                    	event.setCancelled(true);
                        Logger.clientChat("Command not found!");
                    }
                }
            }
        }
    }
}
