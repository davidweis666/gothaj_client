package team.gothaj.client.impl.gui.theme;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.theme.CustomTheme;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.music.input.MusicSearchField;
import team.gothaj.client.impl.module.visual.ClickGUI;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

@SuppressWarnings("all")
public class SelectThemeMenu extends GuiScreen {

    private static final int PICKER_SIZE = 80;
    private static final int HUE_BAR_WIDTH = 8;
    private static final int HUE_BAR_PAD = 18;
    private static final int SV_HUE_PAD = 8;
    private static final int PICKER_BG_WIDTH = 280;
    private static final int PICKER_BG_HEIGHT = 175;
    private static final int PICKER_BG_RADIUS = 16;
    private static final int PANEL_WIDTH = 540;
    private static final int PANEL_HEIGHT = 400;
    private static final int MASK_WIDTH = 524;
    private static final int MASK_HEIGHT = 350;
    private static final int CARD_WIDTH = 140;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 12;
    private static final float BASE_WIDTH = 854f;
    private static final float BASE_HEIGHT = 480f;

    private float scrollOffset = 0f;
    private int maxScroll;
    private int selected = -1;
    private float targetScroll = 0f;

    private Framebuffer cardFrameBuffer;
    private Framebuffer svFrameBufferPrimary;
    private Framebuffer svFrameBufferSecondary;

    private boolean addingTheme = false;
    private float primaryHue = 0f, primarySat = 1f, primaryBright = 1f;
    private boolean draggingPrimarySV = false, draggingPrimaryHue = false;
    private float secondaryHue = 0.66f, secondarySat = 1f, secondaryBright = 1f;
    private boolean draggingSecondarySV = false, draggingSecondaryHue = false;

    private int[] svCachePrimary = null;
    private int[] svCacheSecondary = null;
    private float lastPrimaryHue = -1f;
    private float lastSecondaryHue = -1f;
    private int lastPickerSize = -1;

    private MusicSearchField themeNameField;

    private float getMenuScale() {
        return Math.min(this.width / BASE_WIDTH, this.height / BASE_HEIGHT);
    }

