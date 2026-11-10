package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import systems.vajzii.irc.party.PartyManager;
import team.gothaj.client.api.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client only: full roster sync for a party. Sent to every member
 * whenever the party's membership or leader changes. `disbanded` (or an
 * empty `partyId`) means the recipient is no longer in a party.
 */
public class Irc30PartyUpdate extends IrcPacket {

    public String partyId = "";
    public String leaderNick = "";
    public boolean disbanded = false;
    public List<String> memberNicks = new ArrayList<>();

    @Override
    public void read(DataInputStream in) throws IOException {
        this.partyId = in.readUTF();
        this.leaderNick = in.readUTF();
        this.disbanded = in.readBoolean();

        int count = in.readInt();
        memberNicks = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            memberNicks.add(in.readUTF());
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(partyId != null ? partyId : "");
        out.writeUTF(leaderNick != null ? leaderNick : "");
        out.writeBoolean(disbanded);

        out.writeInt(memberNicks.size());
        for (String nick : memberNicks) {
            out.writeUTF(nick != null ? nick : "");
        }
    }

    @Override
    public void handle(IrcClient client) {
        if (this.disbanded || this.partyId == null || this.partyId.isEmpty()) {
            // The server always sends an explanatory chat line (left/kicked/disbanded)
            // alongside this transition, so stay silent here to avoid duplicating it.
            PartyManager.clear();
            return;
        }

        String selfNick = client.getIrcNick();
        List<String> previousMembers = PartyManager.getMemberNicks();

        for (String member : this.memberNicks) {
            if (!previousMembers.contains(member) && (selfNick == null || !selfNick.equalsIgnoreCase(member))) {
                Logger.ircChat("§e" + member + " §7joined the party.");
            }
        }
        for (String member : previousMembers) {
            if (!this.memberNicks.contains(member) && (selfNick == null || !selfNick.equalsIgnoreCase(member))) {
                Logger.ircChat("§e" + member + " §7left the party.");
            }
        }

        PartyManager.update(this.partyId, this.leaderNick, this.memberNicks);
    }
}
