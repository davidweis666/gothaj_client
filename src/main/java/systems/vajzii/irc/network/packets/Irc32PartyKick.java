package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server only: (leader only) kick `targetNick` from the party.
 */
public class Irc32PartyKick extends IrcPacket {

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
        // Client -> Server direction only; nothing to do if ever received.
    }
}
