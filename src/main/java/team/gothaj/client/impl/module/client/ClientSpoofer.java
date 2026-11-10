package team.gothaj.client.impl.module.client;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.value.ValueChangeEvent;
import team.gothaj.eventbus.Listen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vajzii
 * @since 2026
 */
public class ClientSpoofer extends Module {

    public final ModeProperty mode = new ModeProperty("Client", "Lunar", "Lunar", "Feather", "Forge");

    public static String client;

    private String lunarVersion;
    private boolean fetching = false;

    public ClientSpoofer() {
        super("ClientSpoofer", "", ModuleCategory.CLIENT);
        registerProperties(mode);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Listen
    public void onValue(ValueChangeEvent event) {
        if (event.getProperty() != mode) return;
        applyClient();
    }

    @Listen
    public void onTick(TickEvent event) {
        if (lunarVersion == null && !fetching) {
            fetching = true;
            CompletableFuture.runAsync(this::fetchLatestLunar);
        }
    }

    @Override
    public void onEnable() {
        applyClient();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        client = null;
        super.onDisable();
    }

    private void applyClient() {
        switch (mode.getProperty()) {
            case "Lunar" -> {
                if (lunarVersion != null)
                    client = "lunarclient:" + lunarVersion;
            }
            case "Feather" -> client = "Feather Forge";
            case "Forge" -> client = "fml,forge";
        }
    }

    private void fetchLatestLunar() {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.lunarclient.com/changelog"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP error status code: " + response.statusCode());
            }

            Pattern pattern = Pattern.compile("game\\s*-\\s*(v\\d+\\.\\d+\\.\\d+-\\d+)");
            Matcher matcher = pattern.matcher(response.body());

            if (matcher.find()) {
                lunarVersion = matcher.group(1);
                if (isToggled()) {
                    applyClient();
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            fetching = false;
        }
    }
}
