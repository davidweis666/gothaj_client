package team.gothaj.client.api.util.user;


import lombok.Getter;

@Getter
public class User {
    private final String username;
    private final String uid;
    private final String uidFormatted;

    public User(String username, String uid) {
        this.username = username;
        this.uid = uid;
        this.uidFormatted = String.format("%04d", Integer.parseInt(uid));
    }

    public String getFormattedUser() { return username + "#" + uidFormatted; }

    @Override
    public String toString() { return getFormattedUser();
    }
}
