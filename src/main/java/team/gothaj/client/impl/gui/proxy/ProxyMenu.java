package team.gothaj.client.impl.gui.proxy;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.proxy.Proxy;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.DomainUtil;
import team.gothaj.client.api.util.other.StreamTextureUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.proxy.impl.AddProxyMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class ProxyMenu extends GuiScreen {

    private final List<GothajButton> gothajButtons = new ArrayList<>(List.of(
            new GothajButton("Add", 0),
            new GothajButton("Disconnect", 1),
            new GothajButton("Back", 2)
    ));

    private final List<Rectangle> cardRects = new ArrayList<>();
    private int cachedWidth = -1, cachedHeight = -1;

    @Override
    public void initGui() {
        super.initGui();
        recomputeButtons();
        rebuildLayout();
    }

    private void rebuildLayout() {
        cardRects.clear();
        List<Proxy> proxies = GothajClient.getInstance().getProxyRepository().getItems();

        int cardWidth = 175, cardHeight = 40, margin = 12;
        int panelWidth = width - 120;
        int cardsPerRow = Math.max(1, (panelWidth + margin) / (cardWidth + margin));
        int startX = 60, startY = 65;

        for (int i = 0; i < proxies.size(); i++) {
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            cardRects.add(new Rectangle(startX + col * (cardWidth + margin), startY + row * (cardHeight + margin), cardWidth, cardHeight));
        }
    }

    /*

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
     */

    private void recomputeButtons() {
        float barY = 10f, barHeight = 30f;
        float buttonsY = (barY + barHeight / 2) - 10;
        int spacing = 15;

        float totalButtonsWidth = 0;
        for (GothajButton btn : gothajButtons) {
            float w = Fonts.nunito14.getStringWidth(btn.getDisplayText()) + 10;
            totalButtonsWidth += w;
        }
        totalButtonsWidth += spacing * (gothajButtons.size() - 1);

        float currentX = (width - totalButtonsWidth) / 2f;

        for (GothajButton btn : gothajButtons) {
            float w = Fonts.nunito14.getStringWidth(btn.getDisplayText()) + 10;
            btn.setXPos(currentX);
            btn.setYPos(buttonsY);
            btn.setWidth(w);
            btn.setHeight(20);
            btn.setOnClick(() -> handleAction(btn.getId()));

            currentX += w + spacing;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedWidth = width;
            cachedHeight = height;
            rebuildLayout();
            recomputeButtons();
        }

        drawDefaultBackground();
        float barX = 50, barY = 10, barH = 30;

        Blur.startBlur();
        RoundedUtil.drawShaderRound(barX, barY, width - 100, barH, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barH + 10, width - 100, height - (barY + barH) * 2, 15f, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(false);

        RoundedUtil.drawShaderRound(barX, barY, width - 100, barH, 15f, Color.BLACK);
        RoundedUtil.drawShaderRound(barX, barY + barH + 10, width - 100, height - (barY + barH) * 2, 15f, Color.BLACK);

        List<Proxy> proxies = GothajClient.getInstance().getProxyRepository().getItems();
        for (int i = 0; i < proxies.size() && i < cardRects.size(); i++) {
            Rectangle rect = cardRects.get(i);
            RoundedUtil.drawShaderRound(rect.x, rect.y, rect.width, rect.height, 10f, Color.BLACK);
        }

        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());

        RoundedUtil.drawRound(barX, barY, width - 100, barH, 15f, new Color(20, 20, 20, 100));
        RoundedUtil.drawRound(barX, barY + barH + 10, width - 100, height - (barY + barH) * 2, 15f, new Color(20, 20, 20, 100));

        Fonts.surf20.drawString("PROXY MANAGER", barX + 10, barY + barH / 2f, Color.WHITE.getRGB());
        gothajButtons.forEach(b -> b.drawScreen(mouseX, mouseY));

        Proxy currentProxy = GothajClient.getInstance().getProxyRepository().getCurrentProxy();

        for (int i = 0; i < proxies.size() && i < cardRects.size(); i++) {
            Proxy p = proxies.get(i);
            Rectangle rect = cardRects.get(i);
            boolean hovered = rect.contains(mouseX, mouseY);
            boolean active = p.equals(currentProxy);

            if (active) {
                Color pColor = GothajClient.getInstance().getThemeManager().getCurrentTheme().primary();
                Color sColor = GothajClient.getInstance().getThemeManager().getCurrentTheme().secondary();
                RoundedUtil.drawGradientHorizontal(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2, 12f,
                        new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 120),
                        new Color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), 120));
            }

            Blur.startBlur();
            RoundedUtil.drawShaderRound(rect.x, rect.y, rect.width, rect.height, 10f, Color.BLACK);
            Blur.endBlur(10, 1);
            RoundedUtil.drawRound(rect.x, rect.y, rect.width, rect.height, 10f, hovered ? new Color(25, 25, 25, 100) : new Color(25, 25, 25, 60));

            String resolved = DomainUtil.resolve(p.domain());
            float flagSize = 20;
            float flagPadding = 6;

            if (resolved != null) {
                String flagUrl = "http://api.ip2flag.com/" + resolved + "/128";
                int textureId = StreamTextureUtil.get(flagUrl);

                if (textureId != -1) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
                    RenderUtil.resetColor();
                    RoundedUtil.drawRoundTextured(rect.x + flagPadding, rect.y + (rect.height - flagSize) / 2f, flagSize, flagSize, 10f, 1f);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                } else {
                    RoundedUtil.drawRound(rect.x + flagPadding, rect.y + (rect.height - flagSize) / 2f, flagSize, flagSize, 6f, new Color(50, 50, 50, 160));
                }
            } else {
                RoundedUtil.drawRound(rect.x + flagPadding, rect.y + (rect.height - flagSize) / 2f, flagSize, flagSize, 6f, new Color(40, 25, 25, 160));
            }

            float textX = rect.x + flagPadding + flagSize + 8;
            Fonts.nunito16.drawString(p.domain(), textX, rect.y + (rect.height / 2f) - 8, Color.WHITE.getRGB());
            Fonts.nunito12.drawString(p.port(), textX, rect.y + (rect.height / 2f) + 4, new Color(160, 160, 160).getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void handleAction(int id) {
        switch (id) {
            case 0 -> mc.displayGuiScreen(new AddProxyMenu(this));
            case 1 -> {
                GothajClient.getInstance().getProxyRepository().setCurrentProxy(null);
                GothajClient.getInstance().getNotificationManager().add("Proxy", "Disconnected", Notification.Type.SUCCESS);
            }
            case 2 -> mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        gothajButtons.forEach(b -> b.mouseClicked(mouseX, mouseY, mouseButton));

        List<Proxy> proxies = GothajClient.getInstance().getProxyRepository().getItems();
        for (int i = 0; i < cardRects.size(); i++) {
            if (cardRects.get(i).contains(mouseX, mouseY)) {
                Proxy p = proxies.get(i);
                if (mouseButton == 0) {
                    GothajClient.getInstance().getProxyRepository().setCurrentProxy(p);
                    GothajClient.getInstance().getNotificationManager().add("Proxy", "Connected to " + p.domain(), Notification.Type.SUCCESS);
                } else if (mouseButton == 1) {
                    GothajClient.getInstance().getProxyRepository().remove(p);
                    rebuildLayout();
                    GothajClient.getInstance().getNotificationManager().add("Proxy", "Removed " + p.domain(), Notification.Type.INFO);
                }
                break;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }
}