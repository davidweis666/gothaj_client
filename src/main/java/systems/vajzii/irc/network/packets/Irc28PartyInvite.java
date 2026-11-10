package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server: invite `toNick` to the sender's party.
 * Server -> Client: delivered to the target with `fromNick` set to the inviter.
 */
public class Irc28PartyInvite extends IrcPacket {

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
        Logger.ircChat("§e" + this.fromNick + " §7invited you to their party! Use §f.irc party accept " + this.fromNick);
    }
}
