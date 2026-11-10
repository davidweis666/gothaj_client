package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import net.minecraft.client.Minecraft;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc02Info extends IrcPacket {
	public String message;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.message = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(message != null ? message : "");
	}

	@Override
	public void handle(IrcClient client) {
		if (Minecraft.getMinecraft().theWorld != null) {
			if (message.contains("for") && message.contains("has been banned on")) {
				String[] parts = message.split("\\.");
				String server = parts.length >= 3 ? message.split("\\.")[1] : message.split("\\.")[0];

				if (Minecraft.getMinecraft().getCurrentServerData().serverIP.contains(server)) {
					Logger.ircChat(message);
				}

			} else {
				//fr we dont allow this because of spam
				Logger.ircChat(message);
			}
		}
	}
}
