package team.gothaj.client.impl.gui.config;

import lombok.Getter;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.config.Config;
import team.gothaj.client.api.config.ConfigManager;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.*;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.music.input.MusicSearchField;
import team.gothaj.client.impl.module.visual.ClickGUI;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL11.*;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class ConfigMenu extends GuiScreen {

    private MusicSearchField textField;
    private final List<GothajButton> gothajButtons = new ArrayList<>(List.of(
            new GothajButton("Save", 1),
            new GothajButton("Load", 2),
            new GothajButton("Remove", 3),
            new GothajButton("Download", 4),
            new GothajButton("Upload", 5),
            new GothajButton("Cancel", 0)
    ));

    private static final float SCREEN_WIDTH_MULTIPLIER = 1.0f;
    private static final float SCROLL_SPEED = 0.18f;
    private static final long DOUBLE_CLICK_THRESHOLD = 350;

    private final ConfigManager.Type[] types = ConfigManager.Type.values();
    private final Map<ConfigManager.Type, List<Config>> configsByType = new EnumMap<>(ConfigManager.Type.class);
    private final List<List<Rectangle>> cardRectsByScreen = new ArrayList<>();
    private final List<Integer> btnWidthsCache = new ArrayList<>();

    private Framebuffer cardFrameBuffer;

    private int buttonsTotalWidth = 0;
    private int currentScreen = 0;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private int selectedConfigIndex = -1;

    @Getter
    public static Config selectedConfig = null;

    private long lastConfigClickTime = 0;
    private boolean dataDirty = true;
    private boolean layoutDirty = true;
    private int cachedWidth = -1, cachedHeight = -1;

    private int scissorX, scissorY, scissorW, scissorH;
    private float verticalScroll = 0f;
    private float verticalScrollTarget = 0f;
    public ConfigMenu() {}

    private void runAsync(Runnable ioAction, Runnable onUiDone) {
        CompletableFuture
                .runAsync(ioAction)
                .thenRun(() -> {
                    if (onUiDone != null) {
                        mc.addScheduledTask(onUiDone);
                    }
                });
    }

    @Override
    public void initGui() {
        textField = new MusicSearchField(1, Fonts.nunito20, (int) (width - 160), 10, 100, 30);
        textField.setPlaceholder("Config name...");
        cachedWidth = width;
        cachedHeight = height;

        refreshUI();

        recomputeButtons();

        super.initGui();
    }

    private void snapshotConfigs() {
        configsByType.clear();
        List<Config> allConfigs = new ArrayList<>(GothajClient.getInstance().getConfigManager().getItems());
        for (ConfigManager.Type type : types) {
            List<Config> configsOfType = new ArrayList<>();
            for (Config config : allConfigs) {
                if (config.getType() == type) configsOfType.add(config);
            }
            configsByType.put(type, configsOfType);
        }
        dataDirty = false;
    }

    private void rebuildLayout() {
        cardRectsByScreen.clear();
        for (ConfigManager.Type type : types) {
            List<Config> configs = configsByType.getOrDefault(type, Collections.emptyList());
            cardRectsByScreen.add(computeGridRects(configs.size()));
        }
        layoutDirty = false;
    }

    private List<Rectangle> computeGridRects(int count) {
        List<Rectangle> rects = new ArrayList<>(count);
        int cardWidth = 140, cardHeight = 40, margin = 15;
        int panelWidth = width - 120;
        int cardsPerRow = Math.max(1, (panelWidth + margin) / (cardWidth + margin));
        int startX = 60, startY = 92;
        for (int i = 0; i < count; i++) {
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = startX + col * (cardWidth + margin);
            int y = startY + row * (cardHeight + margin);
            rects.add(new Rectangle(x, y, cardWidth, cardHeight));
        }
        return rects;
    }

    private void recomputeButtons() {
        btnWidthsCache.clear();
        int spacing = 25;
        buttonsTotalWidth = -spacing;
        for (GothajButton button : gothajButtons) {
            int w = (int) Fonts.nunito14.getStringWidth(button.getDisplayText());
            btnWidthsCache.add(w);
            buttonsTotalWidth += w + spacing;
        }
    }

    private void refreshUI() {
        runAsync(() -> GothajClient.getInstance().getConfigManager().load(), () -> {
            dataDirty = true;
            layoutDirty = true;
        });
    }

    private void refreshLocalUI() {
        runAsync(() -> GothajClient.getInstance().getConfigManager().refreshLocal(), () -> {
            dataDirty = true;
            layoutDirty = true;
        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        float diff = targetScrollOffset - scrollOffset;
        if (Math.abs(diff) > 0.5f) scrollOffset += diff * SCROLL_SPEED;
        else scrollOffset = targetScrollOffset;

        float vDiff = verticalScrollTarget - verticalScroll;
        if (Math.abs(vDiff) > 0.5f) verticalScroll += vDiff * SCROLL_SPEED;
        else verticalScroll = verticalScrollTarget;

        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            layoutDirty = true;
            recomputeButtons();
        }
        if (dataDirty) snapshotConfigs();
        if (layoutDirty) rebuildLayout();

        Gui.drawRect(0, 0, width, height, new Color(15, 15, 15, 150).getRGB());

        ScaledResolution scaledResolution = new ScaledResolution(mc);

        float barHeight = 30;
        float barX = 40;
        float barY = 10f;
        float barTextY = barY + barHeight / 2;

        // panel blur + bloom — no scissor active, renders freely
        Blur.startBlur();
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barHeight, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barHeight + 10, width - 100, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(false);
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barHeight, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barHeight + 10, width - 100, height - (barY + barHeight) * 2, 15f, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(barX, barY, width - 100, barHeight, 15f, new Color(20, 20, 20, 100));
        RoundedUtil.drawRound(barX, barY + barHeight + 10, width - 100, height - (barY + barHeight) * 2, 15f, new Color(20, 20, 20, 100));
        Fonts.surf20.drawString("CONFIG MANAGER", barX + 15, barY + barHeight / 2f, Color.WHITE.getRGB());

        scissorX = (int) barX;
        scissorY = (int) (barY + barHeight + 10);
        scissorW = Math.max(0, (int) (width - 100));
        scissorH = Math.max(0, (int) (height - (barY + barHeight) * 2));

        if (scissorW > 0 && scissorH > 0) {
            // pass 1: batched blur for all card shapes — single blur pass instead of per-card
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.startScissorBox(scissorX, scissorY, scissorW, scissorH);
            Blur.startBlur();
            for (int i = 0; i < types.length; i++) {
                renderTypeScreenBloomShapes(types[i], i);
            }
            Blur.endBlur(10, 1);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            // pass 2: draw all card bloom shapes into cardFrameBuffer WITH scissor active,
            // then bloom OUTSIDE scissor — same pattern as SelectThemeMenu
            cardFrameBuffer = RenderUtil.createFrameBuffer(cardFrameBuffer, true);
            cardFrameBuffer.framebufferClear();
            cardFrameBuffer.bindFramebuffer(true);
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.startScissorBox(scissorX, scissorY, scissorW, scissorH);
            for (int i = 0; i < types.length; i++) {
                renderTypeScreenBloomShapes(types[i], i);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST); // disable BEFORE unbind so bloom composites freely
            GL11.glPopMatrix();
            cardFrameBuffer.unbindFramebuffer();
            Bloom.renderBloom(cardFrameBuffer.framebufferTexture, 4, Color.BLACK.getRGB());

            // pass 3: draw actual card content (backgrounds, text, avatars) with scissor
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtil.startScissorBox(scissorX, scissorY, scissorW, scissorH);
            for (int i = 0; i < types.length; i++) {
                renderTypeScreen(types[i], mouseX, mouseY, i);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glPopMatrix();
        } else {
            for (int i = 0; i < types.length; i++) {
                renderTypeScreen(types[i], mouseX, mouseY, i);
            }
        }

        float buttonsY = barTextY - Fonts.nunito14.getHeight() - 3;
        renderButtons(mouseX, mouseY, buttonsY);
        drawScrollIndicator(scaledResolution);
        textField.drawText();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // only draws the black shader rounds used as bloom source shapes
    private void renderTypeScreenBloomShapes(ConfigManager.Type type, int screenIndex) {
        float offsetX = (screenIndex * width * SCREEN_WIDTH_MULTIPLIER) - scrollOffset;
        List<Config> configs = configsByType.getOrDefault(type, Collections.emptyList());
        List<Rectangle> currentRects = (screenIndex < cardRectsByScreen.size()) ? cardRectsByScreen.get(screenIndex) : Collections.emptyList();

        int cardHeight = 40;

        for (int i = 0; i < configs.size() && i < currentRects.size(); i++) {
            Rectangle hitbox = currentRects.get(i);
            float finalX = hitbox.x + offsetX;
            float finalY = hitbox.y - verticalScroll;
            float cardWidth = hitbox.width;
            if (finalX + cardWidth < 0 || finalX > width) continue;
            RoundedUtil.drawShaderRound(finalX, finalY, cardWidth, cardHeight, 10f, Color.BLACK);
        }
    }

    private void renderTypeScreen(ConfigManager.Type type, int mouseX, int mouseY, int screenIndex) {
        float titleY = 70;
        float offsetX = (screenIndex * width * SCREEN_WIDTH_MULTIPLIER) - scrollOffset;

        String typeTitle = type.toString().toLowerCase();
        if (!typeTitle.isEmpty()) typeTitle = typeTitle.substring(0, 1).toUpperCase() + typeTitle.substring(1) + " Configs";
        else typeTitle = "Configs";
        Fonts.surf20.drawCenteredString(typeTitle, (width / 2f) + offsetX, titleY, Color.WHITE.getRGB());

        List<Config> configs = configsByType.getOrDefault(type, Collections.emptyList());
        List<Rectangle> currentRects = (screenIndex < cardRectsByScreen.size()) ? cardRectsByScreen.get(screenIndex) : Collections.emptyList();

        int cardHeight = 40;
        int iconSize = 24;
        int padding = 10;

        for (int i = 0; i < configs.size() && i < currentRects.size(); i++) {
            Rectangle hitbox = currentRects.get(i);

            float finalX = hitbox.x + offsetX;
            float finalY = hitbox.y - verticalScroll;
            float cardWidth = hitbox.width;

            if (finalX + cardWidth < 0 || finalX > width) continue;

            Config config = configs.get(i);

            Color pColor = GothajClient.getInstance().getThemeManager().getCurrentTheme().primary();
            Color sColor = GothajClient.getInstance().getThemeManager().getCurrentTheme().secondary();

            if (currentScreen == type.ordinal() && i == selectedConfigIndex) {
                RoundedUtil.drawGradientHorizontal(finalX - 1, finalY - 1, cardWidth + 2, cardHeight + 2, 12f,
                        new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 120),
                        new Color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), 120));
            }

            boolean isHovered = mouseX >= finalX && mouseX <= finalX + cardWidth && mouseY >= finalY && mouseY <= finalY + cardHeight;
            RoundedUtil.drawRound(finalX, finalY, cardWidth, cardHeight, 10f, isHovered ? new Color(25, 25, 25, 100) : new Color(25, 25, 25, 40));

            AvatarUtil.drawForumAvatar(config.getUid(), (int) (finalX + padding), (int) (finalY + (cardHeight - iconSize) / 2f), iconSize, iconSize, 8f, 1);

            String displayName = config.getProfile();
            float maxTextWidth = cardWidth - iconSize - 25;
            if (Fonts.nunito18.getStringWidth(displayName) > maxTextWidth)
                displayName = Fonts.nunito18.trimStringToWidth(displayName, (int) maxTextWidth - 10) + "...";

            Fonts.nunito18.drawString(displayName,
                    finalX + padding + iconSize + 10,
                    finalY + cardHeight / 2f - Fonts.nunito18.getHeight() / 2f - 6f,
                    Color.WHITE.getRGB());

            Fonts.nunito14.drawString(config.getAuthor(),
                    finalX + padding + iconSize + 10,
                    finalY + cardHeight / 2f - 0.5f,
                    new Color(150, 150, 150).getRGB());

            String typeLabel = config.getSaveDate();
            if (typeLabel != null && !typeLabel.isEmpty()) typeLabel = typeLabel.substring(0, 1).toUpperCase() + typeLabel.substring(1);
            else typeLabel = "N/A";
            Fonts.nunito14.drawString(typeLabel,
                    finalX + padding + iconSize + 10,
                    finalY + cardHeight / 2f + 8,
                    new Color(150, 150, 150).getRGB());
        }
    }

    private void renderButtons(int mouseX, int mouseY, float textY) {
        int btnHeight = 20;
        int spacing = 25;
        if (btnWidthsCache.isEmpty() || btnWidthsCache.size() != gothajButtons.size()) recomputeButtons();
        int currentX = (width - buttonsTotalWidth) / 2;
        for (int i = 0; i < gothajButtons.size(); i++) {
            GothajButton button = gothajButtons.get(i);
            int w = btnWidthsCache.get(i);
            button.setXPos(currentX);
            button.setYPos(textY);
            button.setWidth(w);
            button.setHeight(btnHeight);
            currentX += w + spacing;
            if (button.getOnClick() == null)
                button.setOnClick(() -> handleButtonAction(button.getId()));
            button.drawScreen(mouseX, mouseY);
        }
    }

    private void drawScrollIndicator(ScaledResolution scaledResolution) {
        float indicatorWidth = 100;
        float indicatorHeight = 4;
        float indicatorX = (float) scaledResolution.getScaledWidth() / 2 - indicatorWidth / 2;
        float indicatorY = scaledResolution.getScaledHeight() - 20;

        RoundedUtil.drawRound(indicatorX, indicatorY, indicatorWidth, indicatorHeight, 2f, new Color(255, 255, 255, 30));
        float progress = (scrollOffset / (width * SCREEN_WIDTH_MULTIPLIER)) / (types.length - 1);
        float progressWidth = indicatorWidth * Math.max(0, Math.min(1, progress));
        RoundedUtil.drawRound(indicatorX, indicatorY, progressWidth, indicatorHeight, 2f, new Color(255, 255, 255, 80));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        textField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) {
            GothajClient.getInstance().getModuleManager().find(ClickGUI.class).toggle();
        }
        if (keyCode == 203 && currentScreen == 0) {
            GothajClient.getInstance().getModuleManager().find(ClickGUI.class).toggle();
        }
        if ((keyCode == 203) && currentScreen > 0) {
            currentScreen--;
            targetScrollOffset = currentScreen * width * SCREEN_WIDTH_MULTIPLIER;
            selectedConfigIndex = -1;
            selectedConfig = null;
            verticalScrollTarget = 0;
            verticalScroll = 0;
        } else if ((keyCode == 205) && currentScreen < types.length - 1) {
            currentScreen++;
            targetScrollOffset = currentScreen * width * SCREEN_WIDTH_MULTIPLIER;
            selectedConfigIndex = -1;
            selectedConfig = null;
            verticalScrollTarget = 0;
            verticalScroll = 0;
        } else if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && selectedConfig != null) {
            loadSelectedConfigAsync();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        textField.mouseClicked(mouseX, mouseY, mouseButton);
        gothajButtons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        if (mouseButton == 0) {
            List<Config> configs = configsByType.getOrDefault(types[currentScreen], Collections.emptyList());
            List<Rectangle> currentHitboxes = (currentScreen < cardRectsByScreen.size()) ? cardRectsByScreen.get(currentScreen) : null;
            if (currentHitboxes != null) {
                int clickedIndex = -1;
                int adjustedMouseY = (int) (mouseY + verticalScroll);
                for (int i = 0; i < Math.min(currentHitboxes.size(), configs.size()); i++) {
                    if (currentHitboxes.get(i).contains(mouseX, adjustedMouseY)) {
                        clickedIndex = i;
                        break;
                    }
                }
                if (clickedIndex != -1) {
                    Config clickedConfig = configs.get(clickedIndex);
                    long now = System.currentTimeMillis();
                    if (clickedIndex == selectedConfigIndex && clickedConfig.equals(selectedConfig)
                            && now - lastConfigClickTime < DOUBLE_CLICK_THRESHOLD) {
                        loadSelectedConfigAsync();
                    } else {
                        selectedConfigIndex = clickedIndex;
                        selectedConfig = clickedConfig;
                        lastConfigClickTime = now;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            verticalScrollTarget -= dWheel * 2.5f;
            verticalScrollTarget = Math.clamp(verticalScrollTarget, 0, getMaxVerticalScroll());
        }
    }

    private float getMaxVerticalScroll() {
        List<Config> configs = configsByType.getOrDefault(types[currentScreen], Collections.emptyList());
        int cardHeight = 40, margin = 15;
        int panelWidth = width - 120;
        int cardsPerRow = Math.max(1, (panelWidth + margin) / (140 + margin));
        int rows = (int) Math.ceil(configs.size() / (float) cardsPerRow);
        float totalHeight = rows * (cardHeight + margin);
        float panelHeight = scissorH;
        return Math.max(0, totalHeight - panelHeight + 60);
    }

    private void handleButtonAction(int id) {
        switch (id) {
            case 0 -> mc.displayGuiScreen(null);
            case 1 -> {
                String configName = textField.getText().trim();
                if (configName.isEmpty() && selectedConfig == null) {
                    GothajClient.getInstance().getNotificationManager().add("Config", "Config input cannot be empty", Notification.Type.ERROR);
                    return;
                }
                if (!configName.isEmpty()) {
                    Config config = new Config(configName);
                    runAsync(() -> GothajClient.getInstance().getConfigManager().save(config), () -> {
                        GothajClient.getInstance().getNotificationManager().add("Config", config.getProfile() + " has been saved", Notification.Type.SUCCESS);
                        textField.setText("");
                        refreshLocalUI();
                    });
                } else {
                    runAsync(() -> GothajClient.getInstance().getConfigManager().save(selectedConfig), () -> {
                        GothajClient.getInstance().getNotificationManager().add("Config", selectedConfig.getProfile() + " has been saved", Notification.Type.SUCCESS);
                        refreshLocalUI();
                    });
                }
            }
            case 2 -> { if (selectedConfig != null) loadSelectedConfigAsync(); }
            case 3 -> {
                if (selectedConfig != null) {
                    if (selectedConfig.isOnline()) {
                        GothajClient.getInstance().getNotificationManager().add("Config", "Cannot remove online config!", Notification.Type.ERROR);
                        return;
                    }
                    runAsync(() -> GothajClient.getInstance().getConfigManager().remove(selectedConfig), () -> {
                        GothajClient.getInstance().getNotificationManager().add("Config", selectedConfig.getProfile() + " has been removed", Notification.Type.SUCCESS);
                        selectedConfig = null;
                        selectedConfigIndex = -1;
                        refreshLocalUI();
                    });
                }
            }
            case 4 -> {
                if (selectedConfig == null) {
                    GothajClient.getInstance().getNotificationManager().add("Config", "Select an online config to download", Notification.Type.ERROR);
                    return;
                }
                if (!selectedConfig.isOnline()) {
                    GothajClient.getInstance().getNotificationManager().add("Config", "Only online configs can be downloaded", Notification.Type.ERROR);
                    return;
                }
                final String profileName = selectedConfig.getProfile();
                runAsync(() -> {
                    try {
                        GothajClient.getInstance().getConfigManager().download(selectedConfig);
                    } catch (IOException | InterruptedException e) {
                        GothajClient.getInstance().getNotificationManager().add("Config", "Download failed: " + e.getMessage(), Notification.Type.ERROR);
                    }
                }, () -> {
                    Config localConfig = new Config(profileName);
                    runAsync(() -> GothajClient.getInstance().getConfigManager().load(localConfig, true), () -> {
                        GothajClient.getInstance().getNotificationManager().add("Config", profileName + " has been loaded", Notification.Type.SUCCESS);
                        refreshUI();
                    });
                });
            }
            case 5 -> {
                if (selectedConfig == null) {
                    GothajClient.getInstance().getNotificationManager().add("Config", "Select a local config to upload", Notification.Type.ERROR);
                    return;
                }
                if (!selectedConfig.isLocal()) {
                    GothajClient.getInstance().getNotificationManager().add("Config", "Only local configs can be uploaded", Notification.Type.ERROR);
                    return;
                }
                runAsync(() -> GothajClient.getInstance().getConfigManager().upload(selectedConfig), () ->
                        runAsync(() -> GothajClient.getInstance().getConfigManager().fetchOnlineConfigs(), this::refreshUI));
            }
        }
    }

    private void loadSelectedConfigAsync() {
        if (selectedConfig == null) return;
        if (selectedConfig.isLocal()) {
            runAsync(() -> GothajClient.getInstance().getConfigManager().load(selectedConfig, true), () ->
                    GothajClient.getInstance().getNotificationManager().add("Config", selectedConfig.getProfile() + " has been loaded", Notification.Type.SUCCESS));
        } else {
            final String profileName = selectedConfig.getProfile();
            runAsync(() -> {
                try {
                    GothajClient.getInstance().getConfigManager().download(selectedConfig);
                } catch (IOException | InterruptedException e) {
                    GothajClient.getInstance().getNotificationManager().add("Config", "Download failed: " + e.getMessage(), Notification.Type.ERROR);
                }
            }, () -> {
                Config localConfig = new Config(profileName);
                runAsync(() -> GothajClient.getInstance().getConfigManager().load(localConfig, true), () -> {
                    GothajClient.getInstance().getNotificationManager().add("Config", profileName + " has been loaded", Notification.Type.SUCCESS);
                    refreshUI();
                });
            });
        }
    }
}

