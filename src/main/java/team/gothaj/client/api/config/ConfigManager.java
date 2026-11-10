package team.gothaj.client.api.config;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.manager.ListManager;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.theme.CustomTheme;
import team.gothaj.client.api.theme.Theme;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.game.GameLoopEvent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.gui.config.ConfigMenu;
import team.gothaj.client.main.GothajClient;
import team.gothaj.cloud.client.Cloud;
import team.gothaj.cloud.share.packet.Packet;
import team.gothaj.eventbus.Listen;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Getter
@Setter
public class ConfigManager extends ListManager<Config> {

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final File configDir = new File(GothajClient.getInstance().getClientDir() + File.separator + "configs");
	private final File widgetDir = new File(GothajClient.getInstance().getClientDir() + File.separator + "widgets");
	private final File stateFile = new File(GothajClient.getInstance().getClientDir() + File.separator + "client-settings.json");

	private final HttpClient http = HttpClient.newHttpClient();

	private static final String UPLOAD_ENDPOINT = "https://forums.gothaj.top/upload_config.php";
	private static String UPLOAD_TOKEN;

	private static String SECRET_SALT;

	private TimerUtil saveTimer = new TimerUtil();

	private Config lastConfig = new Config("default");

	@Override
	public void init() {
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
		load();
		loadLastConfig();
		loadWidgets();
		super.init();
	}

	public void load() {
		try {
			if (!configDir.exists()) {
				configDir.mkdirs();
			}

			fetchOnlineConfigs();
			refreshLocal();
		} catch (Exception e) {
			Logger.clientChat("load() list error: " + e.getMessage());
		}
	}

	public void loadLastConfig() {
		Config config = new Config(getLastConfigProfile());
		lastConfig = config;
		ConfigMenu.selectedConfig = config;
		load(config, true);
	}

	public void saveActiveConfig() {
		Config config = ConfigMenu.selectedConfig != null ? ConfigMenu.selectedConfig : lastConfig;
		if (config == null || config.isOnline()) {
			config = new Config("default");
		}

		save(config);
		lastConfig = config;
		ConfigMenu.selectedConfig = config;
		saveLastConfigProfile(config.getProfile());
	}

	public void refreshLocal() {
		try {
			if (!configDir.exists()) {
				configDir.mkdirs();
			}

			File[] files = configDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
			if (files == null)
				return;

			for (File f : files) {
				String name = f.getName();
				String profile = name.substring(0, name.length() - ".json".length());

				boolean exists = getItems().stream().anyMatch(c -> c.getProfile().equals(profile) && !c.isOnline());

				if (exists)
					continue;

				String author = getAuthorFromConfig(new Config(profile));
				String saveDate = getSavedAtFromConfig(new Config(profile));

				Config cfg = new Config(profile, author, saveDate, Type.LOCAL);
				getItems().add(cfg);
			}
		} catch (Exception e) {
			Logger.clientChat("refreshLocal() error: " + e.getMessage());
		}
	}

