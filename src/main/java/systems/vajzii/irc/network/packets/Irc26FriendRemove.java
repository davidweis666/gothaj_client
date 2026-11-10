package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server: remove `targetNick` from friends.
 * Server -> Client: notifies the other side that the friendship was removed.
 */
public class Irc26FriendRemove extends IrcPacket {

    public String targetNick = "";

    @Override
    public void read(DataInputStream in) throws IOException {
        this.targetNick = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(targetNick != null ? targetNick : "");
    }

    @Override
    public void handle(IrcClient client) {
        Logger.ircChat("§e" + this.targetNick + " §7removed you from their friends.");
    }
}
