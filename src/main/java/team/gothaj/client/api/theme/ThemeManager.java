package team.gothaj.client.api.theme;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

@Getter
@Setter
public class ThemeManager extends ListManager<CustomTheme> {

    private CustomTheme currentTheme;

    private final File themeDir = new File(GothajClient.getInstance().getClientDir() + File.separator + "themes");
    private final File themesFile = new File(themeDir, "themes.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void init() {
        for (Theme theme : Theme.values()) {
            add(new CustomTheme(theme.name(), theme.getPrimary(), theme.getSecondary()));
        }
        setCurrentTheme(findTheme("Gothaj"));
        load();
        super.init();
    }

    public CustomTheme findTheme(String name) {
        return getItems().stream()
                .filter(item -> item.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void save() {
        try {
            if (!themeDir.exists()) themeDir.mkdirs();

            JsonObject root = new JsonObject();
            for (CustomTheme theme : getItems()) {
                boolean isBuiltIn = false;
                for (Theme t : Theme.values()) {
                    if (t.name().equals(theme.name())) { isBuiltIn = true; break; }
                }
                if (isBuiltIn) continue;

                JsonObject colors = new JsonObject();
                colors.addProperty("primary", colorToHex(theme.primary()));
                colors.addProperty("secondary", colorToHex(theme.secondary()));
                root.add(theme.name(), colors);
            }

            Files.writeString(themesFile.toPath(), gson.toJson(root));
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public void load() {
        if (!themesFile.exists()) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(themesFile.toPath())).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String name = entry.getKey();
                JsonObject colors = entry.getValue().getAsJsonObject();
                Color primary = hexToColor(colors.get("primary").getAsString());
                Color secondary = hexToColor(colors.get("secondary").getAsString());

                CustomTheme existing = findTheme(name);
                if (existing != null) getItems().remove(existing);
                add(new CustomTheme(name, primary, secondary));
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void addCustomTheme(String name, Color primary, Color secondary) {
        CustomTheme existing = findTheme(name);
        if (existing != null) getItems().remove(existing);
        add(new CustomTheme(name, primary, secondary));
        save();
    }

    public void removeCustomTheme(String name) {
        for (Theme t : Theme.values()) {
            if (t.name().equals(name)) return;
        }
        CustomTheme theme = findTheme(name);
        if (theme == null) return;
        if (currentTheme.equals(theme)) setCurrentTheme(findTheme("Gothaj"));
        getItems().remove(theme);
        save();
    }

    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        return new Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16));
    }
}