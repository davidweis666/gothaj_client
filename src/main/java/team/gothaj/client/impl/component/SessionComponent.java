package team.gothaj.client.impl.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import team.gothaj.client.api.component.Component;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
@Setter
public class SessionComponent extends Component {

    private long playTime;

    private int kills, deaths;

    @Listen
    public void onTick(TickEvent event) {
        playTime++;
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (Hud.widgets.getOptionState("Session Info")) {


            if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
                if (event.getPacket() instanceof S2BPacketChangeGameState state) {
                    if (state.getGameState() == 3) {
                        if (mc.thePlayer.isSpectator() || mc.ingameGUI.getDisplayedTitle().contains("DIED")) {
                            deaths += 1;
                        }
                    }

                }
                if (event.getPacket() instanceof S02PacketChat packet) {
                    if (packet.getChatComponent().getUnformattedText().contains("You killed")
                            || packet.getChatComponent().getUnformattedText().contains("by " + mc.session.getUsername())) {
                        kills += 1;
                    }
                    if ((packet.getChatComponent().getFormattedText().startsWith(mc.session.getUsername()) && packet.getChatComponent().getFormattedText().contains("by "))
                            || packet.getChatComponent().getUnformattedText().contains(mc.session.getUsername() + " was knocked")
                            || packet.getChatComponent().getUnformattedText().contains(mc.session.getUsername() + " fell into")
                            || packet.getChatComponent().getUnformattedText().contains(mc.session.getUsername() + " was killed")) {
                        deaths += 1;
                    }


                    if (packet.getChatComponent().getUnformattedText().contains(" in Limbo") || packet.getChatComponent().getUnformattedText().contains(" to Limbo")) {
                        mc.thePlayer.sendChatMessage("/l");
                    }
                }
            }
        }
    }

    public void reset() {
        setPlayTime(0);
        setKills(0);
        setDeaths(0);
    }
}
