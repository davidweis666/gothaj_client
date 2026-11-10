package team.gothaj.client.impl.module.client;

import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.InputProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.music.spotify.SpotifyAPI;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.widget.MediaPlayerWidget;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;

public class MediaPlayer extends Module {

    private AdvancedPlayer player;
    private Thread playerThread;
    private volatile boolean shouldPlay = false;

    public ModeProperty mode = new ModeProperty("Mode", "Spotify", "Radio", "Spotify")
            .addChangeListener((property, oldValue, newValue) -> {
                if (!oldValue.equalsIgnoreCase(newValue) && newValue.equalsIgnoreCase("Radio")) {
                    restartRadio();
                } else {
                    stopRadio();
                }
                return newValue;
            });

    public ModeProperty colorMode = new ModeProperty("Color Mode", "Track", "Track", "HUD", "Spotify")
            .setVisible(() -> mode.is("Spotify"));

    public ModeProperty uiMode = new ModeProperty("UI Mode", "Minimal", "Minimal", "Classic");

    public final InputProperty clientId = new InputProperty("Client ID")
            .setVisible(() -> mode.is("Spotify")).addChangeListener((property, oldValue, newValue) -> {
                if (!oldValue.equalsIgnoreCase(newValue)) {
                    toggle();
                }
                return newValue;
            });

    public final ModeProperty radioStation = new ModeProperty("Station", "ILoveRadio",
            "ILoveRadio", "ILove2Dance", "ILove2000", "ILove2010", "Custom")
            .setVisible(() -> mode.is("Radio"))
            .addChangeListener((property, oldValue, newValue) -> {
                if (!oldValue.equalsIgnoreCase(newValue) && player != null) {
                    restartRadio();
                }
                return newValue;
            });

    private final InputProperty customStation = new InputProperty("Radio URL")
            .setVisible(() -> mode.is("Radio") && radioStation.is("Custom"));

    private final NumberProperty volume = new NumberProperty("Volume", 50, 1, 100, 1)
            .setVisible(() -> mode.is("Radio"))
            .addChangeListener((property, oldValue, newValue) -> {
                new Thread(() -> {
                    if (player != null) {
                        player.setVolume(newValue.intValue());
                    }
                }).start();
                return newValue;
            });

    public MediaPlayer() {
        super("MediaPlayer", "Media Player for your client", ModuleCategory.CLIENT);
        registerProperties(mode, uiMode, colorMode, clientId, radioStation, customStation, volume);
    }

    @Override
    public String getSuffix() {
        return mode.is("Radio") ? radioStation.getProperty() : mode.getProperty();
    }

    @Listen
    public void onShader(ShaderOverlayEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (w.getLabel().equalsIgnoreCase("MediaPlayer")) {
                w.onShader(event);
            }
        });
    }

    @Listen
    public void onRender2D(RenderOverlayEvent event) {
        GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
            if (w.getLabel().equalsIgnoreCase("MediaPlayer")) {
                w.onRender(event.getScaledResolution());
            }
        });
    }

    @Override
    public void onEnable() {
        if (mode.is("Spotify")) {
            MediaPlayerWidget.spotifyApi = new SpotifyAPI();
            MediaPlayerWidget.spotifyApi.init();
        } else if (mode.is("Radio")) {
            startRadio();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mode.is("Radio")) {
            stopRadio();
        }
        super.onDisable();
    }

    private void startRadio() {
        if (player != null ||!isToggled()) {
            return;
        }
        shouldPlay = true;

        playerThread = new Thread(() -> {
            while (shouldPlay) {
                try {
                    String urlString = getRadioURL();
                    if (urlString == null || urlString.isEmpty())
                        break;

                    URI url = new URI(urlString);
                    InputStream stream = new BufferedInputStream(url.toURL().openStream());
                    JavaSoundAudioDevice device = new JavaSoundAudioDevice();
                    player = new AdvancedPlayer(stream, device);

                    player.setPlaybackListener(new PlaybackListener() {
                        @Override
                        public void playbackFinished(PlaybackEvent evt) {
                            if (shouldPlay)
                                restartRadio();
                        }
                    });

                    player.setVolume(volume.getProperty().intValue());
                    player.play();

                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }
        }, "Radio-Thread");

        playerThread.start();
    }

    private synchronized void restartRadio() {
        stopRadio();
        startRadio();
    }

    private void stopRadio() {
        shouldPlay = false;

        if (player != null) {
            player.close();
            player = null;
        }

        if (playerThread != null && playerThread.isAlive()) {
            playerThread.interrupt();
            try {
                playerThread.join(1000);
            } catch (InterruptedException ignored) { }
            playerThread = null;
        }
    }

    private String getRadioURL() {
        return switch (radioStation.getProperty()) {
            case "ILoveRadio" -> "https://ilm.stream12.radiohost.de/ilm_iloveradio_mp3-192";
            case "ILove2Dance" -> "https://ilm.stream35.radiohost.de/ilm_ilove2dance_mp3-192";
            case "ILove2010" -> "https://ilm.stream12.radiohost.de/ilm_ilove2010throwbacks_mp3-192";
            case "ILove2000" -> "https://ilm.stream12.radiohost.de/ilm_ilove2000throwbacks_mp3-192";
            case "Custom" -> customStation.getString();
            default -> "https://ilm.stream12.radiohost.de/ilm_iloveradio_mp3-192";
        };
    }

    public String getScrapeURL() {
        return switch (radioStation.getProperty()) {
            case "ILoveRadio" -> "https://ilovemusic.de/iloveradio/";
            case "ILove2Dance" -> "https://ilovemusic.de/ilove2dance/";
            case "ILove2010" -> "https://ilovemusic.de/ilove2010throwbacks/";
            case "ILove2000" -> "https://ilovemusic.de/ilove2000throwbacks/";
            default -> "https://ilovemusic.de/iloveradio/";
        };
    }
}
