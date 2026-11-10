package team.gothaj.client.impl.gui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class GothajWhiteMenu extends GuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawWhiteBackground();


        ScaledResolution sc = new ScaledResolution(mc);


        Blur.startBlur();
        RoundedUtil.drawShaderRound(15, sc.getScaledHeight() / 2f - 150, 35, 300, 17, Color.BLACK);
        Blur.endBlur(10, 1);

        guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
        guiFrameBuffer.framebufferClear();
        guiFrameBuffer.bindFramebuffer(true);
        RoundedUtil.drawShaderRound(15, sc.getScaledHeight() / 2f - 150, 35, 300, 17, Color.BLACK);
        guiFrameBuffer.unbindFramebuffer();
        Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 100, new Color(180, 180, 180).getRGB());

        RoundedUtil.drawRound(15, sc.getScaledHeight() / 2f - 150, 35, 300, 17, new Color(240, 240, 240, 100));


        RenderUtil.drawRect(18, sc.getScaledHeight() / 2f - 110, 29, 1,
                new Color(100, 100, 100, 50).getRGB());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }


    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void drawWhiteBackground() {

    }
}
