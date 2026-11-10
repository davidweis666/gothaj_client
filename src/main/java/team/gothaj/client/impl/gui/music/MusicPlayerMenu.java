package team.gothaj.client.impl.gui.music;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.apache.hc.core5.http.ParseException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.music.MusicPlayer;
import team.gothaj.client.api.util.music.playlist.Playlist;
import team.gothaj.client.api.util.music.playlist.PlaylistManager;
import team.gothaj.client.api.util.music.request.SoundCloudTrack;
import team.gothaj.client.api.util.music.spotify.SpotifyAPI;
import team.gothaj.client.api.util.other.colorthief.ColorPaletteService;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.ThumbnailUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.music.input.MusicSearchField;
import team.gothaj.client.main.GothajClient;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class MusicPlayerMenu extends GuiScreen {

    //0 = SoundCloud, 1 = Spotify
    private int provider = 0;

    private float providerAnim = 0f;

    private MusicSearchField searchField;
    private MusicSearchField playlistField;

    // SoundCloud state
    private final List<SoundCloudTrack> scResults = new ArrayList<>();
    private final List<Track> spResults = new ArrayList<>();
    private int scSelected, spSelected = -1;
    private int scCount, spCount = 0;

    private final Object playbackLock = new Object();
    private String currentPlayingTrackId = null;
    private MusicPlayer currentPlayer;
    private boolean isPlaying = false;
    private long playbackStartTime = 0;
    private boolean trackFinished = false;
    private Thread playbackMonitorThread;

    private boolean isSearching;

    private final float[] titleScrollX = new float[15];
    private final int[] scrollDirection = new int[15];
    private final float[] authorScrollX = new float[15];
    private final int[] authorScrollDir = new int[15];


    private volatile ColorPaletteService.ColorPalette currentPalette =
            new ColorPaletteService.ColorPalette(
                    new Color(100, 100, 100),
                    new Color(60, 60, 60),
                    new Color(40, 40, 40),
                    new Color(120, 120, 120)
            );

    private PlaylistManager playlistManager;
    private final String currentUserUid = "1"; //TODO fetch real user
    private List<Playlist> userPlaylists = new ArrayList<>();
    private int selectedPlaylistIndex = -1;
    private boolean showPlaylistView = false;
    private Playlist currentPlaylist = null;
    private int playlistScrollOffset = 0;

    private final SpotifyAPI spotifyApi = new SpotifyAPI();

    @Override
    public void initGui() {
        searchField = new MusicSearchField(1, Fonts.nunito20, 50, 60, 250, 30);
        playlistField = new MusicSearchField(2, Fonts.nunito20, width - 190, 90, 140, 30);
        for (int i = 0; i < scrollDirection.length; i++) {
            scrollDirection[i] = -1;
            titleScrollX[i] = 0;
        }
        for (int i = 0; i < scrollDirection.length; i++) {
            scrollDirection[i] = -1;
            titleScrollX[i] = 0;
            authorScrollDir[i] = -1;
            authorScrollX[i] = 0;
        }

        playlistManager = new PlaylistManager();
        loadUserPlaylists();
        super.initGui();
    }

    private void loadUserPlaylists() {
        userPlaylists = playlistManager.getUserPlaylists(currentUserUid);
        if (selectedPlaylistIndex >= userPlaylists.size()) {
            selectedPlaylistIndex = userPlaylists.isEmpty() ? -1 : userPlaylists.size() - 1;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int trackListWidth = 250;
        int trackListHeight = height - 150;
        int trackInfoWidth = width - 400;
        int trackInfoHeight = height - 90;
        if (showPlaylistView) trackInfoWidth = width - 600;

        Gui.drawRect(0, 0, width, height, new Color(15, 15, 15, 150).getRGB());

        int tabsY = 24;
        boolean scActive = provider == 0;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(50, 120, trackListWidth, trackListHeight, 12f, Color.BLACK);
        searchField.drawTextBox();
        if (!(getSelected() >= 0 && getSelected() < getCount())) {
            RoundedUtil.drawShaderRound(350, 60, trackInfoWidth, trackInfoHeight, 12f, Color.BLACK);
        }
        if (showPlaylistView) {
            RoundedUtil.drawShaderRound(width - 200, 60, 150, trackInfoHeight, 12f, Color.BLACK);
        }

        RoundedUtil.drawShaderRound(50, tabsY, 90, 20, 10f, Color.BLACK);
        Blur.endBlur(30, 0.7f);


        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(50, 120, trackListWidth, trackListHeight, 12f, Color.BLACK);
        searchField.drawTextBox();
        RoundedUtil.drawShaderRound(350, 60, trackInfoWidth, trackInfoHeight, 12f, Color.BLACK);
        if (showPlaylistView) {
            RoundedUtil.drawShaderRound(width - 200, 60, 150, trackInfoHeight, 12f, Color.BLACK);
        }
        RoundedUtil.drawShaderRound(50, tabsY, 90, 20, 10f, Color.BLACK);
        RenderUtil.drawImage(new ResourceLocation("gothaj", "img/soundcloud.png"), 60, tabsY + 4, 25, 10, false);
        RenderUtil.drawImage(new ResourceLocation("gothaj", "img/spotify.png"), 110, tabsY + 2, 15, 15, false);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        int scX = 50;
        int spX = 95;

        int widthBar = 45;

        float target = scActive ? 0f : 1f;
        float speed = 50f;
        float dt = mc.timer.renderPartialTicks;
        float weight = 1f - (float)Math.exp(-speed * (dt / 60f));
        providerAnim += (target - providerAnim) * weight;

        float animX = scX + (spX - scX) * providerAnim;

        RoundedUtil.drawRound(animX, tabsY, widthBar, 20, 10f, scActive ? new Color(0xFF720C) : new Color(0x2EBD59));

        Blur.startBlur();
        RenderUtil.drawImage(new ResourceLocation("gothaj", "img/soundcloud.png"), 60, tabsY + 4, 25, 10, false);
        RenderUtil.drawImage(new ResourceLocation("gothaj", "img/spotify.png"), 110, tabsY + 2, 15, 15, false);
        Blur.endBlur(100, 1f);

        // Search
        int count = getCount();
        if (count > 0) {
            Fonts.nunito21.drawCenteredStringWithShadow("Found " + count + " tracks", 175, 130, -1);
            int startY = 150;
            int lineHeight = 50;
            int renderN = Math.min(count, 6);
            for (int i = 0; i < renderN; i++) {
                String title = getTitle(i);
                String author = getAuthor(i);
                Integer durationSec = getDuration(i);
                String thumb = getThumb(i);

                int y = startY + i * lineHeight;
                if (i == getSelected())
                    RoundedUtil.drawRound(55, y - 6, 215, 50, 12f, new Color(15,15,15,80));

                ThumbnailUtil.drawThumbnail(thumb, 60, y, 38, 38, 6f, 255);
                drawScrollingTitle(title, 105, y + 2, 160, -1, i, Fonts.nunito20);
                int authorY = y + 13;
                if (Fonts.nunito16.getStringWidth(author) <= 160) {
                    Fonts.nunito16.drawStringWithShadow(author, 105, authorY, new Color(150,150,150).getRGB());
                } else {
                    drawScrollingAuthor(author, 105, authorY, 160, new Color(150,150,150).getRGB(), i, Fonts.nunito16);
                }


                if (durationSec != null) {
                    int minutes = durationSec / 60;
                    int remainingSeconds = durationSec % 60;
                    String duration = String.format("%02d:%02d", minutes, remainingSeconds);
                    Fonts.nunito16.drawStringWithShadow(duration, 105, y + 23, new Color(150, 150, 150).getRGB());
                }
            }
        } else {
            String statusText = isSearching ? "Searching..." : "Waiting for input...";
            Fonts.nunito21.drawCenteredStringWithShadow(statusText, 175, height - 230, -1);
        }

        // Currently Playing
        int infoX = 350;
        int infoY = 60;

        if (getSelected() >= 0 && getSelected() < count) {
            String selId = getId(getSelected());
            String selTitle = getTitle(getSelected());
            String selAuthor = getAuthor(getSelected());
            Integer selDuration = getDuration(getSelected());
            String selThumb = getThumb(getSelected());

            ThumbnailUtil.drawThumbnail(selThumb, infoX + 1, infoY + 1, trackInfoWidth - 2, trackInfoHeight - 2, 10f, 255);
            Blur.startBlur();
            RoundedUtil.drawShaderRound(350, 60, trackInfoWidth, trackInfoHeight, 10f, Color.BLACK);
            Blur.endBlur(20, 0.7f);

            guiFrameBuffer.framebufferClear();
            guiFrameBuffer.bindFramebuffer(true);
            ThumbnailUtil.drawThumbnail(selThumb, infoX + (trackInfoWidth / 2f) - 90, infoY + 100, 180, 180, 10f, 255);
            guiFrameBuffer.unbindFramebuffer();
            Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 8, Color.BLACK.getRGB());
            ThumbnailUtil.drawThumbnail(selThumb, infoX + (trackInfoWidth / 2f) - 90, infoY + 100, 180, 180, 12f, 255);

            int textX = (int) (infoX + (trackInfoWidth / 2f));
            int textY = infoY + 300;
            drawScrollingTitle(selTitle, (int) (textX - Fonts.nunito28.getStringWidth(selTitle) / 2f), textY, trackInfoWidth - 5, Color.WHITE.getRGB(), 9, Fonts.nunito28);
            Fonts.nunito24.drawCenteredStringWithShadow(selAuthor, textX, textY + 17, new Color(180, 180, 180).getRGB());

            if (provider == 0 && selDuration != null && (isPlaying || trackFinished) && currentPlayer != null && currentPlayingTrackId != null && currentPlayingTrackId.equals(selId)) {
                long currentTime = System.currentTimeMillis();
                long elapsed = (currentTime - playbackStartTime) / 1000;
                int totalDuration = selDuration;
                float progress = trackFinished ? 1.0f : Math.min(1.0f, (float) elapsed / totalDuration);
                int barWidth = trackInfoWidth - 220;
                int barHeight = 6;
                int barY = textY + 80;

                RoundedUtil.drawRound(textX - barWidth / 2f, barY, barWidth, barHeight, 3f, new Color(10, 10, 10, 30));
                if (progress > 0) {
                    RoundedUtil.drawGradientRound(
                            textX - barWidth / 2f, barY, (int) (barWidth * progress), barHeight, 3f,
                            currentPalette.primary(), currentPalette.secondary(), currentPalette.tertiary(), currentPalette.accent()
                    );
                }

                int displayedElapsed = trackFinished ? totalDuration : (int) elapsed;
                String elapsedStr = String.format("%02d:%02d", displayedElapsed / 60, displayedElapsed % 60);
                String totalStr = String.format("%02d:%02d", totalDuration / 60, totalDuration % 60);

                Fonts.nunito16.drawStringWithShadow(elapsedStr, textX - barWidth / 2f, barY + 15, Color.WHITE.getRGB());
                Fonts.nunito16.drawStringWithShadow(totalStr, textX - barWidth / 2f + barWidth - Fonts.nunito16.getStringWidth(totalStr), barY + 15, Color.WHITE.getRGB());
            }
        } else if (provider == 1 && spotifyApi.authenticated && spotifyApi.currentPlayingContext != null && spotifyApi.currentTrack != null) {

            Track currentTrack = spotifyApi.currentTrack;
            CurrentlyPlayingContext playingContext = spotifyApi.currentPlayingContext;
            onSpotifyColorPick(currentTrack);

            if (currentTrack != spotifyApi.currentTrack || playingContext != spotifyApi.currentPlayingContext) {
                currentTrack = spotifyApi.currentTrack;
                playingContext = spotifyApi.currentPlayingContext;
            }

            final StringBuilder artistsDisplay = new StringBuilder();
            for (int artistIndex = 0; artistIndex < currentTrack.getArtists().length; artistIndex++) {
                final ArtistSimplified artist = currentTrack.getArtists()[artistIndex];
                artistsDisplay.append(artist.getName()).append(artistIndex + 1 == currentTrack.getArtists().length ? "" : ", ");
            }

            ThumbnailUtil.drawThumbnail(currentTrack.getAlbum().getImages()[1].getUrl(), infoX + 1, infoY + 1, trackInfoWidth - 2, trackInfoHeight - 2, 10f, 255);
            Blur.startBlur();
            RoundedUtil.drawShaderRound(350, 60, trackInfoWidth, trackInfoHeight, 10f, Color.BLACK);
            Blur.endBlur(20, 0.7f);

            guiFrameBuffer.framebufferClear();
            guiFrameBuffer.bindFramebuffer(true);
            ThumbnailUtil.drawThumbnail(currentTrack.getAlbum().getImages()[1].getUrl(), infoX + (trackInfoWidth / 2f) - 90, infoY + 100, 180, 180, 10f, 255);
            guiFrameBuffer.unbindFramebuffer();
            Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 8, Color.BLACK.getRGB());

            ThumbnailUtil.drawThumbnail(currentTrack.getAlbum().getImages()[1].getUrl(), infoX + (trackInfoWidth / 2f) - 90, infoY + 100, 180, 180, 12f, 255);

            int textX = (int) (infoX + (trackInfoWidth / 2f));
            int textY = infoY + 300;
            drawScrollingTitle(currentTrack.getName(), (int) (textX - Fonts.nunito28.getStringWidth(currentTrack.getName()) / 2f), textY, trackInfoWidth - 5, Color.WHITE.getRGB(), 9, Fonts.nunito28);
            Fonts.nunito24.drawCenteredStringWithShadow(artistsDisplay.toString(), textX, textY + 17, new Color(180, 180, 180).getRGB());

            if (spotifyApi.authenticated && spotifyApi.currentPlayingContext != null && spotifyApi.currentTrack != null) {
                CurrentlyPlayingContext cpc = spotifyApi.currentPlayingContext;
                Track ct = spotifyApi.currentTrack;

                int elapsedSec = cpc.getProgress_ms() != null ? cpc.getProgress_ms() / 1000 : 0;
                int totalSec = ct.getDurationMs() != null ? ct.getDurationMs() / 1000 : 0;

                float progress = (totalSec > 0) ? Math.min(1.0f, (float) elapsedSec / (float) totalSec) : 0f;

                int barWidth = trackInfoWidth - 220;
                int barHeight = 6;
                int barY = textY + 80;

                RoundedUtil.drawRound(textX - barWidth / 2f, barY, barWidth, barHeight, 3f, new Color(10, 10, 10, 30));
                if (progress > 0f) {
                    RoundedUtil.drawGradientRound(
                            textX - barWidth / 2f, barY, (int) (barWidth * progress), barHeight, 3f,
                            currentPalette.primary(), currentPalette.secondary(), currentPalette.tertiary(), currentPalette.accent()
                    );
                }

                String elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60);
                String totalStr   = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
                Fonts.nunito16.drawStringWithShadow(elapsedStr, textX - barWidth / 2f, barY + 15, Color.WHITE.getRGB());
                Fonts.nunito16.drawStringWithShadow(totalStr, textX - barWidth / 2f + barWidth - Fonts.nunito16.getStringWidth(totalStr), barY + 15, Color.WHITE.getRGB());
            }
        } else {
            Fonts.nunito21.drawCenteredStringWithShadow("Select a track", infoX + trackInfoWidth / 2f, infoY + trackInfoHeight / 2f, new Color(100, 100, 100).getRGB());
        }

        drawPlaylistUI(mouseX, mouseY);

        if (!showPlaylistView) {
            Fonts.nunito16.drawStringWithShadow("Press P for Playlists", width - Fonts.nunito16.getStringWidth("Press P for Playlists") - 2, height - Fonts.nunito16.getHeight() - 1, new Color(100, 100, 100).getRGB());
        }

        searchField.drawText();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawButton(int x, int y, int w, int h, String label) {
        RoundedUtil.drawRound(x, y, w, h, 6f, new Color(35, 40, 50, 200));
        Fonts.nunito18.drawStringWithShadow(label, x + w/2f - Fonts.nunito18.getStringWidth(label)/2f, y + h/2f - Fonts.nunito18.getHeight()/2f + 1, 0xFFFFFFFF);
    }

    private int getCount() {
        return provider == 0 ? scCount : spCount;
    }

    private int getSelected() {
        return provider == 0 ? scSelected : spSelected;
    }

    private String getId(int idx) {
        if (idx < 0 || idx >= getCount()) return "";

        if (provider == 0) {
            SoundCloudTrack t = scResults.get(idx);
            if (t.getId() != null && !t.getId().isEmpty()) return t.getId();
            return t.getUrl() != null ? t.getUrl() : t.getTitle();
        } else {
            return spResults.get(idx).getId();
        }
    }

    private String getTitle(int idx) {
        if (idx < 0 || idx >= getCount()) return "";
        return provider == 0 ? scResults.get(idx).getTitle() : spResults.get(idx).getName();
    }


    private String getAuthor(int idx) {

        if (idx < 0 || idx >= getCount()) return "";

        final StringBuilder artistsDisplay = new StringBuilder();

        if (provider == 1) {
            if (spResults.get(idx).getArtists().length == 0) return "Unknown";
            for (int artistIndex = 0; artistIndex < spResults.get(idx).getArtists().length; artistIndex++) {
                final ArtistSimplified artist = spResults.get(idx).getArtists()[artistIndex];
                artistsDisplay.append(artist.getName()).append(artistIndex + 1 == spResults.get(idx).getArtists().length ? "" : ", ");
            }
        }
        return provider == 0 ? scResults.get(idx).getUploader() : artistsDisplay.toString();
    }

    private Integer getDuration(int idx) {
        if (idx < 0 || idx >= getCount()) return 0;

        return provider == 0 ? scResults.get(idx).getDuration() : spResults.get(idx).getDurationMs() / 1000;
    }

    private String getThumb(int idx) {
        if (idx < 0 || idx >= getCount()) return "";

        return provider == 0 ? scResults.get(idx).getThumbnail() : spResults.get(idx).getAlbum().getImages()[1].getUrl();
    }

    private void drawPlaylistUI(int mouseX, int mouseY) {
        if (!showPlaylistView) return;

        int panelX = width - 200;
        int panelY = 60;
        int panelW = 150;
        int panelH = height - 50;

        Fonts.nunito24.drawCenteredStringWithShadow("Playlists", panelX + panelW / 2f, panelY + 20, -1);

        int createY = panelY + 30;

        playlistField.setPlaceholder("Search playlist...");
        playlistField.drawText();

        int startY = createY + 30;
        int itemH = 56;
        int visible = Math.max(1, (panelH - (startY - panelY) - 20) / itemH);

        if (playlistScrollOffset < 0) playlistScrollOffset = 0;
        int maxOffset = Math.max(0, userPlaylists.size() - visible);
        if (playlistScrollOffset > maxOffset) playlistScrollOffset = maxOffset;

        for (int i = 0; i < visible; i++) {
            int idx = playlistScrollOffset + i;
            if (idx >= userPlaylists.size()) break;
            Playlist pl = userPlaylists.get(idx);

            int y = startY + i * itemH;

            Color bg = (idx == selectedPlaylistIndex) ? new Color(60, 60, 150, 120) : new Color(30, 30, 30, 120);
            RoundedUtil.drawRound(panelX + 10, y, panelW - 20, itemH - 6, 8f, bg);
            Fonts.nunito20.drawStringWithShadow(pl.getName(), panelX + 20, y + 10, -1);
            Fonts.nunito16.drawStringWithShadow(pl.getTrackCount() + " tracks", panelX + 20, y + 21, new Color(150, 150, 150).getRGB());

            Fonts.nunito14.drawStringWithShadow("Right click to add", panelX + 20, y + 40, new Color(120, 120, 120).getRGB());
        }

        if (userPlaylists.size() > visible) {
            float ratio = (float) visible / userPlaylists.size();
            int barH = Math.max(20, (int) (ratio * (panelH - (startY - panelY) - 20)));
            int maxScrollArea = (panelH - (startY - panelY) - 20) - barH;
            int scrollY = (maxOffset == 0) ? 0 : (int) (maxScrollArea * (playlistScrollOffset / (float) maxOffset));
            RoundedUtil.drawRound(panelX + panelW - 8, startY + scrollY, 4, barH, 2f, new Color(200, 200, 200, 120));
        }
    }

    private void drawScrollingTitle(String text, int x, int y, int maxWidth, int color, int trackIndex, IFontRenderer font) {
        int textWidth = (int) font.getStringWidth(text);
        if (textWidth <= maxWidth) {
            font.drawStringWithShadow(text, x, y, color);
            titleScrollX[trackIndex] = 0;
        } else {
            ScaledResolution sr = new ScaledResolution(mc);
            int scale = sr.getScaleFactor();
            int actualClipX, actualClipWidth;
            if (trackIndex == 9) {
                int centerX = x + (int) (Fonts.nunito28.getStringWidth(text) / 2f);
                actualClipX = centerX - maxWidth / 2;
                actualClipWidth = maxWidth;
            } else {
                actualClipX = x;
                actualClipWidth = maxWidth;
            }
            int scissorX = actualClipX * scale;
            int scissorY = (sr.getScaledHeight() - y - font.getHeight() - 2) * scale;
            int scissorWidth = actualClipWidth * scale;
            int scissorHeight = (font.getHeight() + 4) * scale;

            if (scissorX >= 0 && scissorY >= 0 && scissorWidth > 0 && scissorHeight > 0 &&
                    scissorX < sr.getScaledWidth() * scale && scissorY < sr.getScaledHeight() * scale) {

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);

                float scrollSpeed = 0.2f;
                titleScrollX[trackIndex] += scrollSpeed * scrollDirection[trackIndex];
                float minPosition = -(textWidth - maxWidth + 20);
                float maxPosition = 20;
                if (titleScrollX[trackIndex] <= minPosition) {
                    scrollDirection[trackIndex] = 1;
                    titleScrollX[trackIndex] = minPosition;
                } else if (titleScrollX[trackIndex] >= maxPosition) {
                    scrollDirection[trackIndex] = -1;
                    titleScrollX[trackIndex] = maxPosition;
                }

                GlStateManager.pushMatrix();
                float drawX = x + titleScrollX[trackIndex];
                font.drawStringWithShadow(text, drawX, y, color);
                GlStateManager.popMatrix();

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            } else {
                font.drawStringWithShadow(text, x, y, color);
            }
        }
    }

    private void drawScrollingAuthor(String text, int x, int y, int maxWidth, int color, int rowIndex, IFontRenderer font) {
        int textWidth = (int) font.getStringWidth(text);
        if (textWidth <= maxWidth) {
            font.drawStringWithShadow(text, x, y, color);
            authorScrollX[rowIndex] = 0;
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        int scissorX = x * scale;
        int scissorY = (sr.getScaledHeight() - y - font.getHeight() - 2) * scale;
        int scissorWidth = maxWidth * scale;
        int scissorHeight = (font.getHeight() + 4) * scale;

        if (scissorX < 0 || scissorY < 0 || scissorWidth <= 0 || scissorHeight <= 0 ||
                scissorX >= sr.getScaledWidth() * scale || scissorY >= sr.getScaledHeight() * scale) {
            font.drawStringWithShadow(text, x, y, color);
            return;
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        float scrollSpeed = 0.2f;
        authorScrollX[rowIndex] += scrollSpeed * authorScrollDir[rowIndex];
        float minPosition = -(textWidth - maxWidth + 20);
        float maxPosition = 20;
        if (authorScrollX[rowIndex] <= minPosition) {
            authorScrollDir[rowIndex] = 1;
            authorScrollX[rowIndex] = minPosition;
        } else if (authorScrollX[rowIndex] >= maxPosition) {
            authorScrollDir[rowIndex] = -1;
            authorScrollX[rowIndex] = maxPosition;
        }

        GlStateManager.pushMatrix();
        float drawX = x + authorScrollX[rowIndex];
        font.drawStringWithShadow(text, drawX, y, color);
        GlStateManager.popMatrix();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        searchField.textboxKeyTyped(typedChar, keyCode);
        playlistField.textboxKeyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_RETURN) {
            for (int i = 0; i < titleScrollX.length; i++) {
                titleScrollX[i] = 0; scrollDirection[i] = -1;
            }

            ColorPaletteService.clearCache();
            currentPalette = new ColorPaletteService.ColorPalette(
                    new Color(100, 100, 100),
                    new Color(60, 60, 60),
                    new Color(40, 40, 40),
                    new Color(120, 120, 120)
            );
            ThumbnailUtil.clearCache();

            String query = searchField.getText().trim();

            if (provider == 0) {

                if (!query.isEmpty()) {
                    isSearching = true;
                    new Thread(() -> {
                        try {
                            List<SoundCloudTrack> tracks = MusicPlayer.searchTracks(query);
                            GothajClient.scheduledExecutorService.execute(() -> {
                                scResults.clear();
                                scResults.addAll(tracks);
                                scSelected = -1;
                                scCount = tracks.size();
                                isSearching = false;
                            });
                        } catch (Exception e) {
                            GothajClient.scheduledExecutorService.execute(() -> {
                                scResults.clear();
                                scSelected = -1;
                                scCount = 0;
                                isSearching = false;
                            });
                        }
                    }, "MusicSearch-SC").start();
                }
            } else {
                if (!query.isEmpty()) {
                    isSearching = true;
                    new Thread(() -> {
                        try {
                            List<Track> tracks = spotifyApi.searchTracks(query);
                            GothajClient.scheduledExecutorService.execute(() -> {
                                spResults.clear();
                                spResults.addAll(tracks);
                                spSelected = -1;
                                spCount = tracks.size();
                                isSearching = false;
                            });
                        } catch (Exception e) {
                            GothajClient.scheduledExecutorService.execute(() -> {
                                spResults.clear();
                                spSelected = -1;
                                spCount = 0;
                                isSearching = false;
                            });
                        }
                    }, "MusicSearch-SP").start();
                }
            }
        }

        if (keyCode == Keyboard.KEY_P && (!searchField.isFocused() && !playlistField.isFocused())) {
            showPlaylistView = !showPlaylistView;
            if (showPlaylistView) {
                loadUserPlaylists();
            }
        }

        if (showPlaylistView) {
            if (keyCode == Keyboard.KEY_UP) {
                playlistScrollOffset = Math.max(0, playlistScrollOffset - 1);
            } else if (keyCode == Keyboard.KEY_DOWN) {
                int visible = Math.max(1, (height - 120 - 110) / 56);
                int maxOffset = Math.max(0, userPlaylists.size() - visible);
                playlistScrollOffset = Math.min(maxOffset, playlistScrollOffset + 1);
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        playlistField.mouseClicked(mouseX, mouseY, mouseButton);

        int tabsY = 24;
        if (mouseY >= tabsY && mouseY <= tabsY + 20) {
            if (mouseX >= 50 && mouseX <= 95) {
                provider = 0;
            } else if (mouseX >= 95 && mouseX <= 140) {
                provider = 1;
                spotifyApi.init();
            }
        }

        int count = getCount();
        if (count > 0) {
            int startY = 150;
            int lineHeight = 50;
            int renderN = Math.min(count, 6);
            for (int i = 0; i < renderN; i++) {
                int y = startY + i * lineHeight;
                if (mouseX >= 60 && mouseX <= 290 && mouseY >= y && mouseY <= y + lineHeight) {
                    if (provider == 0) {
                        scSelected = i;
                        SoundCloudTrack selectedTrack = scResults.get(i);
                        onTrackSelectedSC(selectedTrack);
                        playTrackSC(selectedTrack);
                    } else {
                        spSelected = i;
                        Track selectedTrack = spResults.get(i);
                        onSpotifyColorPick(selectedTrack);
                        playTrackSP(selectedTrack);
                        spSelected = -1;
                    }
                    break;
                }
            }
        }

        if (showPlaylistView) {
            int panelX = width - 200;
            int panelY = 60;
            int panelW = 150;
            int createY = panelY + 50;

            int startY = createY + 30;
            int itemH = 56;
            int visible = Math.max(1, (height - 120 - 110) / itemH);

            for (int i = 0; i < visible; i++) {
                int idx = playlistScrollOffset + i;
                if (idx >= userPlaylists.size()) break;

                int y = startY + i * itemH;
                if (mouseX >= panelX + 10 && mouseX <= panelX + panelW - 10 &&
                        mouseY >= y && mouseY <= y + itemH - 6) {

                    if (mouseButton == 0) {
                        selectedPlaylistIndex = idx;
                        currentPlaylist = userPlaylists.get(idx);
                        loadUserPlaylists();
                        loadPlaylistTracks();
                    } else if (mouseButton == 1) {
                        if (scSelected >= 0 && scSelected < scResults.size()) {
                            addTrackToPlaylist(userPlaylists.get(idx), scResults.get(scSelected));
                        }
                    }
                    break;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        if (showPlaylistView) {
            int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
            if (dWheel != 0) {
                int panelX = width - 300;
                int panelY = 60;
                int panelW = 280;
                int panelH = height - 120;

                int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
                int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

                if (mouseX >= panelX && mouseX <= panelX + panelW &&
                        mouseY >= panelY && mouseY <= panelY + panelH) {

                    if (dWheel > 0) {
                        playlistScrollOffset = Math.max(0, playlistScrollOffset - 1);
                    } else if (dWheel < 0) {
                        int visible = Math.max(1, (panelH - 110) / 56);
                        int maxOffset = Math.max(0, userPlaylists.size() - visible);
                        playlistScrollOffset = Math.min(maxOffset, playlistScrollOffset + 1);
                    }
                }
            }
        }
    }

    private void onTrackSelectedSC(SoundCloudTrack selectedTrack) {
        String trackIdentifier = (selectedTrack.getId() != null && !selectedTrack.getId().isEmpty())
                ? selectedTrack.getId() : (selectedTrack.getUrl() != null ? selectedTrack.getUrl() : selectedTrack.getTitle());
        ColorPaletteService.ColorPalette cached = ColorPaletteService.getCached(trackIdentifier);
        if (cached != null) {
            currentPalette = cached;
        } else {
            ColorPaletteService.requestPalette(trackIdentifier, selectedTrack.getThumbnail(),
                    new ColorPaletteService.Callback() {
                        @Override
                        public void onResult(String trackId, ColorPaletteService.ColorPalette palette) {
                            GothajClient.scheduledExecutorService.execute(() -> {
                                if (scSelected >= 0 && scSelected < scResults.size()) {
                                    String cur = (scResults.get(scSelected).getId() != null && !scResults.get(scSelected).getId().isEmpty())
                                            ? scResults.get(scSelected).getId()
                                            : (scResults.get(scSelected).getUrl() != null ? scResults.get(scSelected).getUrl() : scResults.get(scSelected).getTitle());
                                    if (cur.equals(trackId)) currentPalette = palette;
                                }
                            });
                        }
                        @Override public void onFail(String trackId, Throwable error) {}
                    });
        }
    }

    private void playTrackSC(SoundCloudTrack track) {
        synchronized (playbackLock) {
            String trackIdentifier = (track.getId() != null && !track.getId().isEmpty())
                    ? track.getId() : (track.getUrl() != null ? track.getUrl() : track.getTitle());
            if (isPlaying && currentPlayingTrackId != null && currentPlayingTrackId.equals(trackIdentifier)) return;
            if (currentPlayer != null) { currentPlayer.stop(); currentPlayer = null; }
            if (playbackMonitorThread != null) { playbackMonitorThread.interrupt(); }
            isPlaying = true; trackFinished = false; currentPlayingTrackId = trackIdentifier;
        }

        new Thread(() -> {
            try {
                MusicPlayer newPlayer = MusicPlayer.downloadTrack(track);
                synchronized (playbackLock) {
                    String id = (track.getId() != null && !track.getId().isEmpty())
                            ? track.getId() : (track.getUrl() != null ? track.getUrl() : track.getTitle());
                    if (!id.equals(currentPlayingTrackId)) return;
                    currentPlayer = newPlayer;
                    playbackStartTime = System.currentTimeMillis();
                    currentPlayer.play();
                    startPlaybackMonitor(track.getDuration() != null ? track.getDuration() : 0);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                synchronized (playbackLock) {
                    isPlaying = false;
                    currentPlayingTrackId = null;
                    trackFinished = false;
                }
            }
        }, "MusicPlay-SC").start();
    }

    private void playTrackSP(Track track) {
        if (track != null && spotifyApi.currentPlayingContext != null && spotifyApi.currentPlayingContext.getDevice() != null) {
            try {
                spotifyApi.SPOTIFY.addItemToUsersPlaybackQueue(track.getUri()).build().execute();
                spotifyApi.skipTrack();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SpotifyWebApiException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void startPlaybackMonitor(int durationSeconds) {
        if (durationSeconds <= 0) return;
        playbackMonitorThread = new Thread(() -> {
            try {
                Thread.sleep(durationSeconds * 1000L);
                synchronized (playbackLock) {
                    if (!Thread.currentThread().isInterrupted()) {
                        trackFinished = true;
                        isPlaying = false;
                    }
                }
            } catch (InterruptedException ignored) {}
        }, "PlaybackMonitor");
        playbackMonitorThread.start();
    }

    private void stopCurrentTrack() {
        synchronized (playbackLock) {
            if (currentPlayer != null) { currentPlayer.stop(); currentPlayer = null; }
            if (playbackMonitorThread != null) { playbackMonitorThread.interrupt(); playbackMonitorThread = null; }
            isPlaying = false; trackFinished = false; currentPlayingTrackId = null; playbackStartTime = 0;
        }
    }

    private void createNewPlaylist() {
        String playlistName = "New Playlist " + (userPlaylists.size() + 1);
        Playlist newPlaylist = playlistManager.createPlaylist(currentUserUid, playlistName, "");
        loadUserPlaylists();
        for (int i = 0; i < userPlaylists.size(); i++) {
            if (userPlaylists.get(i).getId().equals(newPlaylist.getId())) {
                selectedPlaylistIndex = i;
                currentPlaylist = newPlaylist;
                break;
            }
        }
    }

    private void addTrackToPlaylist(Playlist playlist, SoundCloudTrack track) {
        boolean added = playlistManager.addTrackToPlaylist(currentUserUid, playlist.getId(), track);
        if (added) {
            if (currentPlaylist != null && currentPlaylist.getId().equals(playlist.getId())) {
                loadPlaylistTracks();
            }
            loadUserPlaylists();
        }
    }

    private void loadPlaylistTracks() {
        if (currentPlaylist != null) {
            Playlist refreshed = playlistManager.getPlaylist(currentUserUid, currentPlaylist.getId());
            if (refreshed != null) {
                currentPlaylist = refreshed;
                scResults.clear();
                scResults.addAll(currentPlaylist.getTracks());
                scCount = currentPlaylist.getTracks().size();
                scSelected = -1;
            }
        }
    }

    private void onSpotifyColorPick(Track selectedTrack) {
        String trackIdentifier = selectedTrack.getId();
        ColorPaletteService.ColorPalette cached = ColorPaletteService.getCached(trackIdentifier);
        if (cached != null) {
            currentPalette = cached;
        } else {
            ColorPaletteService.requestPalette(trackIdentifier, selectedTrack.getAlbum().getImages()[1].getUrl(),
                    new ColorPaletteService.Callback() {
                        @Override
                        public void onResult(String trackId, ColorPaletteService.ColorPalette palette) {
                            GothajClient.scheduledExecutorService.execute(() -> {
                                if (scSelected >= 0 && scSelected < scResults.size()) {
                                    String cur = (scResults.get(scSelected).getId() != null && !scResults.get(scSelected).getId().isEmpty())
                                            ? scResults.get(scSelected).getId()
                                            : (scResults.get(scSelected).getUrl() != null ? scResults.get(scSelected).getUrl() : scResults.get(scSelected).getTitle());
                                    if (cur.equals(trackId)) currentPalette = palette;
                                }
                            });
                        }
                        @Override public void onFail(String trackId, Throwable error) {}
                    });
        }
    }


    @Override
    public void onGuiClosed() {
        stopCurrentTrack();
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
    }
}
