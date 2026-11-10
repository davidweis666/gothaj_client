package team.gothaj.client.impl.module.visual;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.client.ShaderProcessing;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.awt.*;

/**
 * @author vajzii
 * @since 2025
 */
public class BetterChat extends Module {

    public final ListProperty chatOpt = new ListProperty("Chat Options", "Rounded", "Shaders", "Custom Font");

    public BetterChat() {
        super("BetterChat", "", ModuleCategory.VISUAL);
        registerProperties(chatOpt);
    }

    @Listen
    public void onRender(RenderOverlayEvent event) {


        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, (float) (event.getScaledResolution().getScaledHeight() - 48), 0.0F);
        mc.ingameGUI.getChatGUI().drawChat(mc.ingameGUI.getUpdateCounter());
        GlStateManager.popMatrix();
        RenderUtil.resetColor();

        if (mc.currentScreen instanceof GuiChat) {
            if (chatOpt.getOptionState("Rounded")) {
                ShaderProcessing shaderProcessing = GothajClient.getInstance().getModuleManager().getAndCast(ShaderProcessing.class);
                if (!shaderProcessing.isToggled() || !shaderProcessing.blur.getProperty() ||  shaderProcessing.blurBackground.getProperty()) {
                    RoundedUtil.drawRound(2, event.getScaledResolution().getScaledHeight() - 16, 325, 12, 5f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
                }
            } else {
                Gui.drawRect(2, event.getScaledResolution().getScaledHeight() - 16, event.getScaledResolution().getScaledWidth() - 2, event.getScaledResolution().getScaledHeight() - 4, Integer.MIN_VALUE);
            }
        }



    }
    @Listen
    public void onShader(ShaderOverlayEvent event) {
        if (event.getType() == ShaderOverlayEvent.Type.GOTHAJ) {
            return;
        }

        if (chatOpt.getOptionState("Shaders")) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableAlpha();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, (float) (event.getScaledResolution().getScaledHeight() - 48), 0.0F);
            mc.ingameGUI.getChatGUI().drawChatBox(mc.ingameGUI.getUpdateCounter());
            GlStateManager.popMatrix();
            RenderUtil.resetColor();

            if (mc.currentScreen instanceof GuiChat) {
                if (chatOpt.getOptionState("Rounded")) {
                    RoundedUtil.drawShaderRound(2, event.getScaledResolution().getScaledHeight() - 16, 325, 12, 5f, Color.BLACK);
                } else {
                    Gui.drawRect(2, event.getScaledResolution().getScaledHeight() - 16, event.getScaledResolution().getScaledWidth() - 2, event.getScaledResolution().getScaledHeight() - 4, Color.BLACK.getRGB());
                }
            }

        }
    }
}