	public synchronized void fetchOnlineConfigs() {
		String url = "https://forums.gothaj.top/list_configs.php";

		String key = null;

		try {
			key = Cloud.getInstance()
					.sendRequestSync(Packet.builder().type("GET_VALUE").putString("name", "FETCH_KEY").build(), 10000L)
					.getString("value");
		} catch (Exception e) {
			System.err.println("Failed to fetch fetch_key: " + e.getMessage());
			return;
		}

		try {
			String form = "key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
					.header("Content-Type", "application/x-www-form-urlencoded").timeout(Duration.ofSeconds(5))
					.POST(HttpRequest.BodyPublishers.ofString(form)).build();

			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				return;
			}

			JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

			if (!root.get("ok").getAsBoolean()) {
				return;
			}

			JsonArray users = root.getAsJsonArray("users");

			for (JsonElement userElement : users) {
				JsonObject user = userElement.getAsJsonObject();
				String username = user.get("username").getAsString();

				JsonArray configs = user.getAsJsonArray("configs");
				for (JsonElement configElement : configs) {
					JsonObject config = configElement.getAsJsonObject();
					String filename = config.get("filename").getAsString().split("\\.")[0];
					String configUrl = config.get("url").getAsString();
					String savedAt = config.get("saved_at").getAsString();

					boolean exists = getItems().stream().anyMatch(
							c -> c.getProfile().equals(filename) && c.isOnline() && c.getAuthor().equals(username));

					if (!exists) {
						Config cfg = new Config(filename, username, configUrl, savedAt);
						getItems().add(cfg);
					}
				}
			}

		} catch (Exception e) {
			System.err.println("Error fetching configs: " + e.getMessage());
		}
	}

	public void download(Config cfg) throws IOException, InterruptedException {
		if (!cfg.isOnline()) {
			GothajClient.getInstance().getNotificationManager().add("Config", "Cannot download local config",
					Notification.Type.ERROR);
			return;
		}

		if (cfg.getUrl() == null || cfg.getUrl().isEmpty()) {
			GothajClient.getInstance().getNotificationManager().add("Config",
					"URL is missing for config: " + cfg.getProfile(), Notification.Type.ERROR);
			return;
		}

		Path targetFile = configDir.toPath().resolve(cfg.getProfile() + ".json");

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(cfg.getUrl())).GET().build();

		HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());

		if (response.statusCode() != 200) {
			GothajClient.getInstance().getNotificationManager().add("Config", "Failed to download config",
					Notification.Type.ERROR);
			return;
		}

		try (InputStream inputStream = response.body()) {
			Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
			GothajClient.getInstance().getNotificationManager().add("Config", cfg.getProfile() + " has been downloaded",
					Notification.Type.SUCCESS);
		}

		getItems().removeIf(c -> c.getProfile().equals(cfg.getProfile()) && !c.isOnline());

		String author = getAuthorFromConfig(new Config(cfg.getProfile()));
		String saveDate = getSavedAtFromConfig(new Config(cfg.getProfile()));
		Config localConfig = new Config(cfg.getProfile(), author, saveDate, Type.LOCAL);
		getItems().add(localConfig);
	}

	public void save(Config config) {
		if (config == null || config.getProfile() == null || config.getProfile().isEmpty())
			return;

		try {
			if (!configDir.exists()) {
				configDir.mkdirs();
			}

			JsonObject modulesData = new JsonObject();
			JsonObject modulesObj = new JsonObject();
			for (Module m : GothajClient.getInstance().getModuleManager().getItems()) {
				try {
					JsonObject modJson = m.save();
					modulesObj.add(m.getName(), modJson);
				} catch (Exception ex) {
					Logger.clientChat("Failed to save module " + m.getName() + ": " + ex.getMessage());
				}
			}
			modulesData.add("modules", modulesObj);
			CustomTheme currentTheme = GothajClient.getInstance().getThemeManager().getCurrentTheme();
			modulesData.addProperty("theme", currentTheme.name());

			boolean isBuiltIn = false;
			for (Theme t : Theme.values()) {
				if (t.name().equals(currentTheme.name())) {
					isBuiltIn = true;
					break;
				}
			}

			if (!isBuiltIn) {
				modulesData.addProperty("themePrimary", String.format("#%02X%02X%02X", currentTheme.primary().getRed(), currentTheme.primary().getGreen(), currentTheme.primary().getBlue()));
				modulesData.addProperty("themeSecondary", String.format("#%02X%02X%02X", currentTheme.secondary().getRed(), currentTheme.secondary().getGreen(), currentTheme.secondary().getBlue()));
			}

			String modulesJson = gson.toJson(modulesData);

			byte[] dataBytes = modulesJson.getBytes(StandardCharsets.UTF_8);
			byte[] key = generateKey();

			if (key == null) {
				Logger.clientChat("Failed to save config: encryption key unavailable");
				return;
			}

			for (int i = 0; i < dataBytes.length; i++) {
				dataBytes[i] ^= key[i % key.length];
			}

			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream)) {
				gzipOut.write(dataBytes);
			}

			String dataB64 = Base64.getEncoder().encodeToString(byteStream.toByteArray());

			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
			String currentDate = dateFormat.format(new Date());

			JsonObject wrapper = new JsonObject();
			wrapper.addProperty("version", 2);
			wrapper.addProperty("profile", config.getProfile());
			wrapper.addProperty("author", GothajClient.getInstance().getUser().getUsername());
			wrapper.addProperty("saved_at", currentDate);
			CustomTheme currentThemeWrapper = GothajClient.getInstance().getThemeManager().getCurrentTheme();
			wrapper.addProperty("theme", currentThemeWrapper.name());

			boolean isBuiltInWrapper = false;
			for (Theme t : Theme.values()) {
				if (t.name().equals(currentThemeWrapper.name())) {
					isBuiltInWrapper = true;
					break;
				}
			}

			if (!isBuiltInWrapper) {
				wrapper.addProperty("themePrimary", String.format("#%02X%02X%02X", currentThemeWrapper.primary().getRed(), currentThemeWrapper.primary().getGreen(), currentThemeWrapper.primary().getBlue()));
				wrapper.addProperty("themeSecondary", String.format("#%02X%02X%02X", currentThemeWrapper.secondary().getRed(), currentThemeWrapper.secondary().getGreen(), currentThemeWrapper.secondary().getBlue()));
			}
			wrapper.addProperty("data", dataB64);

			Path file = Paths.get(configDir.getAbsolutePath(), config.getProfile() + ".json");
			try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(wrapper, w);
			}

			config.setSaveDate(currentDate);
			config.setAuthor(GothajClient.getInstance().getUser().getUsername());

			Config existing = getItems().stream()
					.filter(c -> c.getProfile().equals(config.getProfile()) && !c.isOnline()).findFirst().orElse(null);

			if (existing != null) {
				existing.setSaveDate(currentDate);
				existing.setAuthor(GothajClient.getInstance().getUser().getUsername());
			} else {
				getItems().add(new Config(config.getProfile(), GothajClient.getInstance().getUser().getUsername(),
						currentDate, Type.LOCAL));
			}

		} catch (Exception e) {
			Logger.clientChat("save error: " + e.getMessage());
		}
	}

	public void load(Config config, boolean keybinds) {
		try {
			if (config == null || config.getProfile() == null || config.getProfile().isEmpty() || config.isOnline())
				return;

			if(lastConfig == null || !lastConfig.getProfile().equals(config.getProfile())) {
				save(lastConfig);
				lastConfig = config;
			}

			Path file = Paths.get(configDir.getAbsolutePath(), config.getProfile() + ".json");
			if (!Files.exists(file))
				save(config);

			JsonObject root;
			JsonObject wrapper;

			try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				wrapper = JsonParser.parseReader(r).getAsJsonObject();
			}

			if (wrapper.has("version") && wrapper.get("version").getAsInt() == 2) {
				if (!wrapper.has("data")) {
					Logger.clientChat("Invalid config: missing data field");
					return;
				}

				if (wrapper.has("theme")) {
					String themeName = wrapper.get("theme").getAsString();
					CustomTheme theme = GothajClient.getInstance().getThemeManager().findTheme(themeName);
					if (theme == null && wrapper.has("themePrimary") && wrapper.has("themeSecondary")) {
						GothajClient.getInstance().getThemeManager().addCustomTheme(themeName,
								hexToColor(wrapper.get("themePrimary").getAsString()),
								hexToColor(wrapper.get("themeSecondary").getAsString()));
						theme = GothajClient.getInstance().getThemeManager().findTheme(themeName);
					}
					if (theme != null) {
						GothajClient.getInstance().getThemeManager().setCurrentTheme(theme);
					}
				}

				String dataB64 = wrapper.get("data").getAsString();

				try {
					byte[] compressed = Base64.getDecoder().decode(dataB64);

					ByteArrayInputStream byteIn = new ByteArrayInputStream(compressed);
					byte[] encrypted;
					try (GZIPInputStream gzipIn = new GZIPInputStream(byteIn)) {
						encrypted = gzipIn.readAllBytes();
					}

					byte[] keyBytes = generateKey();
					if (keyBytes == null) {
						Logger.clientChat("Failed to load config: encryption key unavailable");
						return;
					}
					for (int i = 0; i < encrypted.length; i++) {
						encrypted[i] ^= keyBytes[i % keyBytes.length];
					}

					String jsonString = new String(encrypted, StandardCharsets.UTF_8);
					root = JsonParser.parseString(jsonString).getAsJsonObject();

				} catch (Exception ex) {
					Logger.clientChat("Failed to decrypt config: " + ex.getMessage());
					return;
				}

			} else {
				root = wrapper;
				Logger.clientChat("§eWarning: Loading old format config");
			}

			if (root == null || !root.has("modules"))
				return;

			if (root.has("theme")) {
				String themeName = root.get("theme").getAsString();
				CustomTheme theme = GothajClient.getInstance().getThemeManager().findTheme(themeName);
				if (theme == null && root.has("themePrimary") && root.has("themeSecondary")) {
					GothajClient.getInstance().getThemeManager().addCustomTheme(themeName,
							hexToColor(root.get("themePrimary").getAsString()),
							hexToColor(root.get("themeSecondary").getAsString()));
					theme = GothajClient.getInstance().getThemeManager().findTheme(themeName);
				}
				if (theme != null) {
					GothajClient.getInstance().getThemeManager().setCurrentTheme(theme);
				}
			}

			JsonObject modulesObj = root.getAsJsonObject("modules");
			for (Module m : GothajClient.getInstance().getModuleManager().getItems()) {
				m.setToggled(false);

				JsonElement el = modulesObj.get(m.getName());
				if (el == null && m.getName().equalsIgnoreCase("Manager")) {
					el = modulesObj.get("AutoArmor");
				}
				if (el == null || !el.isJsonObject())
					continue;
				try {
					m.load(el.getAsJsonObject(), keybinds);
					if (m.getName().equalsIgnoreCase("Manager") && modulesObj.has("AutoArmor")) {
						JsonObject oldAutoArmor = modulesObj.getAsJsonObject("AutoArmor");
						if (oldAutoArmor.has("toggled") && oldAutoArmor.get("toggled").getAsBoolean()) {
							m.setToggled(true);
						}

						BooleanProperty autoArmor = m.getPropertyByName("Auto Armor");
						if (autoArmor != null) {
							autoArmor.setProperty(true);
						}
					}
				} catch (Exception ex) {
					Logger.clientChat("Failed to load module " + m.getName() + ": " + ex.getMessage());
				}
			}

			lastConfig = config;
			ConfigMenu.selectedConfig = config;
			saveLastConfigProfile(config.getProfile());
		} catch (Exception e) {
			Logger.clientChat("load error: " + e.getMessage());
		}
	}

	private String getLastConfigProfile() {
		try {
			if (!stateFile.exists()) {
				return "default";
			}

			try (BufferedReader r = Files.newBufferedReader(stateFile.toPath(), StandardCharsets.UTF_8)) {
				JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
				if (root.has("lastConfig")) {
					String profile = root.get("lastConfig").getAsString();
					if (profile != null && !profile.isBlank()) {
						return profile;
					}
				}
			}
		} catch (Exception e) {
			Logger.clientChat("Failed to read last config: " + e.getMessage());
		}
		return "default";
	}

	private void saveLastConfigProfile(String profile) {
		try {
			if (stateFile.getParentFile() != null) {
				stateFile.getParentFile().mkdirs();
			}

			JsonObject root = new JsonObject();
			root.addProperty("lastConfig", profile == null || profile.isBlank() ? "default" : profile);

			try (BufferedWriter w = Files.newBufferedWriter(stateFile.toPath(), StandardCharsets.UTF_8)) {
				gson.toJson(root, w);
			}
		} catch (Exception e) {
			Logger.clientChat("Failed to save last config: " + e.getMessage());
		}
	}

	private java.awt.Color hexToColor(String hex) {
		hex = hex.replace("#", "");
		return new java.awt.Color(
				Integer.parseInt(hex.substring(0, 2), 16),
				Integer.parseInt(hex.substring(2, 4), 16),
				Integer.parseInt(hex.substring(4, 6), 16));
	}

	public void remove(Config cfg) {
		File f = new File(configDir, cfg.getProfile() + ".json");
		if (f.exists()) {
			f.delete();
			getItems().remove(cfg);
		}
	}

	public void upload(Config cfg) {
		int uid;
		try {
			uid = Integer.parseInt(GothajClient.getInstance().getUser().getUid());
		} catch (Exception ex) {
			GothajClient.getInstance().getNotificationManager().add("Config", "Upload failed! Invalid UID",
					Notification.Type.ERROR);
			return;
		}
		String username = GothajClient.getInstance().getUser().getUsername();

		if (UPLOAD_TOKEN == null) {
			try {
				UPLOAD_TOKEN = Cloud.getInstance()
						.sendRequestSync(Packet.builder().type("GET_VALUE").putString("name", "UPLOAD_KEY").build(),
								10000L)
						.getString("value");
			} catch (Exception e) {
				System.err.println("Failed to fetch upload_key: " + e.getMessage());
			}
		}

		try {
			if (uid <= 0)
				throw new IllegalArgumentException("uid must be > 0");
			if (cfg == null || cfg.getProfile() == null || cfg.getProfile().isEmpty())
				throw new IllegalArgumentException("config/profile is empty");
			if (username == null || username.isEmpty())
				throw new IllegalArgumentException("username is empty");

			Path file = Paths.get(configDir.getAbsolutePath(), cfg.getProfile() + ".json");
			if (!Files.exists(file)) {
				GothajClient.getInstance().getNotificationManager().add("Config", "Upload failed! Config not found",
						Notification.Type.ERROR);
				return;
			}
			byte[] fileBytes = Files.readAllBytes(file);
			String fileName = file.getFileName().toString();

			String boundary = "----GothajBoundary" + System.currentTimeMillis();
			String CRLF = "\r\n";

			StringBuilder sb = new StringBuilder();

			sb.append("--").append(boundary).append(CRLF);
			sb.append("Content-Disposition: form-data; name=\"token\"").append(CRLF).append(CRLF);
			sb.append(UPLOAD_TOKEN).append(CRLF);

			sb.append("--").append(boundary).append(CRLF);
			sb.append("Content-Disposition: form-data; name=\"uid\"").append(CRLF).append(CRLF);
			sb.append(uid).append(CRLF);

			sb.append("--").append(boundary).append(CRLF);
			sb.append("Content-Disposition: form-data; name=\"username\"").append(CRLF).append(CRLF);
			sb.append(username).append(CRLF);

			sb.append("--").append(boundary).append(CRLF);
			sb.append("Content-Disposition: form-data; name=\"config\"; filename=\"").append(fileName).append("\"")
					.append(CRLF);
			sb.append("Content-Type: application/json; charset=utf-8").append(CRLF).append(CRLF);

			byte[] prefix = sb.toString().getBytes(StandardCharsets.UTF_8);
			byte[] suffix = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

			byte[] body = new byte[prefix.length + fileBytes.length + suffix.length];
			System.arraycopy(prefix, 0, body, 0, prefix.length);
			System.arraycopy(fileBytes, 0, body, prefix.length, fileBytes.length);
			System.arraycopy(suffix, 0, body, prefix.length + fileBytes.length, suffix.length);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UPLOAD_ENDPOINT))
					.header("Content-Type", "multipart/form-data; boundary=" + boundary)
					.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();

			HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (resp.statusCode() != 200) {
				System.out.println("Upload failed! " + resp.statusCode() + " | " + resp.body());
				GothajClient.getInstance().getNotificationManager().add("Config",
						"Upload failed! " + resp.statusCode() + " | " + resp.body(), Notification.Type.ERROR);
				return;
			}

			try {
				JsonObject out = JsonParser.parseString(resp.body()).getAsJsonObject();
				boolean ok = out.has("ok") && out.get("ok").getAsBoolean();
				if (!ok) {
					String err = out.has("error") ? out.get("error").getAsString() : "unknown";
					GothajClient.getInstance().getNotificationManager().add("Config", "Upload failed! " + err,
							Notification.Type.ERROR);
					return;
				}
				GothajClient.getInstance().getNotificationManager().add("Config",
						cfg.getProfile() + " has been uploaded", Notification.Type.SUCCESS);
			} catch (Exception parseEx) {
				GothajClient.getInstance().getNotificationManager().add("Config",
						cfg.getProfile() + " has been uploaded", Notification.Type.ERROR);
			}
		} catch (Exception e) {
			GothajClient.getInstance().getNotificationManager().add("Config", "Upload failed!",
					Notification.Type.ERROR);
		}
	}

	private String getSavedAtFromConfig(Config config) {
		try {
			File configFile = new File(configDir, config.getProfile() + ".json");

			if (!configFile.exists()) {
				return "";
			}

			try (FileReader reader = new FileReader(configFile)) {
				JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
				if (jsonObject.has("saved_at")) {
					return jsonObject.get("saved_at").getAsString();
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to read saved_at from config: " + e.getMessage());
		}
		return "";
	}

	private String getAuthorFromConfig(Config config) {
		try {
			File configFile = new File(configDir, config.getProfile() + ".json");

			if (!configFile.exists()) {
				return "";
			}

			try (FileReader reader = new FileReader(configFile)) {
				JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
				if (jsonObject.has("author")) {
					return jsonObject.get("author").getAsString();
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to read author from config: " + e.getMessage());
		}
		return "";
	}

	private byte[] generateKey() {
		try {
			if (SECRET_SALT == null) {
				SECRET_SALT = Cloud.getInstance()
						.sendRequestSync(Packet.builder().type("GET_VALUE").putString("name", "CFG_SALT").build(),
								10000L)
						.getString("value");
			}
		} catch (Exception e) {
			System.out.println("Failed to generate secret salt: " + e.getMessage());
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(SECRET_SALT.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			return SECRET_SALT.getBytes(StandardCharsets.UTF_8);
		}
	}

	public void saveWidgets() {
		try {
			if (!widgetDir.exists()) {
				widgetDir.mkdirs();
			}

			JsonObject root = new JsonObject();

			JsonObject widgetsObj = new JsonObject();
			for (Widget w : GothajClient.getInstance().getWidgetManager().getItems()) {
				try {
					JsonObject modJson = w.save();
					widgetsObj.add(w.getLabel(), modJson);
				} catch (Exception ex) {
					Logger.clientChat("Failed to save widget " + w.getLabel() + ": " + ex.getMessage());
				}
			}
			root.add("widgets", widgetsObj);

			Path file = Paths.get(widgetDir.getAbsolutePath(), "widgets.json");
			try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(root, w);
			}
		} catch (Exception e) {
			Logger.clientChat("save error: " + e.getMessage());
		}
	}

	public void loadWidgets() {
		try {
			Path file = Paths.get(widgetDir.getAbsolutePath(), "widgets.json");
			if (!Files.exists(file))
				saveWidgets();

			JsonObject root;
			try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				root = JsonParser.parseReader(r).getAsJsonObject();
			}
			if (root == null || !root.has("widgets"))
				return;

			JsonObject widgetsObj = root.getAsJsonObject("widgets");
			for (Widget w : GothajClient.getInstance().getWidgetManager().getItems()) {
				JsonElement el = widgetsObj.get(w.getLabel());
				if (el == null || !el.isJsonObject())
					continue;
				try {
					w.load(el.getAsJsonObject());
				} catch (Exception ex) {
					Logger.clientChat("Failed to load module " + w.getLabel() + ": " + ex.getMessage());
				}
			}
		} catch (Exception e) {
			Logger.clientChat("load error: " + e.getMessage());
		}
	}

	public enum Type {
		LOCAL, ONLINE
	}

	public boolean exists(String name) {
		return Files.exists(Path.of(configDir.getPath()).resolve(name + ".json"));
	}

	public void delete(String name) {
		new File(configDir, name + ".json").delete();
	}
}
