package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.friend.FriendManager;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server: request to friend `toNick` (fromNick left blank, filled by the server).
 * Server -> Client: delivered to the target with `fromNick` set to the requester.
 */
public class Irc24FriendRequest extends IrcPacket {

    public String fromNick = "";
    public String toNick = "";

    @Override
    public void read(DataInputStream in) throws IOException {
        this.fromNick = in.readUTF();
        this.toNick = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(fromNick != null ? fromNick : "");
        out.writeUTF(toNick != null ? toNick : "");
    }

    @Override
    public void handle(IrcClient client) {
        FriendManager.addIncomingRequest(this.fromNick);
        Logger.ircChat("§e" + this.fromNick + " §7sent you a friend request! Use §f.irc friend accept " + this.fromNick);
    }
}
