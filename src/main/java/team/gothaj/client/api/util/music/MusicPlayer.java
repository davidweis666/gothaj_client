package team.gothaj.client.api.util.music;

import lombok.Getter;
import team.gothaj.client.api.util.music.client.MusicClient;
import team.gothaj.client.api.util.music.request.DownloadInfo;
import team.gothaj.client.api.util.music.request.SoundCloudDownloadResponse;
import team.gothaj.client.api.util.music.request.SoundCloudResponse;
import team.gothaj.client.api.util.music.request.SoundCloudTrack;

import javazoom.jl.player.Player;
import team.gothaj.client.api.util.music.client.volume.MusicVolume;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class MusicPlayer {
    private static final MusicClient api = new MusicClient("http://130.61.249.13:3501");

    private final String mp3Url;
    @Getter
    private final String trackTitle;
    private MusicVolume volumeDevice;
    private Player player;
    @Getter
    private Thread playThread;
    @Getter
    private boolean isPlaying = false;

    private MusicPlayer(String mp3Url, String trackTitle) {
        this.mp3Url = mp3Url;
        this.trackTitle = trackTitle;
    }

    /**
     * Search for SoundCloud tracks
     * @param query Search query
     * @return List of up to 6 tracks
     * @throws Exception if search fails
     */
    public static List<SoundCloudTrack> searchTracks(String query) throws Exception {
        SoundCloudResponse search = api.searchSoundCloud(query);
        if (search.getCode() != 1 || search.getMsg().isEmpty()) {
            throw new Exception("No tracks found for: " + query);
        }

        List<SoundCloudTrack> tracks = search.getMsg();
        return tracks.subList(0, Math.min(6, tracks.size()));
    }

    /**
     * Download and create player for a specific track
     * @param track SoundCloud track to download
     * @return MusicPlayer ready to play
     * @throws Exception if download fails
     */
    public static MusicPlayer downloadTrack(SoundCloudTrack track) throws Exception {
        // Check if track has M3U8 URL
        if (track.getUrl() == null) {
            throw new Exception("Track not available for streaming: " + track.getTitle());
        }

        SoundCloudDownloadResponse download = api.downloadSoundCloudM3u8(
                track.getUrl(),
                track.getThumbnail()
        );

        if (download.getCode() != 1) {
            throw new Exception("Download failed for: " + track.getTitle());
        }

        DownloadInfo info = download.getMsg();
        return new MusicPlayer(info.getUrl(), track.getTitle());
    }

    // Playback Controls
    public void play() {
        if (isPlaying) return;

        playThread = new Thread(() -> {
            try {
                InputStream inputStream = new URL(mp3Url).openStream();
                volumeDevice = new MusicVolume();
                player = new Player(inputStream, volumeDevice);

                volumeDevice.setVolume(0.5f);

                isPlaying = true;
                player.play();

                // Cleanup
                player.close();
                inputStream.close();
                isPlaying = false;

            } catch (Exception e) {
                isPlaying = false;
            }
        });

        playThread.setDaemon(true);
        playThread.start();
    }

    public void stop() {
        if (player != null) {
            player.close();
        }
        isPlaying = false;
    }

    public void setVolume(float volume) {
        if (volumeDevice != null) {
            volumeDevice.setVolume(volume);
        }
    }

    public float getVolume() {
        return volumeDevice != null ? volumeDevice.getVolume() : 0.0f;
    }

}
