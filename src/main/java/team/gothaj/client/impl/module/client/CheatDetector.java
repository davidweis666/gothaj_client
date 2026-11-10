package team.gothaj.client.impl.module.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.other.cheatdetector.NameAnalysis;
import team.gothaj.client.impl.cheatdetector.misc.InvalidNameCheck;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.event.HoverEvent.Action.SHOW_TEXT;

/**
 * @author vajzii
 * @since 2025
 */
public class CheatDetector extends Module {

    //TODO: TargetManager, this is just temporary
    public static List<EntityPlayer> cheaterList = new ArrayList<>();

    private final ListProperty checks = new ListProperty("Checks",
            "AutoBlock", "InvalidName", "Aimbot", "Reach",
            "AutoClicker", "NoSlow", "OmniSprint", "FakeLag", "FastPlace");

    public CheatDetector() {
        super("CheatDetector", "Checks for cheaters in your game", ModuleCategory.CLIENT);
        registerProperties(checks);
    }

    @Override
    public String getSuffix() {
        return "Clientside";
    }

    @Listen
    public void onUpdate(TickEvent event) {
        for (EntityPlayer player : mc.theWorld.playerEntities) {

            if (mc.thePlayer == player) continue;

            GothajClient.getInstance().getCheckManager().getItems().forEach(check -> {
                if (checks.getOptionState(check.getName())) {

                    if (check.checkPlayer(player) && (check instanceof InvalidNameCheck ? System.currentTimeMillis() > player.lastVL + 20000 : System.currentTimeMillis() > player.lastVL + 500)) {
                        player.currentVL++;
                        player.lastVL = System.currentTimeMillis();
                        if (player.currentVL > check.getMinVL()) {
                            GothajClient.getInstance().getNotificationManager().add("Cheat Detector", player.getName() + " is likely cheating", Notification.Type.WARNING);
                            cheaterList.add(player);
                            final ChatComponentText message = new ChatComponentText(
                                    String.format(EnumChatFormatting.DARK_GRAY + "[" + EnumChatFormatting.DARK_PURPLE + "GAC" + EnumChatFormatting.DARK_GRAY + "] " + EnumChatFormatting.GRAY + "%s has failed %s",
                                            player.getName(),
                                            check.getName()
                                    ));

                            ChatComponentText hoverText;

                            if (check instanceof InvalidNameCheck) {
                                NameAnalysis nameAnalysis = NameAnalysis.analyzePlayerName(player.getName());
                                String typeText = "";
                                if (nameAnalysis.isUnderscoreName() && nameAnalysis.isContainsCurrentMonth()) {
                                    typeText = "M & U";
                                } else if (nameAnalysis.isContainsCurrentMonth()) {
                                    typeText = "M";
                                } else if (nameAnalysis.isUnderscoreName()) {
                                    typeText = "U";
                                }

                                hoverText = new ChatComponentText(
                                        String.format("""
                                                §5Cheat Detector\
                                                
                                                §7Additional info\
                                                
                                                §7Type: %s""", typeText));


                            } else {
                                hoverText = new ChatComponentText(
                                        String.format("""
                                                        §5Cheat Detector\
                                                        
                                                        §7Additional info\
                                                        
                                                        §7Current VL: %s\
                                                        
                                                        §7Min VL: %s\
                                                        
                                                        §7Max VL: %s""",
                                                player.currentVL,
                                                check.getMinVL(),
                                                check.getMaxVL()
                                        ));
                            }

                            final HoverEvent hoverEvent = new HoverEvent(SHOW_TEXT, hoverText);

                            final ChatStyle chatStyle = new ChatStyle().setChatHoverEvent(hoverEvent);
                            message.setChatStyle(chatStyle);

                            Logger.addChat(message);
                        }
                    }
                }
            });
        }
    }
    @Listen
    public void onPacket(PacketEvent event) {
        if (mc.theWorld == null) return;

        GothajClient.getInstance().getCheckManager().getItems().forEach(check -> {
            if (checks.getOptionState(check.getName())) {
                check.onPacket(event);
            }
        });
    }
}
