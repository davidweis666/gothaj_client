package systems.vajzii.irc.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side cache of the local player's current party, kept in sync via
 * Irc30PartyUpdate. Server-authoritative - this only mirrors whatever the
 * server last sent.
 *
 * @author vajzii
 */
public class PartyManager {

    private static volatile String partyId = "";
    private static volatile String leaderNick = "";
    private static volatile List<String> memberNicks = new ArrayList<>();

    private PartyManager() {
    }

    public static boolean isInParty() {
        return !partyId.isEmpty();
    }

    public static String getPartyId() {
        return partyId;
    }

    public static String getLeaderNick() {
        return leaderNick;
    }

    public static List<String> getMemberNicks() {
        return Collections.unmodifiableList(memberNicks);
    }

    public static boolean isLeader(String ownNick) {
        return isInParty() && ownNick != null && leaderNick.equalsIgnoreCase(ownNick);
    }

    public static synchronized void update(String newPartyId, String newLeaderNick, List<String> newMembers) {
        partyId = newPartyId != null ? newPartyId : "";
        leaderNick = newLeaderNick != null ? newLeaderNick : "";
        memberNicks = newMembers != null ? new ArrayList<>(newMembers) : new ArrayList<>();
    }

    public static synchronized void clear() {
        partyId = "";
        leaderNick = "";
        memberNicks = new ArrayList<>();
    }
}
