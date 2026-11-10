package team.gothaj.client.api.util.other.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import systems.vajzii.irc.util.IRCUtil;
import team.gothaj.client.main.GothajClient;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class RememberedCredentials {

    private static final String HEADER = "GOTHAJ-CREDENTIALS-V1";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final Gson GSON = new Gson();

    private RememberedCredentials() {
    }

    public static File getFile() {
        return new File(GothajClient.getInstance().getClientDir(), "credentials.txt");
    }

    public static boolean exists() {
        return getFile().exists();
    }

    public static void save(String username, String password) {
        try {
            File file = getFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            root.addProperty("username", username);
            root.addProperty("password", password);

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(GSON.toJson(root).getBytes(StandardCharsets.UTF_8));

            String text = HEADER + System.lineSeparator()
                    + Base64.getEncoder().encodeToString(iv) + System.lineSeparator()
                    + Base64.getEncoder().encodeToString(encrypted);

            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
            deleteLegacyPlaintextAuth();
        } catch (Exception e) {
            System.err.println("Failed to save remembered credentials: " + e.getMessage());
        }
    }

    public static Credentials load() {
        try {
            File file = getFile();
            if (!file.exists()) {
                return null;
            }

            String[] lines = Files.readString(file.toPath(), StandardCharsets.UTF_8).split("\\R");
            if (lines.length < 3 || !HEADER.equals(lines[0])) {
                return null;
            }

            byte[] iv = Base64.getDecoder().decode(lines[1]);
            byte[] encrypted = Base64.getDecoder().decode(lines[2]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            String json = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String username = root.has("username") ? root.get("username").getAsString() : "";
            String password = root.has("password") ? root.get("password").getAsString() : "";

            if (username.isEmpty() || password.isEmpty()) {
                return null;
            }

            return new Credentials(username, password);
        } catch (Exception e) {
            System.err.println("Failed to load remembered credentials: " + e.getMessage());
            return null;
        }
    }

    public static void delete() {
        try {
            Files.deleteIfExists(getFile().toPath());
            deleteLegacyPlaintextAuth();
        } catch (Exception e) {
            System.err.println("Failed to delete remembered credentials: " + e.getMessage());
        }
    }

    public static void deleteLegacyPlaintextAuth() {
        try {
            Files.deleteIfExists(new File(GothajClient.getInstance().getClientDir(), "auth").toPath());
        } catch (Exception ignored) {
        }
    }

    private static byte[] key() throws Exception {
        String hardware;
        try {
            hardware = IRCUtil.getHardwareID();
        } catch (Exception e) {
            hardware = "unknown-hardware";
        }

        String material = hardware + "|" + System.getProperty("user.name", "") + "|"
                + System.getProperty("os.name", "") + "|" + GothajClient.getInstance().getName();
        return MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
    }

    public record Credentials(String username, String password) {
    }
}
