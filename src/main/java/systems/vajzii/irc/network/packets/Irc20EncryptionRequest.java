package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.util.IrcEncrypt;
import io.netty.channel.ChannelFuture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc20EncryptionRequest extends IrcPacket {
    public byte[] publicKey;

    @Override
    public void read(DataInputStream in) throws IOException {
        int len = in.readInt();
        publicKey = new byte[len];
        in.readFully(publicKey);
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(publicKey.length);
        out.write(publicKey);
    }

    @Override
    public void handle(IrcClient client) {

        byte[] secret = client.generateSharedSecret();

        byte[] encryptedSecret = IrcEncrypt.RSAEncrypt.encrypt(secret, this.publicKey);

        Irc21EncryptionResponse response = new Irc21EncryptionResponse();
        response.encryptedSecret = encryptedSecret;


        if (client.getChannel() != null && client.getChannel().isActive()) {
            ChannelFuture future = client.getChannel().writeAndFlush(response);

            future.addListener(f -> {
                if (f.isSuccess()) {

                    client.getEncryption().enable(secret);

                    Irc00Login login = new Irc00Login();
                    login.uuid = client.getUuid();
                    login.type = client.getType().toString();
                    login.token = client.getToken();
                    login.ircNick = client.getIrcNick();
                    client.sendPacket(login);
                }
            });
        }
    }
}
