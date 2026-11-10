package team.gothaj.client.api.util.music.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import team.gothaj.client.api.util.music.request.SoundCloudTrack;
import team.gothaj.client.main.GothajClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistManager {
    private static final String PLAYLISTS_DIR = GothajClient.getInstance().getClientDir() + "/playlists/";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, Playlist>> userPlaylistsCache = new HashMap<>();

    public PlaylistManager() {
        createPlaylistsDirectory();
    }

    private void createPlaylistsDirectory() {
        try {
            Files.createDirectories(Paths.get(PLAYLISTS_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create playlists directory: " + e.getMessage());
        }
    }

    private String getUserPlaylistsFile(String uid) {
        return PLAYLISTS_DIR + "user_" + uid + "_playlists.json";
    }

    public Map<String, Playlist> loadUserPlaylists(String uid) {
        if (userPlaylistsCache.containsKey(uid)) {
            return userPlaylistsCache.get(uid);
        }

        File file = new File(getUserPlaylistsFile(uid));
        if (!file.exists()) {
            Map<String, Playlist> emptyPlaylists = new HashMap<>();
            userPlaylistsCache.put(uid, emptyPlaylists);
            return emptyPlaylists;
        }

        try {
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            Map<String, Playlist> playlists = objectMapper.readValue(file,
                    typeFactory.constructMapType(HashMap.class, String.class, Playlist.class));
            
            if (playlists == null) playlists = new HashMap<>();
            userPlaylistsCache.put(uid, playlists);
            return playlists;
        } catch (IOException e) {
            System.err.println("Failed to load playlists for user " + uid + ": " + e.getMessage());
            Map<String, Playlist> emptyPlaylists = new HashMap<>();
            userPlaylistsCache.put(uid, emptyPlaylists);
            return emptyPlaylists;
        }
    }

    private boolean saveUserPlaylists(String uid) {
        if (!userPlaylistsCache.containsKey(uid)) {
            return false;
        }

        try {
            Path filePath = Paths.get(getUserPlaylistsFile(uid));
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(filePath.toFile(), userPlaylistsCache.get(uid));
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save playlists for user " + uid + ": " + e.getMessage());
            return false;
        }
    }

    public Playlist createPlaylist(String uid, String name, String description) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        
        Playlist newPlaylist = new Playlist(name, uid);
        newPlaylist.setDescription(description);
        
        playlists.put(newPlaylist.getId(), newPlaylist);
        userPlaylistsCache.put(uid, playlists);
        
        saveUserPlaylists(uid);
        return newPlaylist;
    }

    public List<Playlist> getUserPlaylists(String uid) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        return new ArrayList<>(playlists.values());
    }

    public Playlist getPlaylist(String uid, String playlistId) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        return playlists.get(playlistId);
    }

    public boolean updatePlaylist(String uid, String playlistId, String name, String description, Boolean isPublic) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        Playlist playlist = playlists.get(playlistId);
        
        if (playlist == null) return false;
        
        playlist.updateMetadata(name, description, isPublic);
        return saveUserPlaylists(uid);
    }

    public boolean deletePlaylist(String uid, String playlistId) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        boolean removed = playlists.remove(playlistId) != null;
        
        if (removed) {
            return saveUserPlaylists(uid);
        }
        return false;
    }

    public boolean addTrackToPlaylist(String uid, String playlistId, SoundCloudTrack track) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        Playlist playlist = playlists.get(playlistId);
        
        if (playlist == null) return false;
        
        if (playlist.containsTrack(track.getUrl())) {
            return false; // Track already exists
        }
        
        playlist.addTrack(track);
        return saveUserPlaylists(uid);
    }

    public boolean removeTrackFromPlaylist(String uid, String playlistId, String trackUrl) {
        Map<String, Playlist> playlists = loadUserPlaylists(uid);
        Playlist playlist = playlists.get(playlistId);
        
        if (playlist == null) return false;
        
        boolean removed = playlist.removeTrack(trackUrl);
        if (removed) {
            return saveUserPlaylists(uid);
        }
        return false;
    }

    public void clearUserCache(String uid) {
        userPlaylistsCache.remove(uid);
    }

    public void clearAllCache() {
        userPlaylistsCache.clear();
    }
}
