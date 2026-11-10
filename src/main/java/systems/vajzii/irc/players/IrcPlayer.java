package systems.vajzii.irc.players;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.util.Logger;

import java.util.Collection;
import java.util.HashMap;

/**
 * Class to represent the users connected to the IRC. E.g. used for tab-list or
 * chat prefix for IRC users.
 * 
 * @author vajzii
 */

@Getter
@Setter
public class IrcPlayer {

	/**
	 * HashMap with all for the client visible IRC users. Key is the in-game name of
	 * the player. Value is the IRCPlayer object. 
	 * 
	 * @see IrcPlayer
	 */
	private static HashMap<String, IrcPlayer> ingameToIrcPlayer = new HashMap<>();

	private static HashMap<String, IrcPlayer> usernameIrcPlayer = new HashMap<>();

	/**
	 * Get the IRC player object by it's in-game name
	 * 
	 * @param name in-game name of the player
	 * @return IRC player
	 */
	public static IrcPlayer getByIngameName(String name) {
		return ingameToIrcPlayer.get(name);
	}

	/**
	 * List of all IRC players visible
	 * 
	 * @return collection of IRCPlayer objects
	 */
	public static Collection<IrcPlayer> listPlayers() {
		return ingameToIrcPlayer.values();
	}

	/**
	 * Remove a IRC player from the list if it disconnects or hides.
	 * 
	 * @param ingameName in-game name of the player
	 */
	public static void removeIrcPlayer(String ingameName) {
		ingameToIrcPlayer.remove(ingameName);
	}


	public static void removeIrcUsername(String username) {
		usernameIrcPlayer.remove(username);
	}


	public static void clear(IrcPlayer player) {
		ingameToIrcPlayer.remove(player.getIngameName());
		usernameIrcPlayer.remove(player.getIrcNick());
	}


	public static IrcPlayer getPlayerByIRCUsername(String name) {
		return usernameIrcPlayer.get(name);
	}

	/**
	 * The nickname of the player in the IRC.
	 */
	private String ircNick;

	/**
	 * The Minecraft in-game name of the player.
	 */
	private String ingameName;

	/**
	 * The ip of the current Minecraft-Server the user is connected to. Empty if in main menu.
	 */
	private String serverIp;
	
	/**
	 * The rank of the player in the IRC.
	 */
	private IrcRank rank;


	private String type;
	
	/**
	 * Whether this player is vanished (invisible to others)
	 */
	private boolean vanished = false;

	/**
	 * New IRC player will be added to the ingameToIrcPlayer HashMap
	 *
	 * @param ircNick    IRC nickname of the player
	 * @param ingameName Minecraft in-game name of the player
	 * @param rank       IRC rank of the player
	 */
	public IrcPlayer(String ircNick, String ingameName, String serverIp, IrcRank rank, String type) {
		this.ingameName = ingameName;
		this.ircNick = ircNick;
		this.serverIp = serverIp;
		this.rank = rank;
		this.type = type;
		
		ingameToIrcPlayer.put(ingameName, this);
		usernameIrcPlayer.put(ircNick, this);
	}

	/**
	 * @return in-game name of the player
	 */
	public String getIngameName() {
		return ingameName;
	}

	/**
	 * @return IRC nickname of the player
	 */
	public String getIrcNick() {
		return ircNick;
	}
	
	/**
	 * @return The Minecraft-Server IP the user is currently connected to.
	 */
	public String getServerIp() {
		return ircNick;
	}

	/**
	 * @return IRC rank of the player
	 */
	public IrcRank getRank() {
		return rank;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
