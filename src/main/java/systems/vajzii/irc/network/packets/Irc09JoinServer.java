package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc09JoinServer extends IrcPacket {
	public String server;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.server = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(server);
	}

	@Override
	public void handle(IrcClient client) {
	}
}
