package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc00Login extends IrcPacket {
	public String uuid;
	public String type;
	public String token;
	public String ircNick;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.uuid = in.readUTF();
		this.type = in.readUTF();
		this.token = in.readUTF();
		this.ircNick = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(uuid != null ? uuid : "");
		out.writeUTF(type != null ? type : "");
		out.writeUTF(token != null ? token : "");
		out.writeUTF(ircNick != null ? ircNick : "");
	}

	@Override
	public void handle(IrcClient client) {
	}
}
