package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc05RemovePlayer extends IrcPacket {
	public String ingameName;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.ingameName = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(ingameName);
	}

	@Override
	public void handle(IrcClient client) {
	}
}
