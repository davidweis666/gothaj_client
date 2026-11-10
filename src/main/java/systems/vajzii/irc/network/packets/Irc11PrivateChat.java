package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.players.IrcPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc11PrivateChat extends IrcPacket {
    public String player;
    public String message;
    public String receiver;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.player = in.readUTF();
        this.message = in.readUTF();
        this.receiver = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(message);
        out.writeUTF(receiver);
    }

    @Override
    public void handle(IrcClient client) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            if (this.receiver.equalsIgnoreCase(IrcPlayer.getByIngameName(Minecraft.getMinecraft().thePlayer.getGameProfile().getName()).getIrcNick())) {
                Logger.ircChatPrivate(IrcPlayer.getPlayerByIRCUsername(this.player).getRank().getChatColor() + this.player + EnumChatFormatting.DARK_GRAY + ": §7" + this.message, true);
            }
        }
    }
}

