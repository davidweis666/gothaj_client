package team.gothaj.client.impl.module.player;

import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3EPacketTeams;
import team.gothaj.client.api.command.Command;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

public class Regen extends Module {

	private final ModeProperty mode = new ModeProperty("Mode", "Grim 1.17", "Grim 1.17");
	private final NumberProperty health = new NumberProperty("Health", 19, 2, 19, 1);
	private final NumberProperty grimPackets = new NumberProperty("Grim packets", 10, 1, 50, 1).setVisible(() -> mode.getProperty().equalsIgnoreCase("Grim 1.17"));

	public Regen() {
        super("Regen", "Regenerates your health.", ModuleCategory.PLAYER);
        registerProperties(mode, health, grimPackets);
	}

	@Listen
	public void onSendPacket(PacketEvent e) {
       /* if (e.getDirection() == PacketEvent.Direction.SEND) {
            if (mc.thePlayer.getHealth() > health.getProperty() || mc.thePlayer.fallDistance > 3) return;
            switch (mode.getProperty().toLowerCase()) {
                case "grim 1.17":
                    if (e.getPacket() instanceof C06PacketPlayerPosLook packet) {
                        for (int i = 0; i < grimPackets.getProperty(); i++) {
                            mc.getNetHandler().getNetworkManager().sendPacketSilentEvent(packet);
                        }
                    }
                    break;
            }
        }*/
        
		if (e.getDirection() == PacketEvent.Direction.RECEIVE) {
			if(e.getPacket() instanceof S06PacketUpdateHealth)e.setCancelled(true);
		}
	}
}
