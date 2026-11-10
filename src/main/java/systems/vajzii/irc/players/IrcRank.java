package systems.vajzii.irc.players;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The ranks in the IRC.
 * 
 * @author vajzii
 */
@Getter
@AllArgsConstructor
public enum IrcRank {

    USER("User", 0, 0, ""),
    VIP("VIP", 1, 10, "§6"),
    MEDIA("Media", 2, 10, "§b"),
    MOD("Moderator", 3, 20, "§5"),
    DEV("Developer", 4, 30, "§2"),
    ADMIN("Administrator", 5, 30, "§4");

    /**
     * Get the IRC rank by its id.
     *
     * @param id id of the rank
     * @return the IRC rank
     */
    public static IrcRank getById(int id) {
        for (IrcRank rank : IrcRank.values())
            if (rank.getId() == id)
                return rank;
        return IrcRank.USER;
    }

    /**
     * Full name of the rank
     */
    private final String name;
    private final int id;
    private final int permissionLevel;
    private final String chatColor;
}

