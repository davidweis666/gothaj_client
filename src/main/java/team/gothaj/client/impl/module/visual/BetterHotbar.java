package team.gothaj.client.impl.module.visual;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class BetterHotbar extends Module {

    public final ListProperty barOpt = new ListProperty("Tab Options", "Rounded", "Shaders", "Custom Font");

    public BetterHotbar() {
        super("BetterHotbar", "", ModuleCategory.VISUAL);
        registerProperties(barOpt);
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (mc.thePlayer.isSpectator()) return;
        RenderUtil.checkGLErr("BetterHotbar.enter");
        mc.ingameGUI.renderTooltip(new ScaledResolution(mc), event.getPartialTicks(), false);
        RenderUtil.checkGLErr("BetterHotbar.exit");
    }

    @Listen
    public void onShader(ShaderOverlayEvent event) {
        if (mc.thePlayer.isSpectator()) return;

        if (barOpt.getOptionState("Shaders")) {
            mc.ingameGUI.renderTooltip(event.getScaledResolution(), mc.timer.renderPartialTicks, true);
        }
    }
}
