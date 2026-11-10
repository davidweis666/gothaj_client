package systems.vajzii.irc.network;

import systems.vajzii.irc.IrcClient;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class IrcPacket {

	public abstract void read(DataInputStream in) throws IOException;

	public abstract void write(DataOutputStream out) throws IOException;

	public abstract void handle(IrcClient client);
}
