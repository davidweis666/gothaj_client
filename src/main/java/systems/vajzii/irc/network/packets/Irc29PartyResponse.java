package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server only: response (accept/decline) to a party invite from `fromNick`.
 * The server never echoes this back - the outcome arrives via Irc30PartyUpdate instead.
 */
public class Irc29PartyResponse extends IrcPacket {

    public String fromNick = "";
    public boolean accepted;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.fromNick = in.readUTF();
        this.accepted = in.readBoolean();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(fromNick != null ? fromNick : "");
        out.writeBoolean(accepted);
    }

    @Override
    public void handle(IrcClient client) {
        // Client -> Server direction only; nothing to do if ever received.
    }
}
