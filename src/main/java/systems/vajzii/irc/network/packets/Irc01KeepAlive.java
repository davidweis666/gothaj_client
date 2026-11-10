package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc01KeepAlive extends IrcPacket {

	@Override
	public void read(DataInputStream in) throws IOException { }

	@Override
	public void write(DataOutputStream out) throws IOException { }


	@Override
	public void handle(IrcClient client) {
		client.sendPacket(this);
	}
}
