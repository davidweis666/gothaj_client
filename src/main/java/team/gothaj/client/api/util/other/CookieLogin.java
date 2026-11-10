package team.gothaj.client.api.util.other;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import team.gothaj.client.api.account.Account;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

public class CookieLogin {
    private final static Gson gson = new Gson();
    private final static String useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";

    public static Account loginWithCookie(File cookieFile) {
        try {
            final String[] cookiesText = FileUtils.readFileToString(cookieFile).split("\n");

            final StringBuilder sb = new StringBuilder();

            for (final String cookie : cookiesText) {
                String[] columns = cookie.split("\t");
                if (columns.length < 7) continue;
                final String name = columns[5].trim();
                final String value = columns[6].trim();
                sb.append(name).append("=").append(value).append("; ");
            }

            final String cookie = sb.toString();

            final String loc1 = getNextLocation(new URI("https://sisu.xboxlive.com/connect/XboxLive/?state=login&cobrandId=8058f65d-ce06-4c30-9559-473c9275a65d&tid=896928775&ru=https%3A%2F%2Fwww.minecraft.net%2Fen-us%2Flogin&aid=1142970254").toURL(), "PHPSESSID=0");
            System.out.println("[CookieLogin] loc1: " + loc1);
            if (loc1 == null) { System.err.println("[CookieLogin] FAILED at loc1"); return null; }

            final String loc2 = getNextLocation(new URI(loc1.replaceAll(" ", "%20")).toURL(), cookie);
            System.out.println("[CookieLogin] loc2: " + loc2);
            if (loc2 == null) { System.err.println("[CookieLogin] FAILED at loc2"); return null; }

            final String loc3 = getNextLocation(new URI(loc2.replaceAll(" ", "%20")).toURL(), cookie);
            System.out.println("[CookieLogin] loc3: " + loc3);
            if (loc3 == null || !loc3.contains("accessToken=")) {
                System.err.println("[CookieLogin] FAILED at loc3 - value: " + loc3);
                return null;
            }

            final String accessToken = loc3.split("accessToken=")[1];
            System.out.println("[CookieLogin] accessToken present: " + !accessToken.isEmpty());

            final String decoded = new String(Base64.getDecoder().decode(accessToken), StandardCharsets.UTF_8)
                    .split("\"rp://api.minecraftservices.com/\",")[1];

            final String token = decoded.split("\"Token\":\"")[1].split("\"")[0];
            final String uhs = decoded.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
            final String xbl = "XBL3.0 x=" + uhs + ";" + token;

            System.out.println("[CookieLogin] calling login_with_xbox...");
            final String output = postExternal("https://api.minecraftservices.com/authentication/login_with_xbox", "{\"identityToken\":\"" + xbl + "\",\"ensureLegacyEnabled\":true}", true);
            System.out.println("[CookieLogin] login_with_xbox response: " + output);
            if (output == null) { System.err.println("[CookieLogin] FAILED - login_with_xbox null"); return null; }

            final String mcToken = gson.fromJson(output, JsonObject.class).get("access_token").getAsString();
            System.out.println("[CookieLogin] mcToken present: " + (mcToken != null && !mcToken.isEmpty()));

            final HttpsURLConnection connection = (HttpsURLConnection) new URI("https://api.minecraftservices.com/minecraft/profile").toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + mcToken);

            final JsonObject profileResponse = gson.fromJson(IOUtils.toString(connection.getInputStream()), JsonObject.class);
            System.out.println("[CookieLogin] profile response: " + profileResponse);

            long expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
            return new Account(profileResponse.get("name").getAsString(),
                    UUID.fromString(profileResponse.get("id").getAsString().replaceFirst(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")),
                    null,
                    mcToken,
                    null,
                    false,
                    expiresAt);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static String getNextLocation(URL url, String cookie) throws IOException {
        final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        connection.setRequestProperty("Accept-Encoding", "");
        connection.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
        connection.setRequestProperty("Cookie", cookie);
        connection.setRequestProperty("User-Agent", useragent);
        connection.setInstanceFollowRedirects(false);
        connection.connect();
        return connection.getHeaderField("Location");
    }

    public static String postExternal(final String url, final String post, final boolean json) {
        try {
            final HttpsURLConnection connection = connect(url, post, json);
            final int responseCode = connection.getResponseCode();
            final InputStream stream = responseCode / 100 == 2 || responseCode / 100 == 3 ? connection.getInputStream() : connection.getErrorStream();

            if (stream == null) {
                System.err.println(responseCode + ": " + url);
                return null;
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String lineBuffer;
            final StringBuilder response = new StringBuilder();
            while ((lineBuffer = reader.readLine()) != null) {
                response.append(lineBuffer);
            }
            reader.close();
            return response.toString();
        } catch (final Exception exception) {
            exception.printStackTrace(System.err);
            return null;
        }
    }

    @NotNull
    private static HttpsURLConnection connect(String url, String post, boolean json) throws IOException, URISyntaxException {
        final HttpsURLConnection connection = (HttpsURLConnection) new URI(url).toURL().openConnection();
        connection.addRequestProperty("User-Agent", useragent);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        final byte[] out = post.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(out.length);
        connection.addRequestProperty("Content-Type", json ? "application/json" : "application/x-www-form-urlencoded; charset=UTF-8");
        connection.addRequestProperty("Accept", "application/json");
        connection.connect();
        try (final OutputStream os = connection.getOutputStream()) {
            os.write(out);
        }
        return connection;
    }
}