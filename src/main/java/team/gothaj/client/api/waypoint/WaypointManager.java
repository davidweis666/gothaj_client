package team.gothaj.client.api.waypoint;

import com.google.gson.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.main.GothajClient;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class WaypointManager extends ListManager<Waypoint> {


    @Override
    public void init() {
        GothajClient.getInstance().getEventBus().subscribe(this);
    }

	private Waypoint getWaypoint(String label, String server) {
		for (Waypoint waypoint : getItems()) {
			if (waypoint.getLabel().equalsIgnoreCase(label) && waypoint.getServer().equals(server)) {
				return waypoint;
			}
		}
		return null;
	}


    public void add(String label, String server, double x, double y, double z, int dimension, String creator) {
		getItems().add(new Waypoint(label,  server, x, y, z, dimension, creator));
        GothajClient.getInstance().getIrcManager().sendWaypoint(label, x, y, z, server);
	}


	public void remove(String label, String server) {
		Waypoint f = getWaypoint(label, server);
		if (f != null) {
			getItems().remove(f);
		}
	}

	public boolean isWaypoint(String ign, String server) {
		return getWaypoint(ign, server) != null;
	}
}
