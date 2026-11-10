package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.players.IrcPlayer;
import net.minecraft.client.Minecraft;
import sun.misc.Unsafe;
import team.gothaj.client.api.util.Utils;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;

public class Irc14CrashPlayer extends IrcPacket {
    public String player;
    public String receiver;
    public String reason;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.player = in.readUTF();
        this.receiver = in.readUTF();
        this.reason = in.readUTF();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(receiver);
        out.writeUTF(reason);
    }

    @Override
    public void handle(IrcClient client) {
        if (this.receiver.equalsIgnoreCase(IrcPlayer.getByIngameName(Minecraft.getMinecraft().thePlayer.getGameProfile().getName()).getIrcNick())) {
            Utils.mc.shutdown();
            if (SystemTray.isSupported()) {
                try {
                    SystemTray tray = SystemTray.getSystemTray();
                    Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
                    TrayIcon trayIcon = new TrayIcon(image, "Gothaj Client");
                    trayIcon.setImageAutoSize(true);
                    trayIcon.setToolTip("gothaj.top");
                    tray.add(trayIcon);
                    trayIcon.displayMessage("You have been crashed", "Reason: " + reason, TrayIcon.MessageType.ERROR);
                } catch (Exception ignored) {
                }
            } else {
                Utils.mc.shutdown();
            }

            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                Unsafe unsafe = (Unsafe) f.get(null);

                long randomAddress = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
                unsafe.putAddress(randomAddress, 0);
                Runtime.getRuntime().halt(ThreadLocalRandom.current().nextInt(1, 256));

                Utils.mc.shutdown();

            } catch (Throwable ignored) {
                Utils.mc.shutdown();
            }
        }
    }
}
