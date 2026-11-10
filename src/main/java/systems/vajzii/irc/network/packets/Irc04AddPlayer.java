package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.players.IrcRank;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc04AddPlayer extends IrcPacket {

	public String ingameName;
	public String ircName;
	public int rankID;
	public String serverIp;
	public String type;

	@Override
	public void read(DataInputStream in) throws IOException {
		this.ingameName = in.readUTF();
		this.ircName = in.readUTF();
		this.rankID = in.readInt();
		this.serverIp = in.readUTF();
		this.type = in.readUTF();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(ingameName);
		out.writeUTF(ircName);
		out.writeInt(rankID);
		out.writeUTF(serverIp);
		out.writeUTF(type);
	}

	@Override
	public void handle(IrcClient client) {
		new IrcPlayer(this.ircName, this.ingameName, this.serverIp, IrcRank.getById(this.rankID), this.type);
	}
}
