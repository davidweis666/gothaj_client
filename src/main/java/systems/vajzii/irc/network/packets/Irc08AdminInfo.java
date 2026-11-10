package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc08AdminInfo extends IrcPacket {
    public String message;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.message = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(message);
    }

    @Override
    public void handle(IrcClient client) {
    }
}
