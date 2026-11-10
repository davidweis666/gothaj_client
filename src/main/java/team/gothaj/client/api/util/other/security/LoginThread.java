package team.gothaj.client.api.util.other.security;

import systems.vajzii.irc.main.IrcManager;
import systems.vajzii.irc.network.packets.Irc07ClientCommand;
import systems.vajzii.irc.util.IRCUtil;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.account.AccountRepository;
import team.gothaj.client.api.cheatdetector.CheckManager;
import team.gothaj.client.api.command.CommandManager;
import team.gothaj.client.api.component.ComponentManager;
import team.gothaj.client.api.config.ConfigManager;
import team.gothaj.client.api.friend.FriendManager;
import team.gothaj.client.api.listener.Listener;
import team.gothaj.client.api.proxy.ProxyRepository;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.user.User;
import team.gothaj.client.api.waypoint.WaypointManager;
import team.gothaj.client.impl.gui.menu.GothajTestMenu;
import team.gothaj.client.impl.gui.security.ConsoleLogin;
import team.gothaj.client.main.GothajClient;
import team.gothaj.cloud.client.Cloud;
import team.gothaj.cloud.share.packet.Packet;
import team.gothaj.cloud.share.util.Sha256;

import java.security.NoSuchAlgorithmException;

/**
 * @author vajzii
 * @since 2025
 */
public class LoginThread extends Thread implements Utils {

    public final String username, password, oldPassword;

    public static String status;
    private final boolean rememberCredentials;

    public LoginThread(String username, String password) {
        this(username, password, false);
    }

    public LoginThread(String username, String password, boolean rememberCredentials) {
        this.username = username;
        this.password = Sha256.hex(password);
        this.oldPassword = password;
        this.rememberCredentials = rememberCredentials;
    }

    @Override
    public void run() {
        status = "Logging in...";

        try {
            String hardware;
            try {
                hardware = IRCUtil.getHardwareID();
            } catch (NoSuchAlgorithmException e) {
                status = EnumChatFormatting.RED + "Failed to get hardware information.";
                e.printStackTrace(System.err);
                return;
            }

            try {
                Cloud cloud = Cloud.getInstance();
                if (!cloud.waitUntilReady(30000L)) {
                    status = EnumChatFormatting.RED + "Login failed: not connected to server.";
                    return;
                }

                Packet authRequest = Packet.builder().type("AUTH").putString("username", username).putString("password", password).putString("hardware", hardware).build();

                Packet response = cloud.sendRequestSync(authRequest, 30000);
                handleResponse(response);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                status = EnumChatFormatting.RED + "Login failed: " + e.getMessage();
            }
        } finally {
            ConsoleLogin.loginQueued = false;
        }
    }

    private void handleResponse(Packet response) {
        if (response.getType().equals("ERROR")) {
            status = EnumChatFormatting.RED + response.getString("message") + "!";
            return;
        }

        if (!response.getType().equals("AUTH")) {
            status = EnumChatFormatting.RED + "Invalid response type: " + response.getType();
            return;
        }

        boolean success = response.getBoolean("success");
        String action = response.getString("action");
        String uid = response.getString("uid");
        String message = response.getString("message");

        if (!success) {
            status = EnumChatFormatting.RED + message + "!";
            return;
        } else {
            status = "Checking authentication...";

            sleepUninterrupted(400);

            if ("hwid_assigned".equals(action)) {
                status = EnumChatFormatting.GREEN + "First Login detected! Setting up Hardware ID...";
                sleepUninterrupted(300);
                status = EnumChatFormatting.GREEN + message + "!";
                sleepUninterrupted(300);
            }

            if ("hwid_verified".equals(action)) {
                status = EnumChatFormatting.GREEN + "Hardware ID verification...";
                sleepUninterrupted(300);
            }

            if (rememberCredentials) {
                RememberedCredentials.save(username, oldPassword);
            } else {
                RememberedCredentials.delete();
            }

            status = EnumChatFormatting.GREEN + "Welcome, " + username + "!";

            GothajClient.getInstance().setUser(new User(username, uid));

            GothajClient.getInstance().initIRC();

            GothajClient.getInstance().setCheckManager(new CheckManager());
            GothajClient.getInstance().getCheckManager().init();

            GothajClient.getInstance().getModuleManager().init();

            GothajClient.getInstance().setCommandManager(new CommandManager());
            GothajClient.getInstance().getCommandManager().init();

            GothajClient.getInstance().setAccountRepository(new AccountRepository());
            GothajClient.getInstance().getAccountRepository().init();

            GothajClient.getInstance().setProxyRepository(new ProxyRepository());
            GothajClient.getInstance().getProxyRepository().init();

            GothajClient.getInstance().setComponentManager(new ComponentManager());
            GothajClient.getInstance().getComponentManager().init();

            GothajClient.getInstance().setConfigManager(new ConfigManager());
            GothajClient.getInstance().getConfigManager().init();

            GothajClient.getInstance().setWaypointManager(new WaypointManager());
            GothajClient.getInstance().getWaypointManager().init();

            GothajClient.getInstance().setFriendManager(new FriendManager());
            GothajClient.getInstance().getFriendManager().init();

            GothajClient.getInstance().listeners().forEach(Listener::register);

            String nick = GothajClient.getInstance().getUser().getUsername();
            Irc07ClientCommand irc07ClientCommand = new Irc07ClientCommand();
            irc07ClientCommand.command = "nick " + nick;
            IrcManager.getIrcClient().sendPacket(irc07ClientCommand);

            sleepUninterrupted(250);

            mc.displayGuiScreen(new GothajTestMenu());
        }
    }

    private void sleepUninterrupted(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }
}
