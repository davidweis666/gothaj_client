package systems.vajzii.irc.network;

import java.util.ArrayList;

import systems.vajzii.irc.network.packets.*;

/**
 * Class for managing the packets.
 * 
 * @author vajzii
 */
public class IrcPacketManager {

	/**
	 * List of all packets that can be sent or received. Be aware that the packet id
	 * is based on the order which they were added to this list. Therefore this
	 * order should be the same in both client and server.
	 */
	public final ArrayList<Class<? extends IrcPacket>> packets = new ArrayList<Class<? extends IrcPacket>>();
	{
		packets.add(Irc00Login.class);
		packets.add(Irc01KeepAlive.class);
		packets.add(Irc02Info.class);
		packets.add(Irc03Chat.class);
		packets.add(Irc04AddPlayer.class);
		packets.add(Irc05RemovePlayer.class);
		packets.add(Irc06ChangeIngameName.class);
		packets.add(Irc07ClientCommand.class);
		packets.add(Irc08AdminInfo.class);
		packets.add(Irc09JoinServer.class);
		packets.add(Irc10LeaveServer.class);
		packets.add(Irc11PrivateChat.class);
		packets.add(Irc12LocalChat.class);
		packets.add(Irc13WaypointSync.class);
		packets.add(Irc14CrashPlayer.class);
		packets.add(Irc20EncryptionRequest.class);
		packets.add(Irc21EncryptionResponse.class);
		packets.add(Irc22VanishPlayer.class);
		packets.add(Irc23UnvanishPlayer.class);
		packets.add(Irc24FriendRequest.class);
		packets.add(Irc25FriendResponse.class);
		packets.add(Irc26FriendRemove.class);
		packets.add(Irc27FriendList.class);
		packets.add(Irc28PartyInvite.class);
		packets.add(Irc29PartyResponse.class);
		packets.add(Irc30PartyUpdate.class);
		packets.add(Irc31PartyLeave.class);
		packets.add(Irc32PartyKick.class);
		packets.add(Irc33PartyChat.class);
		packets.add(Irc34PartyDisband.class);
	}

	/**
	 * Get the class of a packet by it's id.
	 * 
	 * @param id id of the packet
	 * @return class of the packet
	 */
	public Class<? extends IrcPacket> getPacketByID(int id) {
		if (id < 0 || id >= packets.size()) return null;
		return this.packets.get(id);
	}

	/**
	 * Get the id of a packet.
	 * 
	 * @param packet the packet
	 * @return id of the packet
	 */
	public int getIDbyPacket(IrcPacket packet) {
		return this.packets.indexOf(packet.getClass());
	}
}