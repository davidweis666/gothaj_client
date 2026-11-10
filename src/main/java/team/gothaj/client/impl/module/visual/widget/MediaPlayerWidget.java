package team.gothaj.client.impl.module.visual.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.Track;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.ThumbnailUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.api.util.music.spotify.SpotifyAPI;
import team.gothaj.client.api.util.other.colorthief.ColorPaletteService;
import team.gothaj.client.impl.module.client.MediaPlayer;
import team.gothaj.client.impl.module.client.ShaderProcessing;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.lang.Integer.MIN_VALUE;

public class MediaPlayerWidget extends Widget {
    public static SpotifyAPI spotifyApi;

    private String radioArtist = "";
    private String radioTitle = "";
    private String radioCoverUrl = "";
    private long lastRadioFetch = 0;
    private static final long RADIO_FETCH_INTERVAL = 3000;

    private float titleScrollX = 0;
    private int titleScrollDir = -1;
    private float authorScrollX = 0;
    private int authorScrollDir = -1;
    private String lastPaletteTrackId = null;
    private ColorPaletteService.ColorPalette currentPalette = new ColorPaletteService.ColorPalette(
            new Color(30, 215, 96), new Color(25, 200, 80), new Color(30, 215, 96), new Color(60, 200, 80)
    );

    private final HttpClient httpClient;
    private boolean anchorRight;
    private float anchorOffsetX;
    private boolean horizontalAnchorInitialized;

    public MediaPlayerWidget() {
        super("MediaPlayer", MIN_VALUE, MIN_VALUE, 160, 50, Type.MODULE);
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void onDrag() {
        super.onDrag();
        syncHorizontalAnchor();
    }

    private MediaPlayer getMediaPlayer() {
        return GothajClient.getInstance().getModuleManager().getAndCast(MediaPlayer.class);
    }

    private void syncHorizontalAnchor() {
        ScaledResolution sr = new ScaledResolution(mc);
        float screenWidth = sr.getScaledWidth();

        anchorRight = getX() + getWidth() > screenWidth / 2.0;
        anchorOffsetX = anchorRight ? screenWidth - (getX() + getWidth()) : getX();
        horizontalAnchorInitialized = true;
    }

    private void applyHorizontalAnchor(ScaledResolution sr, float contentWidth) {
        width = contentWidth;

        if (x == MIN_VALUE || y == MIN_VALUE) {
            x = 8;
            y = 8;
        }

        if (!horizontalAnchorInitialized) {
            syncHorizontalAnchor();
        }

        if (isDragging()) {
            syncHorizontalAnchor();
            return;
        }

        if (anchorRight) {
            x = sr.getScaledWidth() - anchorOffsetX - contentWidth;
        } else {
            x = anchorOffsetX;
        }
    }

    @Override
    public void onRender(ScaledResolution sr) {
        boolean initPosition = x == MIN_VALUE || y == MIN_VALUE;
        if (initPosition) {
            x = 8;
            y = 8;
        }
        float xBase = (float) x;
        float yBase = (float) y;

        final Color backgroundColor = new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue());

        boolean isRadioMode = getMediaPlayer().mode.is("Radio");

        if (isRadioMode) {
            fetchRadioData();
            renderRadioUI(sr, xBase, yBase, backgroundColor);
        } else {
            renderSpotifyUI(sr, xBase, yBase, backgroundColor);
        }

        super.onRender(sr);
    }

