package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.shader.impl.Bloom;
import team.gothaj.client.api.util.render.shader.impl.Blur;
import team.gothaj.client.impl.gui.theme.SelectThemeMenu;


import java.awt.*;

import static team.gothaj.client.impl.module.client.ShaderProcessing.guiFrameBuffer;

public class GuiButton extends Gui
{
    protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");
    protected int width;
    protected int height;
    public int xPosition;
    public int yPosition;
    public String displayString;
    public int id;
    public boolean enabled;
    public boolean visible;
    protected boolean hovered;

    private float scale = 1.0f;
    private float targetScale = 1.0f;

    public GuiButton(int buttonId, int x, int y, String buttonText)
    {
        this(buttonId, x, y, 200, 18, buttonText);
    }

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText)
    {
        this.width = 200;
        this.height = 20;
        this.enabled = true;
        this.visible = true;
        this.id = buttonId;
        this.xPosition = x;
        this.yPosition = y;
        this.width = widthIn;
        this.height = heightIn;
        this.displayString = buttonText;
    }

    protected int getHoverState(boolean mouseOver)
    {
        int i = 1;

        if (!this.enabled)
        {
            i = 0;
        }
        else if (mouseOver)
        {
            i = 2;
        }

        return i;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            this.mouseDragged(mc, mouseX, mouseY);


            targetScale = hovered ? 1.05f : 1.0f;
            scale += (targetScale - scale) * 0.2f;

            Blur.startBlur();
            if (mc.currentScreen instanceof SelectThemeMenu) {
                RoundedUtil.drawShaderRound(xPosition + 1, yPosition + 1, width - 2, height - 2, 8, Color.BLACK);
            } else {
                GL11.glPushMatrix();
                GL11.glTranslatef(xPosition + width / 2f, yPosition + height / 2f, 0);
                GL11.glScalef(scale, scale, 1);
                GL11.glTranslatef(-(xPosition + width / 2f), -(yPosition + height / 2f), 0);
                RoundedUtil.drawShaderRound(xPosition, yPosition, width, height, 8, Color.BLACK);
                GL11.glPopMatrix();
            }
            Blur.endBlur(10, 1);
            
            if (mc.currentScreen instanceof SelectThemeMenu) {
                guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
                guiFrameBuffer.framebufferClear();
                guiFrameBuffer.bindFramebuffer(true);
                RoundedUtil.drawShaderRound(xPosition + 1, yPosition + 1, width - 2, height - 2, 8, Color.BLACK);
                guiFrameBuffer.unbindFramebuffer();
                Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 6, Color.BLACK.getRGB());
            } else {
                guiFrameBuffer = RenderUtil.createFrameBuffer(guiFrameBuffer, true);
                guiFrameBuffer.framebufferClear();
                guiFrameBuffer.bindFramebuffer(true);
                GL11.glPushMatrix();
                GL11.glTranslatef(xPosition + width / 2f, yPosition + height / 2f, 0);
                GL11.glScalef(scale, scale, 1);
                GL11.glTranslatef(-(xPosition + width / 2f), -(yPosition + height / 2f), 0);
                RoundedUtil.drawShaderRound(xPosition, yPosition, width, height, 8, Color.BLACK);
                GL11.glPopMatrix();
                guiFrameBuffer.unbindFramebuffer();
                Bloom.renderBloom(guiFrameBuffer.framebufferTexture, 3, Color.BLACK.getRGB());
            }

            if (mc.currentScreen instanceof SelectThemeMenu) {
                RoundedUtil.drawRoundOutline(xPosition, yPosition, width, height, 8, 0.01f, new Color(10, 10, 10, 100), new Color(30, 30, 30, 150));
            } else {
                GL11.glPushMatrix();
                GL11.glTranslatef(xPosition + width / 2f, yPosition + height / 2f, 0);
                GL11.glScalef(scale, scale, 1);
                GL11.glTranslatef(-(xPosition + width / 2f), -(yPosition + height / 2f), 0);
                RoundedUtil.drawRound(xPosition, yPosition, width, height, 8, new Color(15, 15, 15, 100));
                GL11.glPopMatrix();
            }

            Fonts.rubik18.drawCenteredStringWithShadow(this.displayString, this.xPosition + (float) this.width / 2, this.yPosition + (float) (this.height) / 2 - 2, new Color(216, 216, 216, 255).getRGB());
        }
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
    }

    public void mouseReleased(int mouseX, int mouseY)
    {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        return this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
    }

    public boolean isMouseOver()
    {
        return this.hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY)
    {
    }

    public void playPressSound(SoundHandler soundHandlerIn)
    {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public int getButtonWidth()
    {
        return this.width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }
}