    private void applyScaledScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, w * f, h * f);
    }

    private static int scaled(int value, float scale) {
        return Math.max(1, Math.round(value * scale));
    }

    @Override
    public void initGui() {
        float uiScale = getMenuScale();
        int rowHeight = scaled(CARD_HEIGHT + CARD_GAP, uiScale);
        int visibleH = scaled(MASK_HEIGHT, uiScale);
        int rows = (int) Math.ceil((double) (GothajClient.getInstance().getThemeManager().getItems().size() + 1) / 3);
        maxScroll = Math.max(0, rows * rowHeight - visibleH);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        targetScroll = scrollOffset;

        ScaledResolution sr = new ScaledResolution(mc);
        int fieldW = scaled(PICKER_BG_WIDTH - 40, uiScale);
        int fieldH = 20;
        int fieldX = sr.getScaledWidth() / 2 - fieldW / 2;
        int fieldY = sr.getScaledHeight() / 2 + scaled(PICKER_BG_HEIGHT / 2 - 28, uiScale);
        themeNameField = new MusicSearchField(1, Fonts.nunito16, fieldX, fieldY, fieldW, fieldH);
        themeNameField.setPlaceholder("Theme name...");
        themeNameField.setMaxStringLength(32);

        GothajClient.getInstance().getThemeManager().load();

        super.initGui();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !addingTheme) {
            int step = scaled(40, getMenuScale());
            targetScroll -= wheel > 0 ? step : -step;
            targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float uiScale = getMenuScale();
        if (mouseButton != 0) return;

        if (addingTheme) {
            themeNameField.mouseClicked(mouseX, mouseY, mouseButton);

            ScaledResolution sr = new ScaledResolution(mc);
            int centerX = sr.getScaledWidth() / 2;
            int centerY = sr.getScaledHeight() / 2;

            int pickerSize = scaled(PICKER_SIZE, uiScale);
            int hueBarWidth = scaled(HUE_BAR_WIDTH, uiScale);
            int hueBarPad = scaled(HUE_BAR_PAD, uiScale);
            int svHuePad = scaled(SV_HUE_PAD, uiScale);
            int pickerBgHeight = scaled(PICKER_BG_HEIGHT, uiScale);

            int totalWidth = hueBarWidth + svHuePad + pickerSize + hueBarPad + hueBarWidth + svHuePad + pickerSize;
            int startX = centerX - totalWidth / 2;
            int pickerBgY = centerY - pickerBgHeight / 2;
            int pickerY = pickerBgY + scaled(40, uiScale);

            int primaryHueX = startX;
            int primarySVX = primaryHueX + hueBarWidth + svHuePad;
            int secondaryHueX = primarySVX + pickerSize + hueBarPad;
            int secondarySVX = secondaryHueX + hueBarWidth + svHuePad;

            int confW = scaled(100, uiScale), confH = scaled(18, uiScale);
            int confX = centerX - confW / 2;
            int confY = pickerBgY + pickerBgHeight - scaled(28, uiScale);

            if (mouseX >= primaryHueX && mouseX < primaryHueX + hueBarWidth && mouseY >= pickerY && mouseY < pickerY + pickerSize) {
                draggingPrimaryHue = true;
                primaryHue = MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
            }
            if (mouseX >= primarySVX && mouseX < primarySVX + pickerSize && mouseY >= pickerY && mouseY < pickerY + pickerSize) {
                draggingPrimarySV = true;
                primarySat = MathHelper.clamp(mouseX - primarySVX, 0, pickerSize - 1) / (float) (pickerSize - 1);
                primaryBright = 1f - MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
            }
            if (mouseX >= secondarySVX && mouseX < secondarySVX + pickerSize && mouseY >= pickerY && mouseY < pickerY + pickerSize) {
                draggingSecondarySV = true;
                secondarySat = MathHelper.clamp(mouseX - secondarySVX, 0, pickerSize - 1) / (float) (pickerSize - 1);
                secondaryBright = 1f - MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
            }
            if (mouseX >= secondaryHueX && mouseX < secondaryHueX + hueBarWidth && mouseY >= pickerY && mouseY < pickerY + pickerSize) {
                draggingSecondaryHue = true;
                secondaryHue = MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
            }
            if (mouseX >= confX && mouseX < confX + confW && mouseY >= confY && mouseY < confY + confH) {
                confirmTheme();
            }

            boolean inPrimaryHue = mouseX >= primaryHueX && mouseX < primaryHueX + hueBarWidth && mouseY >= pickerY && mouseY < pickerY + pickerSize;
            boolean inPrimarySV = mouseX >= primarySVX && mouseX < primarySVX + pickerSize && mouseY >= pickerY && mouseY < pickerY + pickerSize;
            boolean inSecondaryHue = mouseX >= secondaryHueX && mouseX < secondaryHueX + hueBarWidth && mouseY >= pickerY && mouseY < pickerY + pickerSize;
            boolean inSecondarySV = mouseX >= secondarySVX && mouseX < secondarySVX + pickerSize && mouseY >= pickerY && mouseY < pickerY + pickerSize;
            boolean inConfirm = mouseX >= confX && mouseX < confX + confW && mouseY >= confY && mouseY < confY + confH;
            boolean inNameField = mouseX >= themeNameField.xPosition && mouseX < themeNameField.xPosition + themeNameField.getWidth()
                    && mouseY >= themeNameField.yPosition && mouseY < themeNameField.yPosition + 20;
            if (!inPrimaryHue && !inPrimarySV && !inSecondaryHue && !inSecondarySV && !inConfirm && !inNameField)
                addingTheme = false;
            return;
        }

        int cardW = scaled(CARD_WIDTH, uiScale);
        int cardH = scaled(CARD_HEIGHT, uiScale);
        int cardGap = scaled(CARD_GAP, uiScale);
        int maskY = height / 2 - scaled(170, uiScale);
        int maskH = scaled(MASK_HEIGHT, uiScale);
        int panelX = width / 2 - (cardW * 3 + cardGap * 2) / 2;
        int panelY = maskY - (int) scrollOffset;

        int themeCount = GothajClient.getInstance().getThemeManager().getItems().size();
        for (int i = 0; i < themeCount; i++) {
            int col = i % 3, row = i / 3;
            int x = panelX + col * (cardW + cardGap);
            int y = panelY + row * (cardH + cardGap);
            CustomTheme theme = GothajClient.getInstance().getThemeManager().getItems().get(i);
            if (mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH
                    && y + cardH >= maskY && y <= maskY + maskH - cardH) {
                selected = i;
                GothajClient.getInstance().getThemeManager().setCurrentTheme(theme);
                GothajClient.getInstance().getThemeManager().save();
                Hud.primaryColor.setColor(theme.primary());
                Hud.secondaryColor.setColor(theme.secondary());
                break;
            }
        }

        int addCol = themeCount % 3, addRow = themeCount / 3;
        int addX = panelX + addCol * (cardW + cardGap);
        int addY = panelY + addRow * (cardH + cardGap);
        if (addY + cardH >= maskY && addY <= maskY + maskH - cardH
                && mouseX >= addX && mouseX <= addX + cardW
                && mouseY >= addY && mouseY <= addY + cardH) {
            addingTheme = true;
            themeNameField.setText("");
            themeNameField.setFocused(true);
        }
    }

    private void confirmTheme() {
        String name = themeNameField.getText().trim();
        if (name.isEmpty()) name = "Custom";
        Color primary = new Color(Color.HSBtoRGB(primaryHue, primarySat, primaryBright));
        Color secondary = new Color(Color.HSBtoRGB(secondaryHue, secondarySat, secondaryBright));
        GothajClient.getInstance().getThemeManager().addCustomTheme(name, primary, secondary);
        addingTheme = false;
        initGui();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        draggingPrimarySV = draggingPrimaryHue = draggingSecondarySV = draggingSecondaryHue = false;
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!addingTheme) return;
        float uiScale = getMenuScale();
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        int pickerSize = scaled(PICKER_SIZE, uiScale);
        int hueBarWidth = scaled(HUE_BAR_WIDTH, uiScale);
        int hueBarPad = scaled(HUE_BAR_PAD, uiScale);
        int svHuePad = scaled(SV_HUE_PAD, uiScale);
        int pickerBgHeight = scaled(PICKER_BG_HEIGHT, uiScale);

        int totalWidth = hueBarWidth + svHuePad + pickerSize + hueBarPad + hueBarWidth + svHuePad + pickerSize;
        int startX = centerX - totalWidth / 2;
        int pickerBgY = centerY - pickerBgHeight / 2;
        int pickerY = pickerBgY + scaled(40, uiScale);

        int primaryHueX = startX;
        int primarySVX = primaryHueX + hueBarWidth + svHuePad;
        int secondaryHueX = primarySVX + pickerSize + hueBarPad;
        int secondarySVX = secondaryHueX + hueBarWidth + svHuePad;

        if (draggingPrimaryHue) {
            primaryHue = MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
            svCachePrimary = null;
        }
        if (draggingPrimarySV) {
            primarySat = MathHelper.clamp(mouseX - primarySVX, 0, pickerSize - 1) / (float) (pickerSize - 1);
            primaryBright = 1f - MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
        }
        if (draggingSecondarySV) {
            secondarySat = MathHelper.clamp(mouseX - secondarySVX, 0, pickerSize - 1) / (float) (pickerSize - 1);
            secondaryBright = 1f - MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
        }
        if (draggingSecondaryHue) {
            secondaryHue = MathHelper.clamp(mouseY - pickerY, 0, pickerSize - 1) / (float) (pickerSize - 1);
            svCacheSecondary = null;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float uiScale = getMenuScale();
        ScaledResolution sr = new ScaledResolution(mc);
        int panelW = scaled(PANEL_WIDTH, uiScale);
        int panelH = scaled(PANEL_HEIGHT, uiScale);
        int panelXC = sr.getScaledWidth() / 2 - panelW / 2;
        int panelYC = sr.getScaledHeight() / 2 - panelH / 2;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(panelXC, panelYC, panelW, panelH, scaled(16, uiScale), Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(panelXC, panelYC, panelW, panelH, scaled(16, uiScale), Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(panelXC, panelYC, panelW, panelH, scaled(16, uiScale), new Color(30, 30, 30, 150));
        Fonts.surf24.drawCenteredStringWithShadow("Themes", sr.getScaledWidth() / 2, sr.getScaledHeight() / 2 - scaled(187, uiScale), -1);

        int cardW = scaled(CARD_WIDTH, uiScale);
        int cardH = scaled(CARD_HEIGHT, uiScale);
        int cardGap = scaled(CARD_GAP, uiScale);
        int maskX = sr.getScaledWidth() / 2 - scaled(262, uiScale);
        int maskY = sr.getScaledHeight() / 2 - scaled(170, uiScale);
        int maskW = scaled(MASK_WIDTH, uiScale);
        int maskH = scaled(MASK_HEIGHT, uiScale);
        int panelX = sr.getScaledWidth() / 2 - (cardW * 3 + cardGap * 2) / 2;
        int panelY = maskY - (int) scrollOffset;

        scrollOffset += (targetScroll - scrollOffset) * 0.28f;
        if (Math.abs(targetScroll - scrollOffset) < 0.5f) scrollOffset = targetScroll;

        cardFrameBuffer = RenderUtil.createFrameBuffer(cardFrameBuffer, true);
        cardFrameBuffer.framebufferClear();
        cardFrameBuffer.bindFramebuffer(true);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        applyScaledScissor(maskX, maskY, maskW, maskH);
        for (int i = 0; i < GothajClient.getInstance().getThemeManager().getItems().size(); i++) {
            int col = i % 3, row = i / 3;
            int x = panelX + col * (cardW + cardGap);
            int y = panelY + row * (cardH + cardGap);
            RoundedUtil.drawGradientHorizontal(x + scaled(10, uiScale), y + scaled(8, uiScale), cardW - scaled(20, uiScale), cardH - scaled(10, uiScale), scaled(6, uiScale), Color.BLACK, Color.BLACK);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
        cardFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(cardFrameBuffer.framebufferTexture, 4, Color.BLACK.getRGB());

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        applyScaledScissor(maskX, maskY, maskW, maskH);
        int themeCount = GothajClient.getInstance().getThemeManager().getItems().size();
        for (int i = 0; i < themeCount; i++) {
            int col = i % 3, row = i / 3;
            int x = panelX + col * (cardW + cardGap);
            int y = panelY + row * (cardH + cardGap);
            CustomTheme theme = GothajClient.getInstance().getThemeManager().getItems().get(i);
            RoundedUtil.drawGradientHorizontal(x + scaled(10, uiScale), y + scaled(8, uiScale), cardW - scaled(20, uiScale), cardH - scaled(10, uiScale), scaled(6, uiScale), theme.primary(), theme.secondary());
            String src = theme.name().trim();
            String themeName = src.replaceAll("(?<=[\\p{Ll}\\p{Nd}])(?=\\p{Lu})|(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})", " ");
            if (selected == i)
                Fonts.notifs28.drawString("s", x + cardW / 2f - scaled(5, uiScale), y + cardH / 2f - scaled(1, uiScale), -1);
            Fonts.nunito18.drawCenteredString(themeName, x + cardW / 2f, y + cardH + scaled(2, uiScale), selected == i ? Color.WHITE.getRGB() : Color.GRAY.getRGB());
        }

        int addCol = themeCount % 3, addRow = themeCount / 3;
        int addX = panelX + addCol * (cardW + cardGap);
        int addY = panelY + addRow * (cardH + cardGap);
        RoundedUtil.drawRound(addX + scaled(10, uiScale), addY + scaled(8, uiScale), cardW - scaled(20, uiScale), cardH - scaled(10, uiScale), scaled(6, uiScale), new Color(60, 60, 60, 200));
        Fonts.theme26.drawCenteredString("a", addX + cardW / 2f, addY + cardH / 2f - scaled(1, uiScale), new Color(150, 150, 150, 200).getRGB());
        Fonts.nunito18.drawCenteredString("Add Theme", addX + cardW / 2f, addY + cardH + scaled(2, uiScale), Color.GRAY.getRGB());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        int themeCardsH = ((int) Math.ceil((double) (themeCount + 1) / 3)) * (cardH + cardGap);
        if (themeCardsH > maskH) {
            float scrollRatio = (float) scrollOffset / Math.max(1, maxScroll);
            int scrollbarH = Math.max(40, (int) ((float) maskH * maskH / themeCardsH));
            int scrollbarY = maskY + (int) ((maskH - scrollbarH) * scrollRatio);
            int scrollbarX = sr.getScaledWidth() / 2 + scaled(262, uiScale);
            RoundedUtil.drawRound(scrollbarX, scrollbarY, scaled(4, uiScale), scrollbarH, scaled(2, uiScale), new Color(210, 210, 210, 130));
        }

        if (addingTheme) {
            Blur.startBlur();
            Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(30, 30, 30, 80).getRGB());
            Blur.endBlur(10, 1);

            int centerX = sr.getScaledWidth() / 2;
            int centerY = sr.getScaledHeight() / 2;
            int pickerBgW = scaled(PICKER_BG_WIDTH, uiScale);
            int pickerBgH = scaled(PICKER_BG_HEIGHT, uiScale);
            int pickerRadius = scaled(PICKER_BG_RADIUS, uiScale);
            int pickerSize = scaled(PICKER_SIZE, uiScale);
            int hueBarWidth = scaled(HUE_BAR_WIDTH, uiScale);
            int hueBarPad = scaled(HUE_BAR_PAD, uiScale);
            int svHuePad = scaled(SV_HUE_PAD, uiScale);
            int pickerBgX = centerX - pickerBgW / 2;
            int pickerBgY = centerY - pickerBgH / 2;

            Blur.startBlur();
            RoundedUtil.drawShaderRound(pickerBgX, pickerBgY, pickerBgW, pickerBgH, pickerRadius, Color.BLACK);
            Blur.endBlur(10, 1);

            guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
            guiFrameBuffer.framebufferClear();
            guiFrameBuffer.bindFramebuffer(true);
            RoundedUtil.drawShaderRound(pickerBgX, pickerBgY, pickerBgW, pickerBgH, pickerRadius, Color.BLACK);
            guiFrameBuffer.unbindFramebuffer();
            Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());
            RoundedUtil.drawRound(pickerBgX, pickerBgY, pickerBgW, pickerBgH, pickerRadius, new Color(30, 30, 30, 150));
            Fonts.surf24.drawCenteredStringWithShadow("Theme Picker", centerX, pickerBgY + scaled(12, uiScale), Color.WHITE.getRGB());

            int totalWidth = hueBarWidth + svHuePad + pickerSize + hueBarPad + hueBarWidth + svHuePad + pickerSize;
            int startX = centerX - totalWidth / 2;
            int pickerY = pickerBgY + scaled(40, uiScale);
            int labelY = pickerY - scaled(10, uiScale);
            int primaryHueX = startX;
            int primarySVX = primaryHueX + hueBarWidth + svHuePad;
            int secondaryHueX = primarySVX + pickerSize + hueBarPad;
            int secondarySVX = secondaryHueX + hueBarWidth + svHuePad;

            if (svCachePrimary == null || lastPrimaryHue != primaryHue || lastPickerSize != pickerSize) {
                svCachePrimary = buildSVCache(pickerSize, primaryHue);
                lastPrimaryHue = primaryHue;
                lastPickerSize = pickerSize;
            }
            if (svCacheSecondary == null || lastSecondaryHue != secondaryHue) {
                svCacheSecondary = buildSVCache(pickerSize, secondaryHue);
                lastSecondaryHue = secondaryHue;
            }

            svFrameBufferPrimary = RenderUtil.createFrameBuffer(svFrameBufferPrimary, true);
            svFrameBufferPrimary.framebufferClear();
            svFrameBufferPrimary.bindFramebuffer(true);
            drawSVFromCache(primarySVX, pickerY, pickerSize, svCachePrimary, true);
            svFrameBufferPrimary.unbindFramebuffer();
            Bloom.renderBloom(svFrameBufferPrimary.framebufferTexture, 6, Color.BLACK.getRGB());
            drawSVFromCache(primarySVX, pickerY, pickerSize, svCachePrimary, false);

            Fonts.nunito16.drawCenteredString("Primary", primarySVX + pickerSize / 2, labelY, Color.WHITE.getRGB());
            int curX = primarySVX + (int) (primarySat * (pickerSize - 1));
            int curY = pickerY + (int) ((1f - primaryBright) * (pickerSize - 1));
            Gui.drawRect(curX - scaled(2, uiScale), curY - scaled(2, uiScale), curX + scaled(3, uiScale), curY + scaled(3, uiScale), Color.WHITE.getRGB());

            renderHueBar(primaryHueX, pickerY, hueBarWidth, pickerSize);
            Gui.drawRect(primaryHueX - scaled(2, uiScale), pickerY + (int) (primaryHue * (pickerSize - 1)) - scaled(2, uiScale),
                    primaryHueX + hueBarWidth + scaled(2, uiScale), pickerY + (int) (primaryHue * (pickerSize - 1)) + 1, Color.WHITE.getRGB());

            svFrameBufferSecondary = RenderUtil.createFrameBuffer(svFrameBufferSecondary, true);
            svFrameBufferSecondary.framebufferClear();
            svFrameBufferSecondary.bindFramebuffer(true);
            drawSVFromCache(secondarySVX, pickerY, pickerSize, svCacheSecondary, true);
            svFrameBufferSecondary.unbindFramebuffer();
            Bloom.renderBloom(svFrameBufferSecondary.framebufferTexture, 6, Color.BLACK.getRGB());
            drawSVFromCache(secondarySVX, pickerY, pickerSize, svCacheSecondary, false);

            Fonts.nunito16.drawCenteredString("Secondary", secondarySVX + pickerSize / 2, labelY, Color.WHITE.getRGB());
            int cur2X = secondarySVX + (int) (secondarySat * (pickerSize - 1));
            int cur2Y = pickerY + (int) ((1f - secondaryBright) * (pickerSize - 1));
            Gui.drawRect(cur2X - scaled(2, uiScale), cur2Y - scaled(2, uiScale), cur2X + scaled(3, uiScale), cur2Y + scaled(3, uiScale), Color.WHITE.getRGB());

            renderHueBar(secondaryHueX, pickerY, hueBarWidth, pickerSize);
            Gui.drawRect(secondaryHueX - scaled(2, uiScale), pickerY + (int) (secondaryHue * (pickerSize - 1)) - scaled(2, uiScale),
                    secondaryHueX + hueBarWidth + scaled(2, uiScale), pickerY + (int) (secondaryHue * (pickerSize - 1)) + 1, Color.WHITE.getRGB());

            themeNameField.xPosition = centerX - themeNameField.getWidth() / 2;
            themeNameField.yPosition = pickerBgY + pickerBgH - scaled(38, uiScale);
            themeNameField.drawText();

            int confW = scaled(100, uiScale), confH = scaled(18, uiScale);
            int confX = centerX - confW / 2;
            int confY = pickerBgY + pickerBgH - scaled(18, uiScale);
            boolean hoveringConfirm = mouseX >= confX && mouseX < confX + confW && mouseY >= confY && mouseY < confY + confH;
            RoundedUtil.drawRound(confX, confY, confW, confH, scaled(6, uiScale),
                    hoveringConfirm ? new Color(80, 80, 80, 200) : new Color(60, 60, 60, 180));
            Fonts.nunito16.drawCenteredString("Confirm", centerX, confY + confH / 2f - Fonts.nunito16.getHeight() / 2f, Color.WHITE.getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int[] buildSVCache(int pickerSize, float hue) {
        int[] cache = new int[pickerSize * pickerSize];
        for (int sy = 0; sy < pickerSize; sy++) {
            float v = 1f - sy / (float) (pickerSize - 1);
            for (int sx = 0; sx < pickerSize; sx++) {
                float s = sx / (float) (pickerSize - 1);
                cache[sy * pickerSize + sx] = Color.HSBtoRGB(hue, s, v);
            }
        }
        return cache;
    }

    private void drawSVFromCache(int svX, int pickerY, int pickerSize, int[] cache, boolean blackOverride) {
        for (int sy = 0; sy < pickerSize; sy++) {
            int y = pickerY + sy;
            int segStart = svX;
            int prevColor = blackOverride ? Color.BLACK.getRGB() : cache[sy * pickerSize];
            for (int sx = 1; sx < pickerSize; sx++) {
                int c = blackOverride ? Color.BLACK.getRGB() : cache[sy * pickerSize + sx];
                if (c != prevColor) {
                    Gui.drawRect(segStart, y, svX + sx, y + 1, prevColor);
                    segStart = svX + sx;
                    prevColor = c;
                }
            }
            Gui.drawRect(segStart, y, svX + pickerSize, y + 1, prevColor);
        }
    }

    private void renderHueBar(int hueX, int pickerY, int hueBarWidth, int pickerSize) {
        for (int sy = 0; sy < pickerSize; sy++) {
            int rgb = Color.HSBtoRGB(sy / (float) (pickerSize - 1), 1f, 1f);
            Gui.drawRect(hueX, pickerY + sy, hueX + hueBarWidth, pickerY + sy + 1, rgb);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (addingTheme) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                addingTheme = false;
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                confirmTheme();
                return;
            }
            themeNameField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (keyCode == 1 || keyCode == Keyboard.KEY_RIGHT)
            GothajClient.getInstance().getModuleManager().find(ClickGUI.class).toggle();
    }
}