    public void fetchRadioData() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRadioFetch < RADIO_FETCH_INTERVAL) {
            return;
        }
        lastRadioFetch = currentTime;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://radio.gothaj.top/scrape?url=" + getMediaPlayer().getScrapeURL()))
                    .header("Accept", "application/json")
                    .header("X-API-Key", "bGVkmFT2lwQE6EvijZcdEXmT")
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {

                        if (response.statusCode() == 200) {
                            try {
                                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                                if (json.get("success").getAsBoolean()) {

                                    radioArtist = json.get("artist").getAsString();
                                    radioTitle = json.get("title").getAsString();
                                    radioCoverUrl = json.has("cover_url") && !json.get("cover_url").isJsonNull()
                                            ? json.get("cover_url").getAsString()
                                            : "";

                                }
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        } else {
                            System.err.println("Failed to fetch radio data: " + response.statusCode());
                            System.err.println(response.body());
                        }

                    })
                    .exceptionally(ex -> null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void renderRadioUI(ScaledResolution sr, float xBase, float yBase, Color backgroundColor) {
        if (getMediaPlayer().uiMode.is("Minimal")) {
            renderRadioMinimal(sr, xBase, yBase, backgroundColor);
        } else {
            renderRadioClassic(sr, xBase, yBase, backgroundColor);
        }
    }

    private void renderRadioMinimal(ScaledResolution sr, float xBase, float yBase, Color backgroundColor) {
        final float maximalTitleBoxWidth = 80f;
        final float maximalArtistsBoxWidth = 44f;
        final float minimalBoxHeight = 12f;
        final float minimalThumbSize = 12f;
        final float minimalRadius = 4f;
        final float minimalPadding = 4f;

        if (getMediaPlayer().isToggled() && !radioTitle.isEmpty() && !radioArtist.isEmpty()) {
            float titleTextWidth = Fonts.nunito16.getStringWidth(radioTitle) + 10;
            float titleBoxWidth = Math.min(titleTextWidth, maximalTitleBoxWidth);
            float artistTextWidth = Fonts.nunito14.getStringWidth(radioArtist) + 10;
            float artistBoxWidth = Math.min(artistTextWidth, maximalArtistsBoxWidth);
            float totalWidth = minimalThumbSize + minimalPadding + titleBoxWidth + minimalPadding + artistBoxWidth;

            applyHorizontalAnchor(sr, totalWidth);
            xBase = (float) x;

            if (!radioCoverUrl.isEmpty()) {
                ThumbnailUtil.drawThumbnail(radioCoverUrl, xBase, yBase, minimalThumbSize, minimalThumbSize, minimalRadius, 255);
            }

            float titleX = xBase + minimalThumbSize + minimalPadding;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty())
                RoundedUtil.drawRound(titleX, yBase, titleBoxWidth, minimalBoxHeight, minimalRadius, backgroundColor);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(titleX + 1, yBase + 1, titleX + titleBoxWidth - 1, yBase + minimalBoxHeight - 1);
            int titleTextY = (int) (yBase + (minimalBoxHeight / 2f) - (Fonts.nunito16.getHeight() / 2f));
            drawScrollingTitle(radioTitle, (int) (titleX + 5), titleTextY + 1, (int) (titleBoxWidth - 10), new Color(240, 240, 240).getRGB(), Fonts.nunito16);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            float artistX = titleX + titleBoxWidth + minimalPadding;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty())
                RoundedUtil.drawRound(artistX, yBase, artistBoxWidth, minimalBoxHeight, minimalRadius, backgroundColor);

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(artistX + 1, yBase + 1, artistX + artistBoxWidth - 1, yBase + minimalBoxHeight - 1);
            int artistTextY = (int) (yBase + (minimalBoxHeight / 2f) - (Fonts.nunito14.getHeight() / 2f));
            drawScrollingAuthor(radioArtist, (int) (artistX + 5), artistTextY + 1, (int) (artistBoxWidth - 8), new Color(200, 200, 200).getRGB(), Fonts.nunito14);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            height = (int) minimalThumbSize;
        } else {
            float offw = Fonts.nunito16.getStringWidth("Radio not playing") + 10;
            applyHorizontalAnchor(sr, offw);
            xBase = (float) x;
            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty())
                RoundedUtil.drawRound(xBase, yBase, offw, 12f, 4f, backgroundColor);
            Fonts.nunito16.drawStringWithShadow("Radio not playing", xBase + 5, yBase + Fonts.nunito16.getMiddleOfBox(12f) + 1, new Color(160, 160, 160));
            height = 16;
        }
    }

    private void renderRadioClassic(ScaledResolution sr, float xBase, float yBase, Color backgroundColor) {
        final float fixedHeight = 40f;
        final float radius = 6f;
        final float albumSize = 32f;
        final float contentPadding = 8f;

        height = (int) fixedHeight;

        if (!radioTitle.isEmpty() && !radioArtist.isEmpty()) {
            float titleWidth = Math.min(Fonts.nunito16.getStringWidth(radioTitle), 120);
            float artistWidth = Math.min(Fonts.nunito12.getStringWidth(radioArtist), 100);

            float infoBoxWidth = Math.max(titleWidth, artistWidth) + 16f;
            float maxWidgetWidth = 150f;
            float contentWidth = Math.min(maxWidgetWidth, contentPadding + albumSize + contentPadding + infoBoxWidth);
            applyHorizontalAnchor(sr, contentWidth);
            xBase = (float) x;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(xBase, yBase, (float) width, fixedHeight, radius, backgroundColor);
            }

            float thumbY = yBase + (fixedHeight - albumSize) / 2f;

            if (!radioCoverUrl.isEmpty()) {
                ThumbnailUtil.drawThumbnail(radioCoverUrl, xBase + contentPadding, thumbY, albumSize, albumSize, 6f, 255);
            } else {
                RoundedUtil.drawRound(xBase + contentPadding, thumbY, albumSize, albumSize, 6f, new Color(40, 40, 40));
            }

            float left = xBase + contentPadding + albumSize + contentPadding;

            float boxW = (float) (width - (albumSize + contentPadding * 2));
            float titleMaxWidth = boxW - 8f;

            float titleY = yBase + 10;
            float titleBoxHeight = Fonts.nunito16.getHeight();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(left, titleY - 2, left + titleMaxWidth, titleY + titleBoxHeight + 2);
            drawScrollingTitle(radioTitle, (int) left, (int) (titleY + Fonts.nunito16.getMiddleOfBox(titleBoxHeight)), (int) titleMaxWidth, Color.WHITE.getRGB(), Fonts.nunito16);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            float artistY = titleY + titleBoxHeight + 4;
            float artistBoxHeight = Fonts.nunito12.getHeight();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(left, artistY - 2, left + titleMaxWidth, artistY + artistBoxHeight);
            drawScrollingAuthor(radioArtist, (int) left, (int) (artistY + Fonts.nunito12.getMiddleOfBox(artistBoxHeight)), (int) titleMaxWidth, new Color(180, 180, 180).getRGB(), Fonts.nunito12);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            applyHorizontalAnchor(sr, Fonts.nunito16.getStringWidth("Radio not playing") + 10);
            xBase = (float) x;
            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(xBase, yBase, (float) width, fixedHeight, radius, backgroundColor);
            }
            Fonts.nunito16.drawCenteredStringWithShadow("Radio not playing",
                    (float) (xBase + width / 2f), yBase + fixedHeight / 2f - 5, new Color(140, 140, 140).getRGB());
        }
    }

    private void renderSpotifyUI(ScaledResolution sr, float xBase, float yBase, Color backgroundColor) {
        if (getMediaPlayer().uiMode.is("Minimal")) {
            renderSpotifyMinimal(sr, xBase, yBase, backgroundColor);
        } else {
            renderSpotifyClassic(sr, xBase, yBase, backgroundColor);
        }
    }

    private void renderSpotifyMinimal(ScaledResolution sr, float xBase, float yBase, Color backgroundColor) {
        final float maximalTitleBoxWidth = 80f;
        final float maximalArtistsBoxWidth = 44f;
        final float minimalBoxHeight = 12f;
        final float minimalThumbSize = 12f;
        final float minimalProgressHeight = 6f;
        final float minimalRadius = 4f;
        final float minimalPadding = 4f;

        if (spotifyApi != null && spotifyApi.authenticated && spotifyApi.currentPlayingContext != null && spotifyApi.currentTrack != null) {
            Track currentTrack = spotifyApi.currentTrack;
            CurrentlyPlayingContext playingContext = spotifyApi.currentPlayingContext;

            onSpotifyColorPick(currentTrack);

            int elapsedSec = playingContext.getProgress_ms() != null ? playingContext.getProgress_ms() / 1000 : 0;
            int totalSec = currentTrack.getDurationMs() != null ? currentTrack.getDurationMs() / 1000 : 0;
            float progress = totalSec > 0 ? Math.min(1.0f, (float) elapsedSec / totalSec) : 0f;
            String elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);
            String totalStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);

            String title = currentTrack.getName();
            float titleTextWidth = Fonts.nunito16.getStringWidth(title) + 10;
            float titleBoxWidth = Math.min(titleTextWidth, maximalTitleBoxWidth);

            StringBuilder artists = new StringBuilder();
            for (int a = 0; a < currentTrack.getArtists().length; a++) {
                artists.append(currentTrack.getArtists()[a].getName());
                if (a < currentTrack.getArtists().length - 1) {
                    artists.append(", ");
                }
            }
            String artistsText = artists.toString();
            float artistTextWidth = Fonts.nunito14.getStringWidth(artistsText) + 10;
            float artistBoxWidth = Math.min(artistTextWidth, maximalArtistsBoxWidth);
            float boxWidth = minimalThumbSize + minimalPadding + titleBoxWidth + minimalPadding + artistBoxWidth;

            applyHorizontalAnchor(sr, boxWidth);
            xBase = (float) x;

            ThumbnailUtil.drawThumbnail(currentTrack.getAlbum().getImages()[1].getUrl(), xBase, yBase, minimalThumbSize, minimalThumbSize, minimalRadius, 255);

            float titleX = xBase + minimalThumbSize + minimalPadding;
            float titleBoxCenterY = yBase + (minimalBoxHeight / 2f);
            float artistX = titleX + titleBoxWidth + minimalPadding;
            float artistBoxCenterY = yBase + (minimalBoxHeight / 2f);

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(titleX, yBase, titleBoxWidth, minimalBoxHeight, minimalRadius, backgroundColor);
            }

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(titleX + 1, yBase + 1, titleX + titleBoxWidth - 1, yBase + minimalBoxHeight - 1);
            int titleTextY = (int) (titleBoxCenterY - (Fonts.nunito16.getHeight() / 2f));
            drawScrollingTitle(title, (int) (titleX + 5), titleTextY + 1, (int) (titleBoxWidth - 10), new Color(240, 240, 240).getRGB(), Fonts.nunito16);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(artistX, yBase, artistBoxWidth, minimalBoxHeight, minimalRadius, backgroundColor);
            }

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(artistX + 1, yBase + 1, artistX + artistBoxWidth - 1, yBase + minimalBoxHeight - 1);
            int artistTextY = (int) (artistBoxCenterY - (Fonts.nunito14.getHeight() / 2f));
            drawScrollingAuthor(artistsText, (int) (artistX + 5), artistTextY + 1, (int) (artistBoxWidth - 8), new Color(200, 200, 200).getRGB(), Fonts.nunito14);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            float lowerY = yBase + minimalThumbSize + minimalPadding;
            float timeWidth = Fonts.nunito14.getStringWidth(elapsedStr) + 10;
            float trackTimeWidth = Fonts.nunito14.getStringWidth(totalStr) + 10;
            float progressWidth = boxWidth - timeWidth - trackTimeWidth - (minimalPadding * 2);
            float curX = xBase;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(curX, lowerY, timeWidth, minimalBoxHeight, minimalRadius, backgroundColor);
            }
            Fonts.nunito14.drawStringWithShadow(elapsedStr, curX + 5, lowerY + 4, new Color(240, 240, 240));
            curX += timeWidth + minimalPadding;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(curX, lowerY + (minimalBoxHeight - minimalProgressHeight) / 2f, progressWidth, minimalProgressHeight, 2, backgroundColor);
            }
            if (progress > 0f) {
                RoundedUtil.drawGradientRound(
                        curX,
                        lowerY + (minimalBoxHeight - minimalProgressHeight) / 2f,
                        (int) (progressWidth * progress),
                        (int) minimalProgressHeight,
                        2,
                        currentPalette.primary(),
                        currentPalette.secondary(),
                        currentPalette.tertiary(),
                        currentPalette.accent()
                );
            }
            curX += progressWidth + minimalPadding;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(curX, lowerY, trackTimeWidth, minimalBoxHeight, minimalRadius, backgroundColor);
            }
            Fonts.nunito14.drawStringWithShadow(totalStr, curX + 5, lowerY + 4, new Color(200, 200, 200));

            height = (int) (lowerY + minimalBoxHeight - yBase);
        } else {
            float offw = Fonts.nunito16.getStringWidth("Spotify not playing") + 10;
            applyHorizontalAnchor(sr, offw);
            xBase = (float) x;
            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(xBase, yBase, offw, 12f, 4f, backgroundColor);
            }
            Fonts.nunito16.drawStringWithShadow("Spotify not playing", xBase + 5, yBase + Fonts.nunito16.getMiddleOfBox(12f) + 1, new Color(160, 160, 160));
            height = 16;
        }
    }

    private void renderSpotifyClassic(ScaledResolution sr, float xBase, float yBase, Color backgroundColor) {
        final float fixedHeight = 40f;
        height = (int) fixedHeight;
        final float radius = 6f;
        final float albumSize = 32f;
        final float contentPadding = 8f;
        final float progressBarHeight = 4f;
        final float progressBarRadius = 2f;
        final float rightPadding = 8f;

        if (spotifyApi != null && spotifyApi.authenticated && spotifyApi.currentPlayingContext != null && spotifyApi.currentTrack != null) {
            Track currentTrack = spotifyApi.currentTrack;
            CurrentlyPlayingContext playingContext = spotifyApi.currentPlayingContext;
            String albumUrl = currentTrack.getAlbum().getImages().length > 1
                    ? currentTrack.getAlbum().getImages()[1].getUrl()
                    : currentTrack.getAlbum().getImages()[0].getUrl();

            onSpotifyColorPick(currentTrack);

            String title = currentTrack.getName();
            StringBuilder artists = new StringBuilder();
            for (int a = 0; a < currentTrack.getArtists().length; a++) {
                artists.append(currentTrack.getArtists()[a].getName());
                if (a < currentTrack.getArtists().length - 1) {
                    artists.append(", ");
                }
            }
            String artistText = artists.toString();

            int elapsedSec = playingContext.getProgress_ms() != null ? playingContext.getProgress_ms() / 1000 : 0;
            int totalSec = currentTrack.getDurationMs() != null ? currentTrack.getDurationMs() / 1000 : 0;
            float progress = totalSec > 0 ? Math.min(1.0f, (float) elapsedSec / totalSec) : 0f;
            String elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);
            String totalStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);

            float titleWidth = Math.min(Fonts.nunito16.getStringWidth(title), 120);
            float artistWidth = Math.min(Fonts.nunito12.getStringWidth(artistText), 100);
            float timeWidthLeft = Fonts.nunito12.getStringWidth(elapsedStr);
            float timeWidthRight = Fonts.nunito12.getStringWidth(totalStr);

            float infoBoxWidth = Math.max(
                    Math.max(titleWidth, artistWidth) + rightPadding * 2,
                    timeWidthLeft + rightPadding + timeWidthRight + rightPadding + 80
            );
            float maxWidgetWidth = 150f;
            float contentWidth = Math.min(maxWidgetWidth, contentPadding + albumSize + contentPadding + infoBoxWidth);

            applyHorizontalAnchor(sr, contentWidth);
            xBase = (float) x;

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(xBase, yBase, (float) width, fixedHeight, radius, backgroundColor);
            }

            float thumbY = yBase + (fixedHeight - albumSize) / 2f;
            ThumbnailUtil.drawThumbnail(albumUrl, xBase + contentPadding - 3, thumbY, albumSize, albumSize, 7f, 255);

            float left = xBase + contentPadding + albumSize + contentPadding - 3;
            float boxW = (float) (width - (albumSize + contentPadding * 2));
            float titleMaxWidth = boxW - rightPadding;

            float titleY = yBase + 7;
            float titleBoxHeight = Fonts.nunito16.getHeight();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(left, titleY - 2, left + titleMaxWidth, titleY + titleBoxHeight);
            drawScrollingTitle(title, (int) left, (int) (titleY + Fonts.nunito16.getMiddleOfBox(titleBoxHeight)), (int) titleMaxWidth, Color.WHITE.getRGB(), Fonts.nunito16);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            float artistY = titleY + titleBoxHeight + 2;
            float artistBoxHeight = Fonts.nunito12.getHeight();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.prepareScissorBox(left, artistY - 2, left + titleMaxWidth, artistY + artistBoxHeight);
            drawScrollingAuthor(artistText, (int) left, (int) (artistY + Fonts.nunito12.getMiddleOfBox(artistBoxHeight)), (int) titleMaxWidth, new Color(180, 180, 180).getRGB(), Fonts.nunito12);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            float barPadding = 7;
            float barW = boxW - 2 * barPadding - timeWidthLeft - timeWidthRight;
            if (barW < 40) {
                barW = 40;
            }

            int barX = (int) (left + barPadding + timeWidthLeft);
            int barY = (int) (yBase + fixedHeight - progressBarHeight - 7);

            Fonts.nunito12.drawString(elapsedStr, (int) left, barY, Color.WHITE.getRGB(), false);

            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(barX, barY, (int) barW, (int) progressBarHeight, progressBarRadius, backgroundColor);
            }

            if (progress > 0f) {
                RoundedUtil.drawGradientRound(barX, barY, (int) (barW * progress), (int) progressBarHeight, progressBarRadius,
                        currentPalette.primary(), currentPalette.secondary(), currentPalette.tertiary(), currentPalette.accent());
            }

            Fonts.nunito12.drawString(totalStr, (int) (barX + barW + rightPadding / 2), barY + 0.5F, Color.WHITE.getRGB(), false);
        } else {
            float offw = Fonts.nunito16.getStringWidth("Spotify not playing") + 10;
            applyHorizontalAnchor(sr, offw);
            xBase = (float) x;
            if (!getShaderProcessing().isToggled() || !ShaderProcessing.blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                RoundedUtil.drawRound(xBase, yBase, offw, 12f, 4f, backgroundColor);
            }
            Fonts.nunito16.drawCenteredStringWithShadow("Spotify not playing",
                    (float) (xBase + width / 2f), yBase + fixedHeight / 2f - 5, new Color(140, 140, 140).getRGB());
        }
    }

    private void drawScrollingTitle(String text, int px, int py, int maxWidth, int color, team.gothaj.client.api.util.font.IFontRenderer font) {
        int textWidth = (int) font.getStringWidth(text);
        if (textWidth > maxWidth) {
            float scrollSpeed = 0.1f;
            titleScrollX += scrollSpeed * titleScrollDir;
            float minPosition = -(textWidth - maxWidth + 20);
            float maxPosition = 20;
            if (titleScrollX <= minPosition) {
                titleScrollDir = 1;
                titleScrollX = minPosition;
            } else if (titleScrollX >= maxPosition) {
                titleScrollDir = -1;
                titleScrollX = maxPosition;
            }
            font.drawString(text, px + titleScrollX, py, color, true);
        } else {
            titleScrollX = 0;
            font.drawString(text, px, py, color, true);
        }
    }

    private void drawScrollingAuthor(String text, int px, int py, int maxWidth, int color, team.gothaj.client.api.util.font.IFontRenderer font) {
        int textWidth = (int) font.getStringWidth(text);
        if (textWidth > maxWidth) {
            float scrollSpeed = 0.1f;
            authorScrollX += scrollSpeed * authorScrollDir;
            float minPosition = -(textWidth - maxWidth + 20);
            float maxPosition = 20;
            if (authorScrollX <= minPosition) {
                authorScrollDir = 1;
                authorScrollX = minPosition;
            } else if (authorScrollX >= maxPosition) {
                authorScrollDir = -1;
                authorScrollX = maxPosition;
            }
            font.drawString(text, px + authorScrollX, py, color, true);
        } else {
            authorScrollX = 0;
            font.drawString(text, px, py, color, true);
        }
    }

    private void onSpotifyColorPick(Track track) {
        switch (getMediaPlayer().colorMode.getProperty()) {
            case "HUD" -> currentPalette = new ColorPaletteService.ColorPalette(Hud.primaryColor.getProperty(), Hud.primaryColor.getProperty(), Hud.secondaryColor.getProperty(), Hud.secondaryColor.getProperty());
            case "Track" -> {
                String trackIdentifier = track.getId();
                String imageUrl = track.getAlbum().getImages().length > 1
                        ? track.getAlbum().getImages()[1].getUrl()
                        : track.getAlbum().getImages()[0].getUrl();
                if (!trackIdentifier.equals(lastPaletteTrackId)) {
                    lastPaletteTrackId = trackIdentifier;
                }
                ColorPaletteService.ColorPalette cached = ColorPaletteService.getCached(trackIdentifier);
                if (cached != null) {
                    currentPalette = cached;
                } else {
                    ColorPaletteService.requestPalette(trackIdentifier, imageUrl, new ColorPaletteService.Callback() {
                        @Override
                        public void onResult(String trackId, ColorPaletteService.ColorPalette palette) {
                            if (lastPaletteTrackId != null && lastPaletteTrackId.equals(trackId)) {
                                currentPalette = palette;
                            }
                        }

                        @Override
                        public void onFail(String trackId, Throwable error) {
                        }
                    });
                }
            }

            case "Spotify" -> currentPalette =  new ColorPaletteService.ColorPalette(
                    new Color(30, 215, 96),
                    new Color(25, 200, 80),
                    new Color(30, 215, 96),
                    new Color(60, 200, 80));
        }
    }

    @Override
    public void onShader(ShaderOverlayEvent event) {
        boolean isRadioMode = getMediaPlayer().mode.is("Radio");

        if (getMediaPlayer().uiMode.is("Minimal")) {
            renderShaderMinimal(isRadioMode);
        } else {
            final float fixedHeight = 40f;
            this.height = (int) fixedHeight;
            RoundedUtil.drawShaderRound((float)x, (float)y, (float)width, fixedHeight, 6f, Color.BLACK);
        }
        super.onShader(event);
    }

    private void renderShaderMinimal(boolean isRadioMode) {
        final float maximalTitleBoxWidth = 80f;
        final float maximalArtistsBoxWidth = 44f;
        final float minimalBoxHeight = 12f;
        final float minimalThumbSize = 12f;
        final float minimalProgressHeight = 6f;
        final float minimalRadius = 4f;
        final float minimalPadding = 4f;
        final Color backgroundColor = Color.BLACK;
        ScaledResolution sr = new ScaledResolution(mc);
        float xBase = (float) x;
        float yBase = (float) y;

        boolean hasContent = isRadioMode
                ? (!radioTitle.isEmpty() && !radioArtist.isEmpty())
                : (spotifyApi != null && spotifyApi.authenticated && spotifyApi.currentPlayingContext != null && spotifyApi.currentTrack != null);

        if (hasContent) {
            Track currentTrack = isRadioMode ? null : spotifyApi.currentTrack;

            String title = isRadioMode ? radioTitle : currentTrack.getName();
            float titleTextWidth = Fonts.nunito16.getStringWidth(title) + 10;
            float titleBoxWidth = Math.min(titleTextWidth, maximalTitleBoxWidth);

            String artistsText;
            if (isRadioMode) {
                artistsText = radioArtist;
            } else {
                StringBuilder artists = new StringBuilder();
                for (int a = 0; a < currentTrack.getArtists().length; a++) {
                    artists.append(currentTrack.getArtists()[a].getName());
                    if (a < currentTrack.getArtists().length - 1) artists.append(", ");
                }
                artistsText = artists.toString();
            }

            float artistTextWidth = Fonts.nunito14.getStringWidth(artistsText) + 10;
            float artistBoxWidth = Math.min(artistTextWidth, maximalArtistsBoxWidth);
            float boxWidth = minimalThumbSize + minimalPadding + titleBoxWidth + minimalPadding + artistBoxWidth;
            applyHorizontalAnchor(sr, boxWidth);
            xBase = (float) x;

            float thumbX = xBase;

            RoundedUtil.drawShaderRound(thumbX, yBase, minimalThumbSize, minimalThumbSize, minimalRadius, backgroundColor);

            float titleX = thumbX + minimalThumbSize + minimalPadding;
            RoundedUtil.drawShaderRound(titleX, yBase, titleBoxWidth, minimalBoxHeight, minimalRadius, backgroundColor);

            float artistX = titleX + titleBoxWidth + minimalPadding;
            RoundedUtil.drawShaderRound(artistX, yBase, artistBoxWidth, minimalBoxHeight, minimalRadius, backgroundColor);

            if (!isRadioMode) {
                CurrentlyPlayingContext playingContext = spotifyApi.currentPlayingContext;
                int elapsedSec = playingContext.getProgress_ms() != null ? playingContext.getProgress_ms() / 1000 : 0;
                int totalSec = currentTrack.getDurationMs() != null ? currentTrack.getDurationMs() / 1000 : 0;
                String elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);
                String totalStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);

                float lowerY = yBase + minimalThumbSize + minimalPadding;
                float timeWidth = Fonts.nunito14.getStringWidth(elapsedStr) + 10;
                float trackTimeWidth = Fonts.nunito14.getStringWidth(totalStr) + 10;
                float progressWidth = boxWidth - timeWidth - trackTimeWidth - (minimalPadding * 2);
                float curX = xBase;

                RoundedUtil.drawShaderRound(curX, lowerY, timeWidth, minimalBoxHeight, minimalRadius, backgroundColor);
                curX += timeWidth + minimalPadding;

                RoundedUtil.drawShaderRound(curX, lowerY + (minimalBoxHeight - minimalProgressHeight) / 2f, progressWidth, minimalProgressHeight, 2f, backgroundColor);
                curX += progressWidth + minimalPadding;

                RoundedUtil.drawShaderRound(curX, lowerY, trackTimeWidth, minimalBoxHeight, minimalRadius, backgroundColor);

                height = (int) (lowerY + minimalBoxHeight - yBase);
            } else {
                height = (int) minimalThumbSize;
            }

        } else {
            String message = isRadioMode ? "Radio not playing" : "Spotify not playing";
            float offw = Fonts.nunito16.getStringWidth(message) + 10;
            applyHorizontalAnchor(sr, offw);
            xBase = (float) x;
            RoundedUtil.drawShaderRound(xBase, yBase, offw, minimalBoxHeight, minimalRadius, backgroundColor);
            height = 40;
        }
    }
}
