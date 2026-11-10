package team.gothaj.client.api.util.music.spotify;

import com.sun.net.httpserver.HttpServer;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import net.minecraft.util.EnumChatFormatting;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;
import se.michaelthelin.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.module.client.MediaPlayer;
import team.gothaj.client.main.GothajClient;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

interface SpotifyCallback {
    void codeCallback(final String code);
}

public class SpotifyAPI {

    public final SpotifyApi SPOTIFY = new SpotifyApi.Builder()
            .setClientId(GothajClient.getInstance().getModuleManager().getAndCast(MediaPlayer.class).clientId.getProperty())
            .setRedirectUri(SpotifyHttpManager.makeUri("http://127.0.0.1:1337"))
            .build();

    private final String CODE_VERIFIER = randomString();
    public final String CODE_CHALLENGE = getChallengeHash(CODE_VERIFIER);
    public AuthorizationCodeUriRequest AUTH_CODE_URI = SPOTIFY.authorizationCodePKCEUri(CODE_CHALLENGE)
            .scope("user-read-playback-state user-read-playback-position user-modify-playback-state user-read-currently-playing")
            .build();
    public Track currentTrack;
    public CurrentlyPlayingContext currentPlayingContext;
    public boolean authenticated;
    private HttpServer callbackServer;
    private int tokenRefreshInterval = 2;

