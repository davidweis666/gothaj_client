// Playlist.java
package team.gothaj.client.api.util.music.playlist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.util.music.request.SoundCloudTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Playlist {
    private String id;
    private String name;
    private String description = "";
    private String ownerUid;
    private boolean isPublic = false;
    private long createdAt;
    private long updatedAt;
    private List<SoundCloudTrack> tracks = new ArrayList<>();

    public Playlist() {
        // For JSON deserialization
    }

    public Playlist(String name, String ownerUid) {
        this.id = "pl_" + UUID.randomUUID().toString().substring(0, 12);
        this.name = name;
        this.ownerUid = ownerUid;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public void addTrack(SoundCloudTrack track) {
        if (!containsTrack(track.getUrl())) {
            tracks.add(track);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public boolean removeTrack(String trackUrl) {
        boolean removed = tracks.removeIf(track -> track.getUrl().equals(trackUrl));
        if (removed) {
            this.updatedAt = System.currentTimeMillis();
        }
        return removed;
    }

    public boolean containsTrack(String trackUrl) {
        return tracks.stream().anyMatch(track -> track.getUrl().equals(trackUrl));
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public void updateMetadata(String name, String description, Boolean isPublic) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (isPublic != null) this.isPublic = isPublic;
        this.updatedAt = System.currentTimeMillis();
    }
}
