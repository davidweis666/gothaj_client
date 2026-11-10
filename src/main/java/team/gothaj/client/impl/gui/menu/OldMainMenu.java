package team.gothaj.client.impl.gui.menu;

import net.minecraft.client.gui.*;
import team.gothaj.client.api.ui.button.GothajButton;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.other.FileUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.account.AccountMenu;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class OldMainMenu extends GuiScreen {

    private final List<GothajButton> buttons = List.of(
            new GothajButton("Singleplayer", 1),
            new GothajButton("Multiplayer", 2),
            new GothajButton("Alt Manager", 3),
            new GothajButton("Options", 4),
            new GothajButton("Exit", 5)
    );

    private int animationTicks = 0;
    private static final int SLIDE_ANIMATION_TICKS = 30;
    private static final int SLIDE_OFFSET = 500;

    @Override
    public void initGui() {
        GothajClient.getInstance().getIrcManager().getClient().setMcServerIp("");

        animationTicks = 0;
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        if (animationTicks < SLIDE_ANIMATION_TICKS) {
            animationTicks++;
        }

        float animationProgress = Math.min(animationTicks / (float) SLIDE_ANIMATION_TICKS, 1.0f);
        float easeInOut = (float) (1 - Math.cos(animationProgress * Math.PI)) / 2;

        /*
         * Animated text
         */
        float textFinalY = height / 2f - 105;
        float textStartY = height + 40;
        float animatedTextY = textStartY * (1 - easeInOut) + textFinalY * easeInOut;

        GothajButton.setUseShader(true);
        Blur.startBlur();
        renderButton(mouseX, mouseY, easeInOut);
        Fonts.titleVK.drawCenteredStringWithShadow("G", width / 2f - 1, animatedTextY, Color.BLACK.getRGB());
        Blur.endBlur(30, 1);

        GothajButton.setUseShader(true);
        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        Fonts.titleVK.drawCenteredStringWithShadow("G", width / 2f - 1, animatedTextY, Color.BLACK.getRGB());
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 11, Color.BLACK.getRGB());

        GothajButton.setUseShader(false);
        renderButton(mouseX, mouseY, easeInOut);
        Fonts.rubik18.drawStringWithShadow(FileUtil.hashBuildDate(GothajClient.getInstance().getVersion()), 0, 2, new Color(210, 210, 210, 10).getRGB());
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderButton(int mouseX, int mouseY, float easeInOut) {
        int spacing = 10;

        for (GothajButton button : buttons) {
            float finalY = ((height / 2f) + spacing - (buttons.size() * 22) / 2f);
            float animatedY = (finalY + SLIDE_OFFSET) * (1 - easeInOut) + finalY * easeInOut;

            button.setXPos(width / 2f - 160 / 2f);
            button.setYPos(animatedY);
            button.setWidth(160);
            button.setHeight(22);

            if (button.getOnClick() == null) {
                button.setOnClick(() -> {
                    switch (button.getId()) {
                        case 1 -> mc.displayGuiScreen(new GuiSelectWorld(this));
                        case 2 -> mc.displayGuiScreen(new GuiMultiplayer(this));
                        case 3 -> mc.displayGuiScreen(new AccountMenu());
                        case 4 -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                        case 5 -> mc.shutdown();
                    }
                });
            }

            button.drawScreen(mouseX, mouseY);
            spacing += (22 + 6);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