    private final SpotifyCallback callback = code -> {
        Logger.spotifyChat("Connecting...");
        AuthorizationCodePKCERequest authCodePKCERequest = SPOTIFY.authorizationCodePKCE(code, CODE_VERIFIER).build();
        try {
            final AuthorizationCodeCredentials authCredentials = authCodePKCERequest.execute();
            SPOTIFY.setAccessToken(authCredentials.getAccessToken());
            SPOTIFY.setRefreshToken(authCredentials.getRefreshToken());
            tokenRefreshInterval = authCredentials.getExpiresIn();
            authenticated = true;
            Logger.spotifyChat("Connected");

            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.SECONDS.sleep(Math.max(5, tokenRefreshInterval - 2));
                        final AuthorizationCodeCredentials refreshRequest = SPOTIFY.authorizationCodePKCERefresh().build().execute();
                        SPOTIFY.setAccessToken(refreshRequest.getAccessToken());
                        if (refreshRequest.getRefreshToken() != null && !refreshRequest.getRefreshToken().isEmpty()) {
                            SPOTIFY.setRefreshToken(refreshRequest.getRefreshToken());
                        }
                        tokenRefreshInterval = refreshRequest.getExpiresIn();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        try { TimeUnit.SECONDS.sleep(5); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    }
                }
            }, "Spotify-TokenRefresh").start();

            new Thread(() -> {
                long nextAllowedAt = 0L;
                CurrentlyPlayingContext lastCtx = null;
                String lastTrackId = null;

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        long now = System.currentTimeMillis();
                        if (now < nextAllowedAt) {
                            TimeUnit.MILLISECONDS.sleep(Math.max(50L, nextAllowedAt - now));
                            continue;
                        }

                        GetInformationAboutUsersCurrentPlaybackRequest getCurrentPlaybackInfo =
                                SPOTIFY.getInformationAboutUsersCurrentPlayback().build();

                        CurrentlyPlayingContext ctx;
                        try {
                            ctx = getCurrentPlaybackInfo.execute();
                        } catch (TooManyRequestsException rate) {
                            int retry = rate.getRetryAfter() != -1 ? rate.getRetryAfter() : 2;
                            nextAllowedAt = System.currentTimeMillis() + retry * 1000L;
                            continue;
                        }

                        if (ctx == null || ctx.getItem() == null) {
                            currentPlayingContext = null;
                            currentTrack = null;
                            lastCtx = null;
                            lastTrackId = null;
                            TimeUnit.MILLISECONDS.sleep(500L);
                            continue;
                        }

                        String trackId = ctx.getItem().getId();
                        boolean changed = lastTrackId == null || !lastTrackId.equals(trackId)
                                || (lastCtx == null)
                                || (ctx.getIs_playing() != null && lastCtx.getIs_playing() != null
                                && !ctx.getIs_playing().equals(lastCtx.getIs_playing()));

                        if (changed) {
                            GetTrackRequest getTrackRequest = SPOTIFY.getTrack(trackId).build();
                            currentTrack = getTrackRequest.execute();
                            currentPlayingContext = ctx;
                            lastCtx = ctx;
                            lastTrackId = trackId;
                        } else {
                            currentPlayingContext = ctx;
                        }

                        TimeUnit.MILLISECONDS.sleep(500L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (SpotifyWebApiException apiEx) {
                        nextAllowedAt = System.currentTimeMillis() + 1000L;
                    } catch (Exception ignored) {
                        nextAllowedAt = System.currentTimeMillis() + 500L;
                    }
                }
            }, "Spotify-Playback").start();

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    };

    private String getChallengeHash(String codeVerifier) {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().encodeToString(digest.digest(bytes)).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private String randomString() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 43; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public void init() {
        if (!authenticated) {
            if (GothajClient.getInstance().getModuleManager().getAndCast(MediaPlayer.class).clientId.getProperty().isEmpty()) {
                GothajClient.getInstance().getNotificationManager().add("Spotify", "Please fill in Client ID in Spotify Module", Notification.Type.WARNING);
                return;
            }
            try {
                Desktop.getDesktop().browse(AUTH_CODE_URI.execute());
                GothajClient.scheduledExecutorService.submit(() -> {
                    try {
                        if (callbackServer != null) {
                            callbackServer.stop(0);
                        }
                        Logger.spotifyChat("Waiting for Spotify confirmation...");
                        callbackServer = HttpServer.create(new InetSocketAddress(1337), 0);
                        callbackServer.createContext("/", context -> {
                            try {
                                String query = context.getRequestURI().getQuery();
                                String code = null;
                                if (query != null) {
                                    String[] parts = query.split("&");
                                    for (String p : parts) {
                                        if (p.startsWith("code=")) {
                                            code = p.substring("code=".length());
                                            break;
                                        }
                                    }
                                }
                                if (code != null) {
                                    callback.codeCallback(code);
                                }
                                final String infoMessage = (code != null)
                                        ? "<h1>Successfully authorized.\nYou can now close this window, have fun!</h1>"
                                        : "<h1>Unable to Authorize client, re-toggle the module.</h1>";
                                byte[] body = infoMessage.getBytes(StandardCharsets.UTF_8);
                                context.sendResponseHeaders(200, body.length);
                                try (OutputStream out = context.getResponseBody()) {
                                    out.write(body);
                                }
                            } finally {
                                callbackServer.stop(0);
                            }
                        });
                        callbackServer.start();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void skipToPreviousTrack() {
        try {
            SPOTIFY.skipUsersPlaybackToPreviousTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            Logger.spotifyChat(EnumChatFormatting.RED + e.getMessage());
        }
    }

    public void skipTrack() {
        try {
            SPOTIFY.skipUsersPlaybackToNextTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            Logger.spotifyChat(EnumChatFormatting.RED + e.getMessage());
        }
    }

    public void toggleShuffleState() {
        try {
            boolean current = currentPlayingContext != null
                    && currentPlayingContext.getShuffle_state() != null
                    && currentPlayingContext.getShuffle_state();
            SPOTIFY.toggleShuffleForUsersPlayback(!current).build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            Logger.spotifyChat(EnumChatFormatting.RED + e.getMessage());
        }
    }

    public void pausePlayback() {
        try {
            SPOTIFY.pauseUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            Logger.spotifyChat(EnumChatFormatting.RED + e.getMessage());
        }
    }

    public void resumePlayback() {
        try {
            SPOTIFY.startResumeUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            Logger.spotifyChat(EnumChatFormatting.RED + e.getMessage());
        }
    }

    public boolean isPlaying() {
        return currentPlayingContext != null
                && currentPlayingContext.getIs_playing() != null
                && currentPlayingContext.getIs_playing();
    }

    public List<Track> searchTracks(String query) throws IOException, ParseException, SpotifyWebApiException {
        SearchTracksRequest tracksRequest = SPOTIFY.searchTracks(query).limit(6).build();
        List<Track> tracks = new ArrayList<>();
        Paging<Track> trackPaging = tracksRequest.execute();
        if (trackPaging.getItems() != null) {
            tracks.addAll(Arrays.asList(trackPaging.getItems()));
        }
        return tracks;
    }
}
