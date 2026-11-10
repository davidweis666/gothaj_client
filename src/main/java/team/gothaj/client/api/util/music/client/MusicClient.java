package team.gothaj.client.api.util.music.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.gothaj.client.api.util.music.request.SoundCloudDownloadResponse;
import team.gothaj.client.api.util.music.request.SoundCloudResponse;

import java.net.URLEncoder;
import java.net.http.*;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;

import java.nio.charset.StandardCharsets;

public class MusicClient {
    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public MusicClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder().build();
        this.mapper = new ObjectMapper();
    }

    public SoundCloudResponse searchSoundCloud(String query) throws Exception {
        String url = baseUrl + "/sc/search?q=" + encode(query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Error: " + response.statusCode() + " - " + response.body());
        }

        return mapper.readValue(response.body(), SoundCloudResponse.class);
    }

    // Fixed: Removed trackId parameter
    public SoundCloudDownloadResponse downloadSoundCloudM3u8(String m3u8Url, String thumbnailUrl) throws Exception {
        String url = baseUrl + "/sc/download?m3u8_url=" + URLEncoder.encode(m3u8Url, StandardCharsets.UTF_8) +
                "&thumbnail=" + URLEncoder.encode(thumbnailUrl != null ? thumbnailUrl : "", StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP Error: " + response.statusCode() + response.body());
        }

        return mapper.readValue(response.body(), SoundCloudDownloadResponse.class);
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
