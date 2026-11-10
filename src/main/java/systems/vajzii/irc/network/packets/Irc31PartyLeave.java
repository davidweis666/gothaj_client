package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Client -> Server only: leave the sender's current party.
 */
public class Irc31PartyLeave extends IrcPacket {

    @Override
    public void read(DataInputStream in) throws IOException {
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
    }

    @Override
    public void handle(IrcClient client) {
        // Client -> Server direction only; nothing to do if ever received.
    }
}
