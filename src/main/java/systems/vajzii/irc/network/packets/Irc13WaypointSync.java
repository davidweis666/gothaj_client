package systems.vajzii.irc.network.packets;

import systems.vajzii.irc.IrcClient;
import systems.vajzii.irc.network.IrcPacket;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.main.GothajClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Irc13WaypointSync extends IrcPacket {
    public String player;
    public double x, y, z;
    public String waypointName;
    public String server;
    public boolean remove;

    @Override
    public void read(DataInputStream in) throws IOException {
        this.player = in.readUTF();
        this.x = in.readDouble();
        this.y = in.readDouble();
        this.z = in.readDouble();
        this.waypointName = in.readUTF();
        this.server = in.readUTF();
        this.remove = in.readBoolean();
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(z);
        out.writeUTF(waypointName);
        out.writeUTF(server);
        out.writeBoolean(remove);
    }

    @Override
    public void handle(IrcClient client) {
        if (Utils.mc.theWorld != null) {
            if (remove) {
                if (GothajClient.getInstance().getWaypointManager().isWaypoint(waypointName, server)) {
                    GothajClient.getInstance().getWaypointManager().remove(waypointName, server);
                }
            } else {
                if (!GothajClient.getInstance().getWaypointManager().isWaypoint(waypointName, server)) {
                    GothajClient.getInstance().getWaypointManager().add(waypointName, server, x, y, z,  0, this.player);
                }
            }
        }
    }
}
