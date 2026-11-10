package team.gothaj.client.impl.gui.menu;

import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.AvatarUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.account.AccountMenu;
import team.gothaj.client.impl.gui.proxy.ProxyMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static team.gothaj.client.api.util.Utils.sr;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class GothajTestMenu extends GuiScreen {

    private static final float SCROLL_SPEED = 0.06f;
    private static final float SCREEN_WIDTH_MULTIPLIER = 1.0f;
    private static final long SCROLL_COOLDOWN = 500;
    private static final long LOADING_DELAY = 1000;
    private static final float LOADING_FADE_SPEED = 0.05f;
    private static final long LOADING_DURATION = 600;
    private final List<GothajButton> buttons = List.of(
            new GothajButton("Home", 0),
            new GothajButton("Singleplayer", 1),
            new GothajButton("Multiplayer", 2),
            new GothajButton("Account Manager", 3),
            new GothajButton("Proxy Manager", 4),
            new GothajButton("Options", 5),
            new GothajButton("Exit", 6)
    );
    private int currentScreen = 0;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private long lastScrollTime = 0;
    private long screenChangeTime = 0;
    private float loadingAlpha = 0f;
    private boolean shouldLoad = false;
    private boolean isLoading = false;
    private int targetScreen = -1;
    private long loadingStartTime = 0;

    @Override
    public void initGui() {
        super.initGui();
        currentScreen = 0;
        scrollOffset = 0f;
        targetScrollOffset = 0f;
        resetLoading();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        handleScroll();
        updateScrollAnimation(partialTicks);
        updateLoading();

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        float scaledWidth = (float) scaledResolution.getScaledWidth();
        float scaledHeight = (float) scaledResolution.getScaledHeight();

        drawBackground();

        glPushMatrix();
        glTranslatef(-scrollOffset, 0, 0);

        for (int i = 0; i < buttons.size(); i++) {
            float screenX = i * scaledWidth * SCREEN_WIDTH_MULTIPLIER;
            drawScreenContent(i, mouseX + (int) scrollOffset - (int) screenX, mouseY, screenX, scaledWidth, scaledHeight);
        }

        glPopMatrix();

        if ((shouldLoad || isLoading) && loadingAlpha > 0f) {
            drawLoadingOverlay();
        }

        drawUI(mouseX, mouseY, scaledResolution);
        drawScrollIndicator(scaledResolution);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawLoadingOverlay() {

        Gui.drawRect(0, 0, width, height, new Color(0, 0, 0, 230).getRGB());

        if (loadingAlpha > 0.3f) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            if (loadingAlpha > 0.7f) {
                RenderUtil.drawLoadingCircle(width / 2f, height / 2f);
            }

            String loadingText = getLoadingText(targetScreen);
            if (loadingText != null && loadingAlpha > 0.5f) {
                int textAlpha = (int) ((loadingAlpha - 0.5f) * 2 * 255);
                Fonts.surf20.drawCenteredStringWithShadow(loadingText,
                        (float) width / 2, (float) height / 2 + 40,
                        new Color(255, 255, 255, textAlpha).getRGB());
            }
        }
    }

    private String getLoadingText(int screen) {
        if (screen == -1) return null;

        return switch (screen) {
            case 1 -> "LAUNCHING SINGLEPLAYER...";
            case 2 -> "CONNECTING TO MULTIPLAYER...";
            case 3 -> "OPENING ACCOUNT MANAGER...";
            case 4 -> "OPENING PROXY MANAGER...";
            case 5 -> "LOADING OPTIONS...";
            case 6 -> "GOODBYE...";
            default -> null;
        };
    }

    private void drawScreenContent(int screenIndex, int mouseX, int mouseY, float screenX, float scaledWidth, float scaledHeight) {
        GothajButton currentButton = buttons.get(screenIndex);
        String title = currentButton.getDisplayText().toUpperCase();

        float textX = screenX + scaledWidth / 2;
        float textY = scaledHeight / 2;

        boolean titleHovered = mouseX >= -100 && mouseX <= 100 &&
                mouseY >= textY - 20 && mouseY <= textY + 20;

        int titleColor = titleHovered ? new Color(255, 255, 255, 255).getRGB() :
                new Color(255, 255, 255, 180).getRGB();

        if (screenIndex == 0) {
            Fonts.surf105.drawCenteredStringWithShadow("GOTHAJ", textX, textY, titleColor);
        } else {
            Fonts.surf105.drawCenteredStringWithShadow(title, textX, textY, titleColor);
        }

        drawScreenSpecificContent(screenIndex, mouseX, mouseY, screenX, titleHovered, scaledWidth, scaledHeight);
    }

    private void drawScreenSpecificContent(int screenIndex, int mouseX, int mouseY, float screenX, boolean titleHovered, float scaledWidth, float scaledHeight) {
        float contentY = scaledHeight / 2 + 40;
        float screenCenterX = screenX + scaledWidth / 2;

        switch (screenIndex) {
            case 0:
                Fonts.nunito18.drawCenteredString("Enhanced gameplay - Advanced features - Premium experience",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;

            case 1:
                Fonts.nunito18.drawCenteredString("Create a new world or continue your adventure",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;

            case 2:
                Fonts.nunito18.drawCenteredString("Connect to servers and play with friends",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;

            case 3:
                Fonts.nunito18.drawCenteredString("Manage your alternative accounts",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;

            case 4:
                Fonts.nunito18.drawCenteredString("Manage your proxies",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;

            case 5:
                Fonts.nunito18.drawCenteredString("Configure game settings and preferences",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;

            case 6:
                Fonts.nunito18.drawCenteredString("Close the game",
                        screenCenterX, contentY, new Color(200, 200, 200, 150).getRGB());
                break;
        }

        Fonts.nunito18.drawCenteredString("ALPHA RELEASE", screenCenterX, scaledHeight - Fonts.nunito18.getHeight() - 5,  new Color(200, 200, 200, 150).getRGB());

    }

    private void drawUI(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        float scaledWidth = (float) scaledResolution.getScaledWidth();
        float barWidth = scaledWidth * 0.57f;
        float barHeight = 30;
        float barX = scaledWidth / 2 - barWidth / 2;
        float barY = 10f;
        float barTextY = barY + barHeight / 2;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(barX, barY, barWidth, barHeight, 15f, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(barX, barY, barWidth, barHeight, 15f, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 4, Color.BLACK.getRGB());

        RoundedUtil.drawRound(barX, barY, barWidth, barHeight, 15f,  new Color(20, 20, 20, 100));

        Fonts.surf20.drawString("GOTHAJ", barX + 10, barTextY, -1);

        renderButton(mouseX, mouseY, barX + 90, barTextY - Fonts.nunito14.getHeight() - 3, barWidth - 90 - 40);

        AvatarUtil.drawForumAvatar(barX + barWidth - 28, barY + 5.2f, 20, 20, 10f, 1);
        RoundedUtil.drawRound(barX + barWidth - 28 - 0.5F, barY + 5.2f - 0.5F, 20 + 1, 20 + 1, 10f, new Color(0, 0, 0, 100));

        String shortName = String.valueOf(GothajClient.getInstance().getUser().getUsername().charAt(0)) + GothajClient.getInstance().getUser().getUsername().charAt(2);

        Fonts.rubik14Bold.drawCenteredString(shortName.toUpperCase(), barX + barWidth - 18, barY + 14f, Color.white);
    }

    private void drawScrollIndicator(ScaledResolution scaledResolution) {
        float indicatorWidth = 100;
        float indicatorHeight = 4;
        float indicatorX = (float) scaledResolution.getScaledWidth() / 2 - indicatorWidth / 2;
        float indicatorY = scaledResolution.getScaledHeight() - 30;

        RoundedUtil.drawRound(indicatorX, indicatorY, indicatorWidth, indicatorHeight, 2f,
                new Color(255, 255, 255, 30));

        float progress = (scrollOffset / (width * SCREEN_WIDTH_MULTIPLIER)) / (buttons.size() - 1);
        float progressWidth = indicatorWidth * Math.max(0, Math.min(1, progress));
        RoundedUtil.drawRound(indicatorX, indicatorY, progressWidth, indicatorHeight, 2.f,
                new Color(255, 255, 255, 80));
    }

    private void handleScroll() {
        int scroll = Mouse.getDWheel();
        long currentTime = System.currentTimeMillis();

        if (scroll != 0 && currentTime - lastScrollTime > SCROLL_COOLDOWN) {
            int oldScreen = currentScreen;

            if (scroll > 0 && currentScreen > 0) {
                currentScreen--;
                lastScrollTime = currentTime;
            } else if (scroll < 0 && currentScreen < buttons.size() - 1) {
                currentScreen++;
                lastScrollTime = currentTime;
            }

            if (oldScreen != currentScreen) {
                resetLoading();
            }

            ScaledResolution sr = new ScaledResolution(mc);
            float scaledWidth = (float) sr.getScaledWidth();
            targetScrollOffset = currentScreen * scaledWidth * SCREEN_WIDTH_MULTIPLIER;
        }
    }

    private void resetLoading() {
        screenChangeTime = System.currentTimeMillis();
        loadingAlpha = 0f;
        shouldLoad = false;
        isLoading = false;
        targetScreen = -1;
        loadingStartTime = 0;
    }

    private void updateLoading() {
        long currentTime = System.currentTimeMillis();

        if (!shouldLoad && !isLoading && currentTime - screenChangeTime >= LOADING_DELAY) {
            int target = getLoadingTarget(currentScreen);
            if (target != -1) {
                shouldLoad = true;
                targetScreen = target;
                loadingStartTime = currentTime;
            }
        }

        if (shouldLoad || isLoading) {
            if (loadingAlpha < 1f) {
                loadingAlpha = Math.min(1f, loadingAlpha + LOADING_FADE_SPEED);
            } else if (shouldLoad && loadingAlpha >= 1f) {
                shouldLoad = false;
                isLoading = true;
                loadingStartTime = currentTime;
            } else if (isLoading && currentTime - loadingStartTime >= LOADING_DURATION) {
                executeButtonAction(targetScreen);
            }
        }
    }

    private int getLoadingTarget(int screen) {
        return switch (screen) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 5;
            case 6 -> 6;
            default -> -1;
        };
    }

    private void updateScrollAnimation(float partialTicks) {
        float diff = targetScrollOffset - scrollOffset;
        if (Math.abs(diff) > 0.5f) {
            scrollOffset += diff * SCROLL_SPEED;
        } else {
            scrollOffset = targetScrollOffset;
        }
    }

    private void renderButton(int mouseX, int mouseY, float textX, float textY, float availableWidth) {
        ScaledResolution sr = new ScaledResolution(mc);
        float scaledWidth = (float) sr.getScaledWidth();
        float currentScreenFloat = scrollOffset / (scaledWidth * SCREEN_WIDTH_MULTIPLIER);
        int focusedScreen = Math.round(currentScreenFloat);

        float totalButtonWidth = 0;
        for (GothajButton button : buttons) {
            totalButtonWidth += Fonts.nunito14.getStringWidth(button.getDisplayText());
        }

        float gap = (availableWidth - totalButtonWidth) / buttons.size();
        float currentX = textX + gap / 2f;

        for (int i = 0; i < buttons.size(); i++) {
            GothajButton button = buttons.get(i);

            float buttonWidth = Fonts.nunito14.getStringWidth(button.getDisplayText());
            float buttonHeight = 20;

            button.setXPos(currentX);
            button.setYPos(textY);
            button.setWidth(buttonWidth);
            button.setHeight(buttonHeight);

            if (button.getOnClick() == null) {
                final int screenIndex = i;
                button.setOnClick(() -> {
                    currentScreen = screenIndex;
                    ScaledResolution srClick = new ScaledResolution(mc);
                    targetScrollOffset = currentScreen * (float) srClick.getScaledWidth() * SCREEN_WIDTH_MULTIPLIER;
                    if (screenIndex > 0) {
                        executeButtonAction(screenIndex);
                    }
                });
            }

            boolean isCurrentScreen = (i == focusedScreen);

            if (isCurrentScreen) {
                float bgPad = 6;
                float roundWidth = buttonWidth + bgPad * 2;
                float roundHeight = buttonHeight - 2;

                float textCenterX = button.getXPos() + buttonWidth / 2f;
                float roundX = textCenterX - roundWidth / 2f - 0.5f;
                float roundY = button.getYPos();

                float distance = Math.abs(i - currentScreenFloat);
                float alpha = 1.0f - Math.min(1.0f, distance * 1.4f);
                int alphaInt = (int) (alpha * 40);

                RoundedUtil.drawRound(
                        roundX,
                        roundY,
                        roundWidth,
                        roundHeight,
                        6f,
                        new Color(255, 255, 255, alphaInt)
                );
            }

            button.drawScreen(mouseX, mouseY);
            currentX += buttonWidth + gap;
        }
    }



    private void executeButtonAction(int buttonId) {
        switch (buttonId) {
            case 0 -> {
            }
            case 1 -> {
                GuiSelectWorld selectWorldGui = new GuiSelectWorld(this);
                mc.displayGuiScreen(selectWorldGui);
            }
            case 2 -> {
                GuiMultiplayer multiplayerGui = new GuiMultiplayer(this);
                mc.displayGuiScreen(multiplayerGui);
            }
            case 3 -> {
                AccountMenu accountMenu = new AccountMenu();
                mc.displayGuiScreen(accountMenu);
            }
            case 4 -> {
                ProxyMenu proxyMenu = new ProxyMenu();
                mc.displayGuiScreen(proxyMenu);
            }
            case 5 -> {
                GuiOptions optionsGui = new GuiOptions(this, mc.gameSettings);
                mc.displayGuiScreen(optionsGui);
            }
            case 6 -> {
                mc.shutdown();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            return;
        }

        resetLoading();

        if (keyCode == Keyboard.KEY_RETURN) {
            if (currentScreen == 0) {
                currentScreen = 1;
                ScaledResolution sr = new ScaledResolution(mc);
                targetScrollOffset = currentScreen * (float) sr.getScaledWidth() * SCREEN_WIDTH_MULTIPLIER;
            } else {
                executeButtonAction(currentScreen);
            }
            return;
        }

        if (keyCode == 203 && currentScreen > 0) {
            currentScreen--;
            ScaledResolution sr = new ScaledResolution(mc);
            targetScrollOffset = currentScreen * (float) sr.getScaledWidth() * SCREEN_WIDTH_MULTIPLIER;
        } else if (keyCode == 205 && currentScreen < buttons.size() - 1) {
            currentScreen++;
            ScaledResolution sr = new ScaledResolution(mc);
            targetScrollOffset = currentScreen * (float) sr.getScaledWidth() * SCREEN_WIDTH_MULTIPLIER;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));

        for (int i = 0; i < buttons.size(); i++) {
            float screenX = i * width * SCREEN_WIDTH_MULTIPLIER;
            if (mouseX + scrollOffset >= screenX && mouseX + scrollOffset < screenX + width) {
                handleScreenClick(i, (int) (mouseX + scrollOffset - screenX), mouseY, mouseButton);
                break;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleScreenClick(int screenIndex, int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            float scaledWidth = (float) sr.getScaledWidth();
            float scaledHeight = (float) sr.getScaledHeight();
            float textY = scaledHeight / 2;
            float textX = scaledWidth / 2;

            if (mouseX >= textX - 100 && mouseX <= textX + 100 &&
                    mouseY >= textY - 20 && mouseY <= textY + 20) {

                resetLoading();

                if (screenIndex == 0) {
                    currentScreen = 1;
                    targetScrollOffset = currentScreen * scaledWidth * SCREEN_WIDTH_MULTIPLIER;
                } else {
                    executeButtonAction(screenIndex);
                }
                return;
            }

            if (screenIndex == 0) {
                float buttonWidth = 180;
                float buttonHeight = 35;
                float buttonX = scaledWidth / 2 - buttonWidth / 2;
                float buttonY = scaledHeight / 2 + 90;

                if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                        mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {

                    resetLoading();
                    currentScreen = 1;
                    targetScrollOffset = currentScreen * scaledWidth * SCREEN_WIDTH_MULTIPLIER;
                }
            }
        }
    }
}
