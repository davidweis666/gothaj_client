package team.gothaj.client.impl.component;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.exceptions.UnsupportedOsType;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import net.minecraft.client.gui.GuiDisconnected;
import team.gothaj.client.api.component.Component;
import team.gothaj.client.impl.module.client.NoDiscordRPC;
import team.gothaj.client.main.GothajClient;
import team.gothaj.cloud.client.Cloud;
import team.gothaj.cloud.share.packet.Packet;

public class DiscordComponent extends Component implements Runnable {

    private DiscordRpc rpc = new DiscordRpc();
    private boolean initialized = false;
    private final Thread thread;
    private volatile boolean running = false;
    private String ID;

    public DiscordComponent() {
        running = true;
        thread = new Thread(this, "DiscordRPC-Thread");
        thread.start();
    }

    @Override
    public void run() {
        while (running) {
            boolean noDiscord = GothajClient.getInstance().getModuleManager().find(NoDiscordRPC.class).isToggled();

            if (noDiscord) {
                if (initialized) {
                    rpc.shutdown();
                    initialized = false;
                }
            } else {
                if (!initialized) {
                    RPCEventHandler handler = new RPCEventHandler() {
                        @Override
                        public void ready(User user) {
                            updatePresence();
                        }
                        @Override
                        public void disconnected(ErrorCode errorCode, String message) {
                            System.out.println("Disconnected " + errorCode + " - " + message);
                            initialized = false;
                        }
                        @Override
                        public void errored(ErrorCode errorCode, String message) {
                            System.out.println("Errored " + errorCode + " - " + message);
                        }
                    };
                    try {

                        rpc = new DiscordRpc();

                        if (ID == null)
                            try {
                                ID = Cloud.getInstance().sendRequestSync(
                                        Packet.builder()
                                                .type("GET_VALUE")
                                                .putString("name", "DISCORD_ID")
                                                .build(),
                                                10000L)
                                        .getString("value");
                            } catch (Exception e) {
                                return;
                            }

                        rpc.init(ID, handler, false);
                        initialized = true;
                    } catch (UnsupportedOsType e) {
                        e.printStackTrace(System.err);
                    }
                } else {
                    updatePresence();
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updatePresence() {
        if (!initialized) return;
        DiscordRichPresence presence;

        if (!GothajClient.getInstance().getUser().getUid().equalsIgnoreCase("0000") && GothajClient.getInstance().getCurrentPlayer() != null) {
            presence = DiscordRichPresence.builder()
                    .details("Rank: " +  GothajClient.getInstance().getCurrentPlayer().getRank().getName())
                    .state(mc.getCurrentServerData() == null || mc.currentScreen instanceof GuiDisconnected ? "In Menu" : mc.isSingleplayer() ? "Singleplayer" : "Server: " + mc.getCurrentServerData().serverIP)
                    .largeImageKey("maintest")
                    .largeImageText(GothajClient.getInstance().getUser().getFormattedUser())
                    .activityType(ActivityType.PLAYING)
                    .button(DiscordRichPresence.RPCButton.of("Buy Now!", "https://gothaj.top"))
                    .button(DiscordRichPresence.RPCButton.of("Join Discord!", "https://discord.gg/akUsCyCEaf"))
                    .build();
        } else {
            presence = DiscordRichPresence.builder()
                    .details("Logging in...")
                    .largeImageKey("maintest")
                    .largeImageText("Waiting for login...")
                    .activityType(ActivityType.PLAYING)
                    .button(DiscordRichPresence.RPCButton.of("Buy Now!", "https://gothaj.top"))
                    .button(DiscordRichPresence.RPCButton.of("Join Discord!", "https://discord.gg/akUsCyCEaf"))
                    .build();
        }
        rpc.updatePresence(presence);
    }

    @Override
    public void onDisable() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) {}
        }
        if (initialized) {
            rpc.shutdown();
            initialized = false;
        }
        super.onDisable();
    }
}
