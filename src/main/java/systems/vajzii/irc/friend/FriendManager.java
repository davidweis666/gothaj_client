package systems.vajzii.irc.friend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side cache of the local player's friend list and pending incoming
 * requests, kept in sync via Irc27FriendList. Server-authoritative - this
 * only mirrors whatever the server last sent.
 *
 * @author vajzii
 */
public class FriendManager {

    public record FriendEntry(String nickname, boolean online, String ingameName, String serverIp) {
    }

    private static volatile List<FriendEntry> friends = new ArrayList<>();
    private static volatile List<String> incomingRequests = new ArrayList<>();

    private FriendManager() {
    }

    public static synchronized void update(List<FriendEntry> newFriends, List<String> newIncomingRequests) {
        friends = newFriends != null ? new ArrayList<>(newFriends) : new ArrayList<>();
        incomingRequests = newIncomingRequests != null ? new ArrayList<>(newIncomingRequests) : new ArrayList<>();
    }

    /**
     * Optimistically records an incoming request between full syncs (the
     * authoritative Irc27FriendList that follows will supersede this).
     */
    public static synchronized void addIncomingRequest(String fromNick) {
        if (fromNick == null || fromNick.isEmpty() || incomingRequests.contains(fromNick)) return;
        List<String> updated = new ArrayList<>(incomingRequests);
        updated.add(fromNick);
        incomingRequests = updated;
    }

    public static List<FriendEntry> getFriends() {
        return Collections.unmodifiableList(friends);
    }

    public static List<String> getIncomingRequests() {
        return Collections.unmodifiableList(incomingRequests);
    }

    public static boolean isFriend(String nickname) {
        if (nickname == null) return false;
        for (FriendEntry f : friends) {
            if (nickname.equalsIgnoreCase(f.nickname)) return true;
        }
        return false;
    }

    public static synchronized void clear() {
        friends = new ArrayList<>();
        incomingRequests = new ArrayList<>();
    }
}
