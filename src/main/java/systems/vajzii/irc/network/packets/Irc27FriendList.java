package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.friend.FriendManager;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client only: full sync of the client's friend list (with online
 * status) plus any pending incoming friend requests. Sent on login and
 * whenever the list changes (accept/remove/online status change) or on
 * request via ".irc friend list".
 */
public class Irc27FriendList extends IrcPacket {

    public static class Entry {
        public String nickname;
        public boolean online;
        public String ingameName;
        public String serverIp;
    }

    public List<Entry> friends = new ArrayList<>();
    public List<String> incomingRequests = new ArrayList<>();

    @Override
    public void read(DataInputStream in) throws IOException {
        int friendCount = in.readInt();
        friends = new ArrayList<>(Math.max(0, friendCount));
        for (int i = 0; i < friendCount; i++) {
            Entry entry = new Entry();
            entry.nickname = in.readUTF();
            entry.online = in.readBoolean();
            entry.ingameName = in.readUTF();
            entry.serverIp = in.readUTF();
            friends.add(entry);
        }

        int requestCount = in.readInt();
        incomingRequests = new ArrayList<>(Math.max(0, requestCount));
        for (int i = 0; i < requestCount; i++) {
            incomingRequests.add(in.readUTF());
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(friends.size());
        for (Entry entry : friends) {
            out.writeUTF(entry.nickname != null ? entry.nickname : "");
            out.writeBoolean(entry.online);
            out.writeUTF(entry.ingameName != null ? entry.ingameName : "");
            out.writeUTF(entry.serverIp != null ? entry.serverIp : "");
        }

        out.writeInt(incomingRequests.size());
        for (String nick : incomingRequests) {
            out.writeUTF(nick != null ? nick : "");
        }
    }

    @Override
    public void handle(IrcClient client) {
        List<FriendManager.FriendEntry> entries = new ArrayList<>(friends.size());
        for (Entry e : friends) {
            entries.add(new FriendManager.FriendEntry(e.nickname, e.online, e.ingameName, e.serverIp));
        }
        FriendManager.update(entries, incomingRequests);
    }
}
