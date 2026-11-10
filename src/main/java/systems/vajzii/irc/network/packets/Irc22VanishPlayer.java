package systems.vajzii.irc.network.packets;

import lombok.AllArgsConstructor;
import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.players.IrcPlayer;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc22VanishPlayer extends IrcPacket {

    public String player;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.player = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(player);
    }

    @Override
    public void handle(IrcClient client) {
        IrcPlayer vanishedPlayer = IrcPlayer.getPlayerByIRCUsername(this.player);
        IrcPlayer currentPlayer = GothajClient.getInstance().getCurrentPlayer();

        if (vanishedPlayer != null) {
            if (currentPlayer != null && currentPlayer.getIrcNick().equals(this.player)) {
                vanishedPlayer.setVanished(true);
            } else {
                IrcPlayer.clear(vanishedPlayer);
            }
        }
    }
}
