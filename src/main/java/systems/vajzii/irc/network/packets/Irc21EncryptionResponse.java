package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc21EncryptionResponse extends IrcPacket {

    public byte[] encryptedSecret;

    public Irc21EncryptionResponse() {
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        int length = in.readInt();
        this.encryptedSecret = new byte[length];
        in.readFully(this.encryptedSecret);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(this.encryptedSecret.length);
        out.write(this.encryptedSecret);
    }

    @Override
    public void handle(IrcClient client) {

    }
}
