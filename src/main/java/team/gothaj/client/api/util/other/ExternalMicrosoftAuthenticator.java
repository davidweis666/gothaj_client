package team.gothaj.client.api.util.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.main.GothajClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ExternalMicrosoftAuthenticator {

    private final HttpClient httpClient;
    private final AtomicReference<LoginState> loginState = new AtomicReference<>();

    public ExternalMicrosoftAuthenticator() {
        this.httpClient = MinecraftAuth.createHttpClient("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
    }

    public DeviceCodeLogin startDeviceCodeLogin() throws Exception {
        CompletableFuture<MsaDeviceCode> codeFuture = new CompletableFuture<>();
        CompletableFuture<MsaToken> tokenFuture = new CompletableFuture<>();

        LoginState newState = new LoginState(codeFuture, tokenFuture);
        if (!loginState.compareAndSet(null, newState)) {
            throw new IllegalStateException("A device code login is already in progress");
        }

        Thread thread = new Thread(() -> {
            try {
                DeviceCodeMsaAuthService authService = new DeviceCodeMsaAuthService(
                        httpClient,
                        new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH),
                        codeFuture::complete
                );
                tokenFuture.complete(authService.acquireToken());
            } catch (Throwable t) {
                codeFuture.completeExceptionally(t);
                tokenFuture.completeExceptionally(t);
            }
        }, "minecraftauth-device-code-login");

        thread.setDaemon(true);
        thread.start();

        MsaDeviceCode codeInfo;
        try {
            codeInfo = codeFuture.get();
        } catch (ExecutionException e) {
            loginState.compareAndSet(newState, null);
            throw unwrap(e);
        }

        return new DeviceCodeLogin(
                codeInfo.getUserCode(),
                codeInfo.getDirectVerificationUri(),
                buildMessage(codeInfo),
                codeInfo.getExpireTimeMs(),
                codeInfo.getIntervalMs()
        );
    }


    public AuthResult awaitDeviceCodeLogin(DeviceCodeLogin loginInfo) throws Exception {
        if (loginInfo == null) {
            throw new IllegalArgumentException("loginInfo");
        }

        LoginState state = loginState.get();
        if (state == null) {
            throw new IllegalStateException("No device code login is in progress");
        }

        try {
            MsaToken msaToken = state.tokenFuture().get();
            JavaAuthManager authManager = JavaAuthManager.create(httpClient).login(msaToken);

            MinecraftProfile minecraftProfile = authManager.getMinecraftProfile().getUpToDate();
            MinecraftToken minecraftToken = authManager.getMinecraftToken().getUpToDate();
            MsaToken upToDateMsaToken = authManager.getMsaToken().getCached();

            return new AuthResult(
                    minecraftProfile.getName(),
                    minecraftProfile.getId(),
                    minecraftToken.getToken(),
                    upToDateMsaToken != null ? upToDateMsaToken.getRefreshToken() : null,
                    JavaAuthManager.toJson(authManager).toString());
        } catch (ExecutionException e) {
            throw unwrap(e);
        } finally {
            loginState.compareAndSet(state, null);
        }
    }

    public static String[] getProfileInfo(String token) {
        return getProfileInfo(token, true);
    }

    public static String[] getProfileInfo(String token, boolean notifyError) {
        try {
            final URL url = new URI("https://api.minecraftservices.com/minecraft/profile").toURL();
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String response = reader.lines().collect(Collectors.joining());
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                if (!json.has("id") || !json.has("name")) {
                    String error = json.has("error")
                            ? json.get("error").getAsString() + ": " + json.get("errorMessage").getAsString()
                            : "Missing profile fields in response.";
                    throw new Exception(error);
                }

                return new String[]{
                        json.get("name").getAsString(),
                        json.get("id").getAsString()
                };
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            if (notifyError) {
                e.printStackTrace(System.err);
                GothajClient.getInstance().getNotificationManager().add("Account", "Invalid access token", Notification.Type.ERROR);
            }
        }
        return null;
    }

    public AuthResult loginWithRefreshToken(String refreshToken) throws Exception {
        JavaAuthManager authManager = JavaAuthManager.create(httpClient).login(refreshToken);

        MinecraftProfile minecraftProfile = authManager.getMinecraftProfile().getUpToDate();
        MinecraftToken minecraftToken = authManager.getMinecraftToken().getUpToDate();
        MsaToken upToDateMsaToken = authManager.getMsaToken().getCached();

        return new AuthResult(
                minecraftProfile.getName(),
                minecraftProfile.getId(),
                minecraftToken.getToken(),
                upToDateMsaToken != null ? upToDateMsaToken.getRefreshToken() : refreshToken,
                JavaAuthManager.toJson(authManager).toString());
    }


    private static String buildMessage(MsaDeviceCode codeInfo) {
        return "Go to " + codeInfo.getVerificationUri() + " and enter code " + codeInfo.getUserCode();
    }

    private static Exception unwrap(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof Exception ex) {
            return ex;
        }
        return new RuntimeException(cause);
    }

    private record LoginState(CompletableFuture<MsaDeviceCode> codeFuture, CompletableFuture<MsaToken> tokenFuture) {}

    public record AuthResult(String username, UUID uuid, String accessToken, String refreshToken, String serializedSession) {}

    public record DeviceCodeLogin(
            String userCode,
            String verificationUri,
            String message,
            long expiresIn,
            long interval) {

    }
}
