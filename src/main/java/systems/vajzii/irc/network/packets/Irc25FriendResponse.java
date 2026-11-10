package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server: response (accept/decline) to a pending friend request from `otherNick`.
 * Server -> Client: notifies the original requester of the outcome, `otherNick` set to the responder.
 */
public class Irc25FriendResponse extends IrcPacket {

    public String otherNick = "";
    public boolean accepted;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.otherNick = in.readUTF();
        this.accepted = in.readBoolean();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(otherNick != null ? otherNick : "");
        out.writeBoolean(accepted);
    }

    @Override
    public void handle(IrcClient client) {
        if (this.accepted) {
            Logger.ircChat("§e" + this.otherNick + " §aaccepted your friend request!");
        } else {
            Logger.ircChat("§e" + this.otherNick + " §7declined your friend request.");
        }
    }
}
