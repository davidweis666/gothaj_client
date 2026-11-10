package team.gothaj.client.api.proxy;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import team.gothaj.client.api.account.Account;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.main.GothajClient;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProxyRepository extends ListManager<Proxy> {

    private final File proxiesFile = new File(GothajClient.getInstance().getClientDir() + File.separator + "proxies.json");

    @Setter
    @Getter
    private Proxy currentProxy;

    @Override
    public void init() {
        load();
        super.init();
    }

    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(proxiesFile)) {
            gson.toJson(items, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void load() {
        if (!proxiesFile.exists()) {
            try {
                proxiesFile.getParentFile().mkdirs();
                save();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            return;
        }

        Gson gson = new Gson();
        try (Reader reader = new FileReader(proxiesFile)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (jsonElement.isJsonArray()) {
                Proxy[] loaded = gson.fromJson(jsonElement, Proxy[].class);
                items.clear();
                if (loaded != null) {
                    Collections.addAll(items, loaded);
                }
            } else if (jsonElement.isJsonObject()) {
                Logger.log("proxies.json has wrong format (object instead of array). Resetting to empty array.");
                items.clear();
                save();
            } else {
                Logger.log("proxies.json has unknown format. Resetting to empty array.");
                items.clear();
                save();
            }
        } catch (JsonSyntaxException | JsonIOException e) {
            Logger.log("Failed to parse proxies.json: " + e.getMessage());
            Logger.log("Backing up corrupted file and creating new one.");

            try {
                File backup = new File(proxiesFile.getAbsolutePath() + ".backup");
                if (proxiesFile.renameTo(backup)) {
                    Logger.log("Corrupted file backed up to: " + backup.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }

            items.clear();
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Type {
        SOCKS5("Socks 5"),
        SOCKS5_PASSWORDLESS("Socks 5 (no auth)");

        @Getter
        private final String name;

        Type(String name) {
            this.name = name;
        }
    }

}
