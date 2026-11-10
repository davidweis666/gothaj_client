package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc07ClientCommand extends IrcPacket {
	public String command;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.command = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(command);
	}

	@Override
	public void handle(IrcClient client) {
	}
}
