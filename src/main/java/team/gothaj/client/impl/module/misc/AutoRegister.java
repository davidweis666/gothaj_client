package team.gothaj.client.impl.module.misc;

import net.minecraft.network.play.server.S02PacketChat;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.InputProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class AutoRegister extends Module {

	public final ModeProperty passMode = new ModeProperty("Password Mode", "Random", "Random", "Input");

	public final NumberProperty randomPassLength = new NumberProperty("Random Password Length", 10, 4, 16, 1)
			.setVisible(() -> passMode.is("Random"));

	public final InputProperty ownPass = new InputProperty("Password", "", true).setVisible(() -> passMode.is("Input"));

	private String password;

	private String lastServer;

	private boolean registerDone, sentRegister;

	public AutoRegister() {
		super("AutoRegister", "", ModuleCategory.MISC);
		registerProperties(passMode, randomPassLength, ownPass);
	}

	@Listen
	public void onPacket(PacketEvent event) {

		if (!registerDone && event.getPacket() instanceof S02PacketChat chat) {
			if (chat.getChatComponent().getUnformattedText().contains("/reg")) {

				switch (passMode.getProperty()) {
				case "Input" -> password = ownPass.getProperty();
				case "Random" -> password = MathUtil.getRandomString(randomPassLength.getProperty().intValue());
				}
				mc.thePlayer.sendChatMessage("/reg " + password + " " + password);

				sentRegister = true;
			}
		}
	}

	@Listen
	public void onWorldChange(WorldChangeEvent event) {
		try {
			if (mc.isSingleplayer())
				return;

			if (!mc.getCurrentServerData().serverIP.equalsIgnoreCase(lastServer)) {
				registerDone = false;
			}

			if (sentRegister && !registerDone) {
				lastServer = mc.getCurrentServerData().serverIP;
				registerDone = true;
				sentRegister = false;
				setToggled(false);
			}
		} catch (Exception ex) {

		}
	}
}
