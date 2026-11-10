package team.gothaj.client.impl.gui.account.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.account.Account;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.CookieLogin;
import team.gothaj.client.api.util.other.ExternalMicrosoftAuthenticator.AuthResult;
import team.gothaj.client.api.util.other.ExternalMicrosoftAuthenticator;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.account.AccountMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static team.gothaj.client.api.util.other.MouseUtil.isHovered;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class AddAccountMenu extends GuiScreen {

    private static final char[] RANDOM_USERNAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".toCharArray();

    private GuiTextField usernameField;
    private float animO = 1.0f;
    private float animW = 1.0f;
    private float animT = 1.0f;
    private float animC = 1.0f;

    private int loginMethod = 0;

    private final boolean directLogin;
    private volatile boolean waitingForExternalWebResponse;

    private volatile String deviceUserCode = null;
    private volatile String deviceVerificationUri = null;

    public AddAccountMenu() {
        directLogin = false;
    }

    public AddAccountMenu(boolean directLogin) {
        this.directLogin = directLogin;
    }

    @Override
    public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);

        this.buttonList.clear();
        if (loginMethod == 0) {
            this.buttonList.add(new GuiButton(1, sr.getScaledWidth() / 2 - 60, sr.getScaledHeight() / 2 + 20, 120, 24, "Add Account"));
            this.buttonList.add(new GuiButton(3, sr.getScaledWidth() / 2 - 60, sr.getScaledHeight() / 2 + 52, 120, 24, "Generate Random"));
        } else {
            this.buttonList.add(new GuiButton(1, sr.getScaledWidth() / 2 - 60, sr.getScaledHeight() / 2 - 12, 120, 24,
                    loginMethod == 1
                            ? (waitingForExternalWebResponse && deviceUserCode != null && deviceVerificationUri != null ? "Code: "  + deviceUserCode : "Open Browser")
                            : loginMethod == 2 ? "Paste Token" : "Select A File"));
        }
        this.buttonList.add(new GuiButton(2, sr.getScaledWidth() - 30, 10, 20, 20, "X"));

        usernameField = new GuiTextField(0, mc.fontRendererObj, sr.getScaledWidth() / 2 - 80, sr.getScaledHeight() / 2 - 12, 160, 24);
        usernameField.setMaxStringLength(32);

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        drawDefaultBackground();

        int panelWidth = 140, panelHeight = 35;
        int panelX = (width - panelWidth) / 2;
        int panelY = 10;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(panelX, panelY, panelWidth, panelHeight, 14, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(panelX, panelY, panelWidth, panelHeight, 14, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(panelX, panelY, panelWidth, panelHeight, 14, new Color(20, 20, 20, 100));

        int iconSize = 30;
        int iconPadding = 3;
        int firstIconX = panelX + iconPadding + 6;
        int iconY = panelY + panelHeight / 3;

        int oX = firstIconX + iconPadding + 5;
        int wX = firstIconX + (iconSize + iconPadding) + 16;
        int tX = firstIconX + 2 * (iconSize + iconPadding) + 11;
        int cX = firstIconX + 3 * (iconSize + iconPadding) + 11;

        float targetScale = 1.08f;
        float animSpeed = 0.05f;

        RoundedUtil.drawRound(oX + 19, panelY + 10, 1, panelHeight - 20, 2f, new Color(255, 255, 255, 70));

        boolean hoverO = isHovered(mouseX, mouseY, oX - iconSize / 2f, iconY - iconSize / 2f, oX + iconSize / 2f, iconY + iconSize / 2f);
        animO += ((hoverO ? targetScale : 1.0f) - animO) * animSpeed;
        GL11.glPushMatrix();
        GL11.glTranslatef(oX, iconY, 0);
        GL11.glScalef(animO, animO, 1.0f);
        GL11.glTranslatef(-oX, -iconY, 0);
        Fonts.login36.drawCenteredString("O", oX, iconY, loginMethod == 0 ? new Color(170, 0, 170).getRGB() : -1);
        GL11.glPopMatrix();

        boolean hoverW = isHovered(mouseX, mouseY, wX - iconSize / 2f, iconY - iconSize / 2f, wX + iconSize / 2f, iconY + iconSize / 2f);
        animW += ((hoverW ? targetScale : 1.0f) - animW) * animSpeed;
        GL11.glPushMatrix();
        GL11.glTranslatef(wX, iconY, 0);
        GL11.glScalef(animW, animW, 1.0f);
        GL11.glTranslatef(-wX, -iconY, 0);
        Fonts.login36.drawCenteredString("W", wX, iconY, loginMethod == 1 ? new Color(170, 0, 170).getRGB() : -1);
        GL11.glPopMatrix();

        boolean hoverT = isHovered(mouseX, mouseY, tX - iconSize / 2f, iconY - iconSize / 2f, tX + iconSize / 2f, iconY + iconSize / 2f);
        animT += ((hoverT ? targetScale : 1.0f) - animT) * animSpeed;
        GL11.glPushMatrix();
        GL11.glTranslatef(tX, iconY, 0);
        GL11.glScalef(animT, animT, 1.0f);
        GL11.glTranslatef(-tX, -iconY, 0);
        Fonts.login36.drawCenteredString("T", tX, iconY, loginMethod == 2 ? new Color(170, 0, 170).getRGB() : -1);
        GL11.glPopMatrix();

        boolean hoverC = isHovered(mouseX, mouseY, cX - iconSize / 2f, iconY - iconSize / 2f, cX + iconSize / 2f, iconY + iconSize / 2f);
        animC += ((hoverC ? targetScale : 1.0f) - animC) * animSpeed;
        GL11.glPushMatrix();
        GL11.glTranslatef(cX, iconY, 0);
        GL11.glScalef(animC, animC, 1.0f);
        GL11.glTranslatef(-cX, -iconY, 0);
        Fonts.login36.drawCenteredString("C", cX, iconY, loginMethod == 3 ? new Color(170, 0, 170).getRGB() : -1);
        GL11.glPopMatrix();

        RenderUtil.resetColor();

        ScaledResolution sr = new ScaledResolution(mc);

        if (loginMethod == 0) {
            usernameField.setPlaceholder("Username");
            usernameField.drawTextBox();
        }

        String currentType = loginMethod == 0 ? "Offline Login" :
                loginMethod == 1 ? "Web Login" :
                loginMethod == 2 ? "Token Login" :
                "Cookie Login";

        Fonts.surf20.drawCenteredStringWithShadow(currentType, sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f - Fonts.surf20.getHeight() - 20, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        int panelWidth = 140, panelHeight = 35;
        int panelX = (width - panelWidth) / 2;
        int panelY = 10;

        int iconSize = 30;
        int iconPadding = 3;
        int firstIconX = panelX + iconPadding + 6;
        int iconY = panelY + panelHeight / 3;

        int oX = firstIconX + iconPadding + 5;
        int wX = firstIconX + (iconSize + iconPadding) + 16;
        int tX = firstIconX + 2 * (iconSize + iconPadding) + 11;
        int cX = firstIconX + 3 * (iconSize + iconPadding) + 11;

        if (mouseY >= iconY - iconSize / 2f && mouseY <= iconY + iconSize / 2f) {
            if (mouseX >= oX - iconSize / 2f && mouseX <= oX + iconSize / 2f) {
                loginMethod = 0; initGui();
            } else if (mouseX >= wX - iconSize / 2f && mouseX <= wX + iconSize / 2f) {
                loginMethod = 1; initGui();
            } else if (mouseX >= tX - iconSize / 2f && mouseX <= tX + iconSize / 2f) {
                loginMethod = 2; initGui();
            } else if (mouseX >= cX - iconSize / 2f && mouseX <= cX + iconSize / 2f) {
                loginMethod = 3; initGui();
            }
        }

        if (loginMethod == 0) {
            usernameField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (loginMethod == 0) {
            usernameField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {

            if (loginMethod == 0 && !usernameField.getText().trim().isEmpty()) {
                Account offline = new Account(usernameField.getText().trim(), true);
                if (!directLogin) GothajClient.getInstance().getAccountRepository().add(offline);
                GothajClient.getInstance().getAccountRepository().setCurrentAccount(offline);
                GothajClient.getInstance().getIrcManager().getClient().setIngameName(offline.getUsername());
                GothajClient.getInstance().getNotificationManager().add("Account", (directLogin ? "Logged in as " : "Account added: ") + offline.getUsername(), Notification.Type.SUCCESS);
                mc.session = new Session(offline.getUsername(), offline.getUuid().toString(), offline.getAccessToken(), "legacy");
                mc.displayGuiScreen(new AccountMenu());

            } else if (loginMethod == 1) {
                if (waitingForExternalWebResponse) {
                    GothajClient.getInstance().getNotificationManager().add("Account", "Login is already in progress", Notification.Type.INFO);
                    return;
                }

                new Thread(() -> {
                    ExternalMicrosoftAuthenticator authenticator = new ExternalMicrosoftAuthenticator();
                    try {
                        waitingForExternalWebResponse = true;
                        deviceUserCode = null;
                        deviceVerificationUri = null;
                        mc.addScheduledTask(this::initGui);

                        ExternalMicrosoftAuthenticator.DeviceCodeLogin login = authenticator.startDeviceCodeLogin();

                        deviceUserCode = login.userCode();
                        deviceVerificationUri = login.verificationUri();

                        initGui();

                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI(login.verificationUri()));
                        }

                        if (deviceUserCode != null) {
                            setClipboardString(deviceUserCode);
                            GothajClient.getInstance().getNotificationManager().add(
                                    "Account",
                                    "Device login initiated. Code: " + deviceUserCode + " (copied to clipboard)",
                                    Notification.Type.INFO
                            );
                        }

                        AuthResult result = authenticator.awaitDeviceCodeLogin(login);

                        long expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
                        Account microsoft = new Account(
                                result.username(),
                                result.uuid(),
                                null,
                                result.accessToken(),
                                result.refreshToken(),
                                false,
                                expiresAt
                        );

                        if (!directLogin) GothajClient.getInstance().getAccountRepository().add(microsoft);
                        GothajClient.getInstance().getAccountRepository().setCurrentAccount(microsoft);
                        mc.session = new Session(microsoft.getUsername(), microsoft.getUuid().toString().replace("-", ""), microsoft.getAccessToken(), "msa");
                        GothajClient.getInstance().getIrcManager().getClient().setIngameName(microsoft.getUsername());
                        GothajClient.getInstance().getNotificationManager().add(
                                "Account",
                                (directLogin ? "Logged in as " : "Account added: ") + microsoft.getUsername(),
                                Notification.Type.SUCCESS
                        );
                        mc.addScheduledTask(() -> mc.displayGuiScreen(new AccountMenu()));

                    } catch (Exception e) {
                        GothajClient.getInstance().getNotificationManager().add(
                                "Account",
                                "Login failed: " + e.getMessage(),
                                Notification.Type.ERROR
                        );

                        System.err.println(e.getMessage());
                    } finally {
                        waitingForExternalWebResponse = false;
                        deviceUserCode = null;
                        deviceVerificationUri = null;
                        mc.addScheduledTask(this::initGui);
                    }
                }).start();

            } else if (loginMethod == 2) {
                new Thread(() -> {
                    String clip = getClipboardText();
                    if (clip != null && !clip.isEmpty()) {
                        String accessToken = extractTokenValue(clip, "mctoken:", "access_token:", "accessToken:");
                        String refreshToken = extractTokenValue(clip, "refreshtoken:", "refresh_token:", "refreshToken:");
                        if (accessToken == null) accessToken = findAccessToken(clip);
                        if (refreshToken == null) refreshToken = findRefreshToken(clip);
                        String rawToken = clip.trim();
                        if (accessToken == null && refreshToken == null) {
                            accessToken = rawToken;
                        }

                        Account microsoft = null;
                        if (accessToken != null) {
                            String[] info = ExternalMicrosoftAuthenticator.getProfileInfo(accessToken, false);
                            if (info != null) {
                                long expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
                                microsoft = new Account(
                                        info[0],
                                        UUID.fromString(info[1].replaceFirst(
                                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")),
                                        null,
                                        accessToken,
                                        refreshToken,
                                        false,
                                        expiresAt
                                );
                            }
                        }

                        if (microsoft == null) {
                            String tokenToRefresh = refreshToken != null ? refreshToken : rawToken;
                            try {
                                AuthResult result = new ExternalMicrosoftAuthenticator().loginWithRefreshToken(tokenToRefresh);
                                long expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
                                microsoft = new Account(
                                        result.username(),
                                        result.uuid(),
                                        null,
                                        result.accessToken(),
                                        result.refreshToken(),
                                        false,
                                        expiresAt
                                );
                            } catch (Exception e) {
                                GothajClient.getInstance().getNotificationManager().add("Account", "Invalid access or refresh token", Notification.Type.ERROR);
                                System.err.println(e.getMessage());
                                return;
                            }
                        }

                        loginMicrosoftAccount(microsoft);
                    }
                }).start();

            } else if (loginMethod == 3) {
                new Thread(() -> {
                    final String directory = Paths.get(System.getProperty("user.home"), "Downloads").toString();
                    final FileDialog fileChooser = new FileDialog((Frame) null, "Select Cookie File", FileDialog.LOAD);
                    fileChooser.setFilenameFilter((ignored, name) -> name.endsWith(".txt"));
                    fileChooser.setDirectory(directory);
                    fileChooser.setVisible(true);
                    fileChooser.setAlwaysOnTop(true);

                    final String fileDirectory = fileChooser.getDirectory();
                    final String fileName = fileChooser.getFile();

                    if (fileDirectory != null && fileName != null) {
                        final File selectedFile = new File(fileDirectory, fileName);
                        if (selectedFile.isFile() && !selectedFile.isDirectory()) {
                            Account microsoft = CookieLogin.loginWithCookie(selectedFile);
                            if (microsoft != null) {
                                if (!directLogin) GothajClient.getInstance().getAccountRepository().add(microsoft);
                                GothajClient.getInstance().getAccountRepository().setCurrentAccount(microsoft);
                                mc.session = new Session(microsoft.getUsername(), microsoft.getUuid().toString().replace("-", ""), microsoft.getAccessToken(), "msa");
                                GothajClient.getInstance().getIrcManager().getClient().setIngameName(microsoft.getUsername());
                                GothajClient.getInstance().getNotificationManager().add("Account", (directLogin ? "Logged in as " : "Account added: ") + microsoft.getUsername(), Notification.Type.SUCCESS);
                                mc.addScheduledTask(() -> mc.displayGuiScreen(new AccountMenu()));
                            } else {
                                GothajClient.getInstance().getNotificationManager().add("Account", "Failed to login", Notification.Type.ERROR);
                            }
                        } else {
                            GothajClient.getInstance().getNotificationManager().add("Account", "No file selected", Notification.Type.ERROR);
                        }
                    }
                }).start();

            } else {
                Logger.log("error");
            }

        } else if (button.id == 2) {
            mc.displayGuiScreen(new AccountMenu());
        } else if (button.id == 3) {
            String randomUsername = generateOpsec();
            Account offline = new Account(randomUsername, true);
            if (!directLogin) GothajClient.getInstance().getAccountRepository().add(offline);
            GothajClient.getInstance().getAccountRepository().setCurrentAccount(offline);
            GothajClient.getInstance().getIrcManager().getClient().setIngameName(offline.getUsername());
            GothajClient.getInstance().getNotificationManager().add("Account", (directLogin ? "Logged in as " : "Account added: ") + offline.getUsername(), Notification.Type.SUCCESS);
            mc.session = new Session(offline.getUsername(), offline.getUuid().toString(), offline.getAccessToken(), "legacy");
            mc.displayGuiScreen(new AccountMenu());

        }
    }

    private void loginMicrosoftAccount(Account microsoft) {
        if (!directLogin) GothajClient.getInstance().getAccountRepository().add(microsoft);
        GothajClient.getInstance().getAccountRepository().setCurrentAccount(microsoft);
        mc.session = new Session(microsoft.getUsername(), microsoft.getUuid().toString().replace("-", ""), microsoft.getAccessToken(), "msa");
        GothajClient.getInstance().getIrcManager().getClient().setIngameName(microsoft.getUsername());
        GothajClient.getInstance().getNotificationManager().add("Account", (directLogin ? "Logged in as " : "Account added: ") + microsoft.getUsername(), Notification.Type.SUCCESS);
        mc.addScheduledTask(() -> mc.displayGuiScreen(new AccountMenu()));
    }

    private static String extractTokenValue(String source, String... labels) {
        String text = source.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        for (String label : labels) {
            String lowerLabel = label.toLowerCase(Locale.ROOT);
            int start = lower.indexOf(lowerLabel);
            if (start == -1) {
                continue;
            }

            String value = text.substring(start + label.length()).trim();
            int pipe = value.indexOf('|');
            int newline = value.indexOf('\n');
            int end = -1;
            if (pipe >= 0) end = pipe;
            if (newline >= 0) end = end == -1 ? newline : Math.min(end, newline);
            if (end >= 0) value = value.substring(0, end);
            value = value.trim();
            return value.isEmpty() ? null : value;
        }

        return null;
    }

    private static String findRefreshToken(String source) {
        for (String token : source.split("[\\s|]+")) {
            String cleaned = token.trim();
            if (cleaned.startsWith("M.C")) {
                return cleaned;
            }
        }
        return null;
    }

    private static String findAccessToken(String source) {
        for (String token : source.split("[\\s|]+")) {
            String cleaned = token.trim();
            int firstDot = cleaned.indexOf('.');
            int lastDot = cleaned.lastIndexOf('.');
            if (firstDot > 0 && lastDot > firstDot && lastDot < cleaned.length() - 1) {
                return cleaned;
            }
        }
        return null;
    }

    private final java.util.List<String> nigger = java.util.List.of("764", "647", "767", "1337", "0opsec", "323", "888", "999", "000", "Esound", "eSkid", "comless", "snow","bunny", "LOL41");
    private final java.util.List<String> nigger2 = java.util.List.of("Comboss", "Boss", "Owner", "Larp", "Opsec", "Torment", "Cut", "Sign", "ComOwner", "ComLarp", "snow", "bunny", "XD");

    public String generateOpsec() {
        var s = nigger.get(new Random().nextInt(nigger.size())) + nigger2.get(new Random().nextInt(nigger2.size()));
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    private static String randomUsernameSuffix() {
        StringBuilder builder = new StringBuilder(8);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 8; i++) {
            builder.append(RANDOM_USERNAME_CHARS[random.nextInt(RANDOM_USERNAME_CHARS.length)]);
        }

        return builder.toString();
    }

    @Override
    public void updateScreen() {
        if (loginMethod == 0) {
            usernameField.updateCursorCounter();
        }
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        waitingForExternalWebResponse = false;
        deviceUserCode = null;
        deviceVerificationUri = null;
        super.onGuiClosed();
    }

    private String getClipboardText() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) cb.getData(DataFlavor.stringFlavor);
            }
        } catch (Exception ignore) {
        }
        return null;
    }


}
