package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server: chat message scoped to the sender's current party - only
 * the message is written, the server fills in the sender's nickname.
 * Server -> Client: relayed to every party member with `player` set to the
 * sender, so incoming reads two fields while outgoing writes one.
 */
public class Irc33PartyChat extends IrcPacket {

    public String player = "";
    public String message = "";

    @Override
    public void read(DataInputStream in) throws IOException {
        this.player = in.readUTF();
        this.message = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(message != null ? message : "");
    }

    @Override
    public void handle(IrcClient client) {
        Logger.ircPartyChat(this.player, this.message);
    }
}
