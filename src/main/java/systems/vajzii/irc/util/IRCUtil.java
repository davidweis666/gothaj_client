package systems.vajzii.irc.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.players.IrcRank;

import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class IRCUtil {

    public static String getIrcPrefix(GameProfile gameProfile) {
        try {
            IrcPlayer ircPlayer1 = IrcPlayer.getByIngameName(gameProfile.getName());
            if (ircPlayer1 != null) {
                if (ircPlayer1.getRank() == IrcRank.ADMIN) {
                    return "§8[§4Administrator§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.DEV) {
                    return "§8[§2Developer§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.MOD) {
                    return "§8[§5Moderator§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.MEDIA) {
                    return "§8[§bMedia§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.VIP) {
                    return "§8[§6VIP§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.USER) {
                    return "§8[§aUser§8] §f";
                }
            }

        } catch (Exception e) {

        }
        return "";
    }

    public static IrcRank getSelfRank() {
        try {
            IrcPlayer ircPlayer1 = IrcPlayer.getByIngameName(Minecraft.getMinecraft().session.getProfile().getName());
            if (ircPlayer1 != null) {
                return ircPlayer1.getRank();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean isDevOrHigher() {
        var rank = getSelfRank();
        if (rank == null) return false;
	    return rank == IrcRank.ADMIN || rank == IrcRank.DEV;
    }

    public static Color getIrcColor(GameProfile gameProfile) {
        IrcPlayer ircPlayer1 = IrcPlayer.getByIngameName(gameProfile.getName());

        if (ircPlayer1 != null) {
            if (ircPlayer1.getRank() == IrcRank.ADMIN) {
                return new Color(0xAA0000);
            } else if (ircPlayer1.getRank() == IrcRank.DEV) {
                return new Color(0x00AA00);
            } else if (ircPlayer1.getRank() == IrcRank.MOD) {
                return new Color(0xAA00AA);
            } else if (ircPlayer1.getRank() == IrcRank.MEDIA) {
                return new Color(0x55FFFF);
            } else if (ircPlayer1.getRank() == IrcRank.VIP) {
                return new Color(0xFFAA00);
            } else if (ircPlayer1.getRank() == IrcRank.USER) {
                return new Color(0x55FF55);
            }
        }
        return Color.black;
    }

    public static String getIrcPrefix(String name) {
        try {
            IrcPlayer ircPlayer1 = IrcPlayer.getByIngameName(name);
            if (ircPlayer1 != null) {
                if (ircPlayer1.getRank() == IrcRank.ADMIN) {
                    return "§8[§4Administrator§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.DEV) {
                    return "§8[§2Developer§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.MOD) {
                    return "§8[§5Moderator§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.MEDIA) {
                    return "§8[§bMedia§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.VIP) {
                    return "§8[§6VIP§8] §f";
                } else if (ircPlayer1.getRank() == IrcRank.USER) {
                    return "§8[§aUser§8] §f";
                }
            }

        } catch (Exception e) {

        }
        return "";
    }

    public static String getHardwareID() throws NoSuchAlgorithmException {
        String unhashed = String.format("%s-%s-%s-%s-%s", System.getProperty("os.version"),
                System.getProperty("os.name"),
                System.getProperty("user.name"),
                System.getProperty("user.home"),
                System.getProperty("os.arch"));
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.reset();
        digest.update(unhashed.getBytes());
        StringBuilder hash = new StringBuilder(new BigInteger(1, digest.digest()).toString(16));
        while (hash.length() < 32) {
            hash.insert(0, "0");
        }
        return hash.toString();
    }
}