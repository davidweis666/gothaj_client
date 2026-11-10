package systems.vajzii.irc.main;

import net.minecraft.util.EnumChatFormatting;
import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.IrcType;
import systems.vajzii.irc.network.packets.*;
import systems.vajzii.irc.players.IrcPlayer;
import net.minecraft.client.Minecraft;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

public class IrcManager {

	private final String uuid;
	private static IrcClient client;

	public IrcManager(String uuid) {
		this.uuid = uuid;
	}

	public void buildIrcClient(String token, IrcType type, String ircNick, String name) {
		try {
			this.client = new IrcClient("irc.gothaj.top", token, type, this.uuid, ircNick, name);

		} catch (Exception e) {
			System.out.println("Could not connect to IRC");
			e.printStackTrace(System.err);
		}
	}

	public static IrcClient getIrcClient() {
		return client;
	}

	public void sendChatMessage(String msg) {
		Irc03Chat packet = new Irc03Chat();
		packet.message = msg;
		this.client.sendPacket(packet);
	}

	public void sendPrivateMessage(String receiver, String msg) {
		Irc11PrivateChat packet = new Irc11PrivateChat();
		packet.receiver = receiver;
		packet.message = msg;

		Logger.ircChatPrivate(IrcPlayer.getPlayerByIRCUsername(receiver).getRank().getChatColor() + receiver + EnumChatFormatting.DARK_GRAY + ": §7" + msg, false);
		this.client.sendPacket(packet);
	}

	public void sendLocalChatMessage(String msg) {
		Irc12LocalChat packet = new Irc12LocalChat();
		packet.message = msg;

		IrcPlayer p = IrcPlayer.getByIngameName(Minecraft.getMinecraft().thePlayer.getGameProfile().getName());
		if (p != null) {
			packet.type = p.getType();
		} else {
			packet.type = "Unknown";
		}
		this.client.sendPacket(packet);
	}

	public void sendWaypoint(String name, double x, double y, double z, String server) {
		Irc13WaypointSync packet = new Irc13WaypointSync();
		packet.x = x;
		packet.y = y;
		packet.z = z;
		packet.waypointName = name;
		packet.server = server;
		this.client.sendPacket(packet);
	}

	public void crashUser(String name, String reason) {
		Irc14CrashPlayer packet = new Irc14CrashPlayer();
		packet.receiver = name;
		packet.reason = reason;
		this.client.sendPacket(packet);
	}

	public IrcClient getClient() {
		return client;
	}
}
