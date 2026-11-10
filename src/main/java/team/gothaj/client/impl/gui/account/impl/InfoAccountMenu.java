package team.gothaj.client.impl.gui.account.impl;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import team.gothaj.client.api.account.Account;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.SkinUtil;
import team.gothaj.client.api.util.render.AvatarUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.account.AccountMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class InfoAccountMenu extends GuiScreen {

    private final java.util.List<GothajButton> gothajButtons = new ArrayList<>(List.of(
            new GothajButton("Mark As Favorite", 0),
            new GothajButton("Refresh Token", 1),
            new GothajButton("Cancel", 2)
    ));

    private final Account selectedAccount;

    private ServerList saved;

    private final Map<String, Integer> avatarTextureCache = new HashMap<>();


    public InfoAccountMenu(Account selectedAccount) {
        this.selectedAccount = selectedAccount;


    }

    @Override
    public void initGui() {

        saved = new ServerList(mc);
        saved.loadServerList();

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        drawBackground();


        float barHeight = 30;
        float barX = 50;
        float barY = 10f;
        float barTextY = barY + barHeight / 2;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barHeight, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barHeight + 10, width / 4f - 20, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX + width / 4f - 10, barY + barHeight + 10, width / 2f + 150,
                height - (barY + barHeight) * 2, 15f, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barHeight, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barHeight + 10, width / 4f - 20, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX + width / 4f - 10, barY + barHeight + 10, width / 2f + 150, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(barX, barY, width - 100, barHeight, 15f, new Color(20, 20, 20, 100));

        Fonts.surf20.drawString("ACCOUNT INFO", barX + 10, barY + barHeight / 2f, Color.WHITE.getRGB());


        RoundedUtil.drawRound(barX, barY + barHeight + 10, width / 4f - 20,
                height - (barY + barHeight) * 2, 15f, new Color(20, 20, 20, 100));

        RoundedUtil.drawRound(barX + width / 4f - 10, barY + barHeight + 10, width / 2f + 150,
                height - (barY + barHeight) * 2, 15f, new Color(20, 20, 20, 100));


        float buttonsY = barTextY - Fonts.nunito14.getHeight() - 3;

        renderButtons(mouseX, mouseY, buttonsY);

        renderInfo(barX + 62, barY + barHeight + 50);


        for (int i = 0, y = 0; i < saved.countServers(); i++) {
            ServerData sd = saved.getServerData(i);
            String ip = sd.serverIP == null ? "" : sd.serverIP;
            String ipLC = ip.toLowerCase(Locale.ROOT);
            if (ipLC.contains("test") || ipLC.contains("loyisa")) continue;


            ResourceLocation rs = AvatarUtil.prepareServerIcon(sd);
            RenderUtil.drawImage(rs, barX + width / 4f + 11, barY + barHeight + 30 + y , 48, 48, false);

            y += 76;

        }

        super.drawScreen(mouseX, mouseY, partialTicks);

    }


    private void renderInfo(float x, float y) {

        int spacing = Fonts.rubik23.getHeight() + 10;

        int avatarSize = 96;

        Account acc = selectedAccount;
        String avatarKey = acc.isCracked() ? "cracked" : acc.getUsername();
        int textureId;

        if (avatarTextureCache.containsKey(avatarKey)) {
            textureId = avatarTextureCache.get(avatarKey);
        } else {
            String url = acc.isCracked()
                    ? "https://minotar.net/helm/gothajgayshit/96"
                    : "https://minotar.net/helm/" + acc.getUsername() + "/96";
            ByteBuffer buffer = SkinUtil.bufferedImageToByteBuffer(url);
            textureId = SkinUtil.uploadHeadTexture(buffer, avatarSize, avatarSize);
            avatarTextureCache.put(avatarKey, textureId);
        }

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(x, y, avatarSize, avatarSize, 24f, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        RenderUtil.resetColor();
        RoundedUtil.drawRoundTextured(x, y, avatarSize, avatarSize, 24f, 255f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);


        long timeAdded = acc.getTimeAdded();
        long playTime = acc.getPlayTime();

        long totalSeconds = playTime / 20;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        Date dateAdded = new Date(timeAdded);

        SimpleDateFormat sdfTime = new SimpleDateFormat("dd.MM.yy HH:mm");


        Fonts.rubik23.drawCenteredString(EnumChatFormatting.DARK_GRAY + "Username: " + EnumChatFormatting.WHITE + selectedAccount.getUsername(), x + (width / 4f - 20) / 2f - 62, y + avatarSize  + spacing, -1);
        Fonts.rubik23.drawCenteredString(EnumChatFormatting.DARK_GRAY + "Status: " +  (acc.isCracked() ? "§cCracked" : acc.isExpired() ? "§cExpired" : "§aPremium"), x + (width / 4f - 20) / 2f - 62, y + avatarSize + spacing * 2, -1);
        Fonts.rubik23.drawCenteredString(EnumChatFormatting.DARK_GRAY + "Time Added: " + EnumChatFormatting.WHITE + sdfTime.format(dateAdded), x + (width / 4f - 20) / 2f - 62, y + avatarSize + spacing * 3, -1);
        Fonts.rubik23.drawCenteredString(EnumChatFormatting.DARK_GRAY + "Playtime: " + EnumChatFormatting.WHITE + String.format("%02d:%02d:%02d", hours, minutes, seconds), x + (width / 4f - 20) / 2f - 62, y + avatarSize + spacing * 4, -1);

        long expiresAt = acc.getExpiresAt();
        long now = System.currentTimeMillis();
        long timeLeft = expiresAt - now;

        long secondsE = timeLeft / 1000;
        long hoursE = secondsE / 3600;
        long minutesE = (secondsE % 3600) / 60;
        long secs = secondsE % 60;

        if (!acc.isCracked() && !acc.isExpired())
            Fonts.rubik23.drawCenteredString(EnumChatFormatting.DARK_GRAY + "Expires in: " + EnumChatFormatting.WHITE + String.format("%02d:%02d:%02d", hoursE, minutesE, secs), x + (width / 4f - 20) / 2f - 62, y + avatarSize + spacing * 5, -1);

    }


    private void renderButtons(int mouseX, int mouseY, float textY) {
        int btnHeight = 20;
        int spacing = 25;

        List<Integer> btnWidths = new ArrayList<>();
        int totalWidth = -spacing;
        for (GothajButton button : gothajButtons) {
            int w = (int) Fonts.nunito14.getStringWidth(button.getDisplayText());
            btnWidths.add(w);
            totalWidth += w + spacing;
        }

        int currentX = (width - totalWidth) / 2;
        for (int i = 0; i < gothajButtons.size(); i++) {
            GothajButton button = gothajButtons.get(i);
            int w = btnWidths.get(i);
            button.setXPos(currentX);
            button.setYPos(textY);
            button.setWidth(w);
            button.setHeight(btnHeight);
            currentX += w + spacing;

            if (button.getOnClick() == null) {
                button.setOnClick(() -> handleButtonAction(button.getId()));
            }
            button.drawScreen(mouseX, mouseY);
        }
    }

    private void handleButtonAction(int id) {
        switch (id) {
            case 0 -> {

            }
            case 1 -> {
                if (!selectedAccount.isCracked()) {
                    new Thread(() -> {
                        GothajClient.getInstance().getNotificationManager().add("Account", "Refreshing...", Notification.Type.INFO);
                        try {
                            if (selectedAccount.getRefreshToken() == null || selectedAccount.getRefreshToken().isEmpty()) {
                                GothajClient.getInstance().getNotificationManager().add("Account", "No refresh token available", Notification.Type.ERROR);
                                return;
                            }

                            JavaAuthManager authManager = JavaAuthManager
                                    .create(MinecraftAuth.createHttpClient("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"))
                                    .login(selectedAccount.getRefreshToken());

                            MinecraftToken minecraftToken = authManager.getMinecraftToken().getUpToDate();
                            MinecraftProfile minecraftProfile = authManager.getMinecraftProfile().getUpToDate();

                            long expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L;
                            selectedAccount.setInvalidCredentials(false);
                            selectedAccount.setExpiresAt(expiresAt);
                            selectedAccount.setAccessToken(minecraftToken.getToken());
                            selectedAccount.setUsername(minecraftProfile.getName());
                            selectedAccount.setUuid(minecraftProfile.getId());

                            if (authManager.getMsaToken().getCached() != null && authManager.getMsaToken().getCached().getRefreshToken() != null) {
                                selectedAccount.setRefreshToken(authManager.getMsaToken().getCached().getRefreshToken());
                            }

                            GothajClient.getInstance().getNotificationManager().add("Account", "Token refresh successful", Notification.Type.SUCCESS);
                        } catch (Exception e) {
                            GothajClient.getInstance().getNotificationManager().add("Account", "Error occurred", Notification.Type.ERROR);
                        }
                    }).start();
                } else {
                    GothajClient.getInstance().getNotificationManager().add("Account", "Unable to refresh offline account", Notification.Type.ERROR);
                }
            }
            case 2 -> mc.displayGuiScreen(new AccountMenu());
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        for (int textureId : avatarTextureCache.values()) {
            GL11.glDeleteTextures(textureId);
        }
        avatarTextureCache.clear();
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        gothajButtons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
