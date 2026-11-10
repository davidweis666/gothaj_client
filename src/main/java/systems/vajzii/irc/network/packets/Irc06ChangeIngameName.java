package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc06ChangeIngameName extends IrcPacket {
	public String name;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.name = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(name);
	}

	@Override
	public void handle(IrcClient client) {
	}
}
