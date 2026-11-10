package team.gothaj.client.api.waypoint;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Waypoint {

    private String label, server;
    private double x, y, z;
    private int dimension;
    private String creator;

}
