package team.gothaj.client.impl.module.misc;

import net.minecraft.network.play.server.S02PacketChat;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class AutoGG extends Module {

    boolean sent = false;

    public AutoGG() {
        super("AutoGG", "", ModuleCategory.MISC);
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!sent && mc.ingameGUI.getDisplayedTitle().contains("VICTORY")) {
            //mc.thePlayer.sendChatMessage("gg");
            sent = true;
        }
       /* if (sent && mc.ingameGUI.getDisplayedTitle().isEmpty()) {
            sent = false;
        }*/
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
           if (event.getPacket() instanceof S02PacketChat chat) {
               if (!sent && (chat.getChatComponent().getUnformattedText().contains("Reward Summary") || chat.getChatComponent().getUnformattedText().contains("won the game") || chat.getChatComponent().getUnformattedText().contains("WINNER") || chat.getChatComponent().getUnformattedText().contains("Winner"))) {
                   mc.thePlayer.sendChatMessage("gg");
                   sent = true;
               } else if (sent && chat.getChatComponent().getUnformattedText().contains("Sending you to")) {
                   sent = false;
               }
           }
        }
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        sent = false;
    }
}
