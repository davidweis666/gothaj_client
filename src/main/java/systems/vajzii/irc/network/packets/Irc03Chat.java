package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.players.IrcPlayer;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc03Chat extends IrcPacket {
	public String player;
	public String message;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.player = in.readUTF();
		this.message = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(message != null ? message : "");
	}

	@Override
	public void handle(IrcClient client) {
		Logger.ircChat(IrcPlayer.getPlayerByIRCUsername(this.player).getRank().getChatColor() + this.player + EnumChatFormatting.DARK_GRAY + ": §7" + this.message);
	}
}
