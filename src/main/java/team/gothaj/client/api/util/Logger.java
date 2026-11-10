package team.gothaj.client.api.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.impl.module.client.Debug;
import team.gothaj.client.main.GothajClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

    @Getter
    @Setter
    public static String ircNamePrefix;

    public static void clientChat(String message) {
        addChat(EnumChatFormatting.DARK_GRAY + "["  + EnumChatFormatting.DARK_PURPLE + "G" + EnumChatFormatting.DARK_GRAY +  "] " + EnumChatFormatting.GRAY + message);
    }

    public static void debugChat(Module mod, String message) {
        if (GothajClient.getInstance().getModuleManager().find(Debug.class).isToggled())
            addChat(EnumChatFormatting.DARK_GRAY + "["  + EnumChatFormatting.DARK_PURPLE + mod.getName() + EnumChatFormatting.DARK_GRAY +  "] " + EnumChatFormatting.GRAY + message);
    }

    public static void spotifyChat(String message) {
        addChat(EnumChatFormatting.DARK_GRAY + "["  + EnumChatFormatting.GREEN + "Spotify" + EnumChatFormatting.DARK_GRAY +  "] " + EnumChatFormatting.GRAY + message);
    }

    public static void ircChat(String message) {
        addChat(EnumChatFormatting.DARK_GRAY + "["  + EnumChatFormatting.DARK_AQUA + "IRC" + EnumChatFormatting.DARK_GRAY +  "] " + EnumChatFormatting.GRAY + message);
    }

    public static void ircChatPrivate(String message, boolean receive) {
        addChat(EnumChatFormatting.DARK_GRAY + "["  + EnumChatFormatting.BLUE + (receive ? "PM From" : "PM Sent") + EnumChatFormatting.DARK_GRAY +  "] " + EnumChatFormatting.GRAY + message);
    }

    public static void ircLocalChat(String message) {
        addChat(EnumChatFormatting.DARK_GRAY + "["  + EnumChatFormatting.DARK_AQUA + "IRL" + EnumChatFormatting.DARK_GRAY +  "] " + EnumChatFormatting.GRAY + message);
    }

    public static void ircPartyChat(String player, String message) {
        addChat(EnumChatFormatting.DARK_GRAY + "[" + EnumChatFormatting.LIGHT_PURPLE + "Party" + EnumChatFormatting.DARK_GRAY + "] " + EnumChatFormatting.GRAY + player + EnumChatFormatting.DARK_GRAY + ": §7" + message);
    }

    public static void addChat(String text) {
        if (Minecraft.getMinecraft().theWorld != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(text));
        }
    }

    public static void addChat(IChatComponent text) {
        if (Minecraft.getMinecraft().theWorld != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(text);
        }
    }

    public static void log(Object message) {
        System.out.printf("[%s] [%s] %s%n", new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()), GothajClient.getInstance().getName(), message);
    }
}
