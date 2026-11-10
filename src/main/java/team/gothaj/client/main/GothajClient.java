package team.gothaj.client.main;

import com.cortex.legacy.Cortex;
import systems.vajzii.irc.IrcType;
import systems.vajzii.irc.main.IrcManager;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.util.IRCUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import team.gothaj.client.api.account.AccountRepository;
import team.gothaj.client.api.cheatdetector.CheckManager;
import team.gothaj.client.api.command.CommandManager;
import team.gothaj.client.api.component.ComponentManager;
import team.gothaj.client.api.config.ConfigManager;
import team.gothaj.client.api.friend.FriendManager;
import team.gothaj.client.api.listener.Listener;
import team.gothaj.client.api.module.ModuleManager;
import team.gothaj.client.api.proxy.ProxyRepository;
import team.gothaj.client.api.resource.GothajResource;
import team.gothaj.client.api.theme.ThemeManager;
import team.gothaj.client.api.util.ActionManager;
import team.gothaj.client.api.util.Logger;
import com.cortex.legacy.text.CortexFonts;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.rotation.library.LibraryClient;
import team.gothaj.client.api.util.user.User;
import team.gothaj.client.api.waypoint.WaypointManager;
import team.gothaj.client.api.widget.WidgetManager;
import team.gothaj.client.impl.listener.chat.ChatListener;
import team.gothaj.client.impl.listener.input.InputListener;
import team.gothaj.client.via.GothajVia;
import team.gothaj.client.api.notification.NotificationManager;
import team.gothaj.cloud.client.Cloud;
import team.gothaj.cloud.share.packet.Packet;
import team.gothaj.eventbus.PubSub;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static team.gothaj.client.api.util.Utils.mc;

@Getter
public final class GothajClient {

    @Getter
    private static final GothajClient Instance = new GothajClient();

    private final String name = "Gothaj", fancyName = "gothaj.top", version = "4.0-a6 (Ex 2)";
    private final String[] authors = {"vajzii", "dejvak", "kawase", "horizon", "jrexha", "h2x1", "ssh444"};

    @Setter
    private User user = new User("user", "0000");

    private final File clientDir = new File(Minecraft.getMinecraft().mcDataDir, "gothaj");

    @Setter
    private CommandManager commandManager;
    @Setter
    private ModuleManager moduleManager;
    @Getter
    private final GothajResource resource = new GothajResource();
    @Setter
    private AccountRepository accountRepository;
    @Setter
    private ProxyRepository proxyRepository;
    @Setter
    private WidgetManager widgetManager;
    @Setter
    private ComponentManager componentManager;
    @Setter
    private ConfigManager configManager;
    @Setter
    private NotificationManager notificationManager;
    @Setter
    private ActionManager actionManager;
    @Setter
    private WaypointManager waypointManager;
    @Setter
    private CheckManager checkManager;
    @Setter
    private FriendManager friendManager;
    @Setter
    private ThemeManager themeManager;

    private LibraryClient libraryClient;

    private IrcManager ircManager;

    private final PubSub eventBus;

    public static final ScheduledExecutorService scheduledExecutorService = newScheduledThreadPool(1);

    private boolean loaded;

    private double PI = Double.NaN;
    public final long menuMDuration = 115240L;

    @Getter
    @Setter
    private int widgetKey;

    private GothajClient() {
        eventBus = new PubSub();
        eventBus.subscribe(this);
    }

    public void init() {
    	GothajVia.init();
    	
        Cloud.getInstance();

        RenderUtil.initTime = System.currentTimeMillis();

        Display.setTitle(name + " " + version);

        if (!clientDir.exists())
            if (clientDir.mkdirs())
                Logger.log("Directory init");


        FontUtil.setupFonts();

        Cortex.init();
        CortexFonts.loadFonts();

        moduleManager = new ModuleManager();
        themeManager = new ThemeManager();
        themeManager.init();

        configManager = new ConfigManager();
        configManager.init();
        
        widgetManager = new WidgetManager();
        widgetManager.init();
        widgetKey = Keyboard.KEY_INSERT;

        notificationManager = new NotificationManager();
        notificationManager.init();

        actionManager = new ActionManager();
        actionManager.register();

        if (libraryClient != null && !libraryClient.isConnected()) {
            libraryClient.ensureConnected();
        }

        loaded = true;

        mc.gameSettings.guiScale = 2;
        mc.gameSettings.ofFastRender = false;
    }

    public void shutdown() {
        if (accountRepository != null) accountRepository.save();
        if (proxyRepository != null) proxyRepository.save();
        if (configManager != null) {
            configManager.saveActiveConfig();
            configManager.saveWidgets();
        }
        if (friendManager != null) friendManager.save();
//        sapphire.getSphContext().dispose();
        if (libraryClient != null) libraryClient.close();
    }

    public void initIRC() {
        try {
            this.ircManager = new IrcManager(IRCUtil.getHardwareID());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            this.getIrcManager().buildIrcClient(Cloud.getInstance().sendRequestSync(
                    Packet.builder().type("GET_VALUE").putString("name", "IRC_TOKEN").build(), 10000L).getString("value"),
                    IrcType.GOTHAJ,
                    getUser().getUsername(),
                    Minecraft.getMinecraft().getSession().getUsername());
        } catch (Exception e) {

        }

    }

    public List<Listener> listeners() {
        return Arrays.asList(
                new ChatListener(),
                new InputListener()
        );
    }

    public IrcPlayer getCurrentPlayer() {
        return IrcPlayer.getByIngameName(this.getIrcManager().getClient().getIngameName());
    }

    public double getPI() {
        while (Double.isNaN(PI)) {
            try {
                PI = Cloud.getInstance().sendRequestSync(
                        Packet.builder()
                                .type("GET_VALUE")
                                .putString("name", "MATH_PI")
                                .build(),
                        30000L
                ).getDouble("value");
            } catch (Exception ignored) {
            }
        }

        return PI;
    }
}
