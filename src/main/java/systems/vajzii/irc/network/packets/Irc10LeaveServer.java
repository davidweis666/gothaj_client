package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc10LeaveServer extends IrcPacket {
	public String reason;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.reason = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(reason);
	}

	@Override
	public void handle(IrcClient client) {
	}
}
