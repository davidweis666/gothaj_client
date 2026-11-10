package team.gothaj.client.api.friend;

import com.google.gson.*;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class FriendManager extends ListManager<Friend> {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File friendDir = new File(GothajClient.getInstance().getClientDir() + File.separator + "friends");

    @Override
    public void init() {
        if (!friendDir.exists()) friendDir.mkdirs();
        load();
        super.init();
    }

    public void save() {
        if (!friendDir.exists()) friendDir.mkdirs();

        var root = new JsonObject();
        var friendsObj = new JsonObject();
        for (var friend : getItems()) {
            friendsObj.add(friend.getName(), new JsonObject());
        }
        root.add("friends", friendsObj);

        Path file = Paths.get(friendDir.getAbsolutePath(), "friends.json");
        try (var w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(root, w);
        } catch (Exception e) {
            Logger.clientChat("save error: " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path file = Paths.get(friendDir.getAbsolutePath(), "friends.json");
            if (!Files.exists(file)) return;

            JsonObject root;
            try (var r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(r).getAsJsonObject();
            }
            if (root == null || !root.has("friends")) return;

            var friendObj = root.getAsJsonObject("friends");
            getItems().clear();
            for (var entry : friendObj.entrySet()) {
                var name = entry.getKey();
                try {
                    getItems().add(new Friend(name));
                } catch (Exception ex) {
                    Logger.clientChat("Failed to add friend " + name + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.clientChat("load error: " + e.getMessage());
        }
    }

    public void addFriend(String friend) {
        if (isFriend(friend)) return;
        getItems().add(new Friend(friend));
        GothajClient.getInstance().getNotificationManager().add("Friends", friend + " is now your friend!", Notification.Type.INFO);
    }

    public void removeFriend(String friend) {
        getItems().remove(new Friend(friend));
        GothajClient.getInstance().getNotificationManager().add("Friends", friend + " is no longer your friend!", Notification.Type.INFO);
    }

    public boolean isFriend(String friend) {
        return getItems().contains(new Friend(friend));
    }
}
