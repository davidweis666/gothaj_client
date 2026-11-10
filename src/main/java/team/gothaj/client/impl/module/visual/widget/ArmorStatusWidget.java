package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.states.GlState;
import team.gothaj.client.api.widget.Widget;
import java.awt.*;
import java.util.Arrays;

/**
 * @author ssh444
 * @since 2026
 */
public class ArmorStatusWidget extends Widget {

    public ArmorStatusWidget() {
        super("Armor Status", 8, 132, 120, 50);
    }

    @Override
    public void onRender(ScaledResolution sr) {
        float iconSize = 11f;
        float iconPadding = 2f;
        float yOffset = 0f;
        float scale = 1.2f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(scale, scale, 1f);
        RenderHelper.enableGUIStandardItemLighting();
        final var list = Arrays.asList(mc.thePlayer.inventory.armorInventory).reversed();
        if (list.isEmpty()) return;
        int index = 0;
        for (final var stack : list) {
            var thing = 2f;
            if (list.getFirst() != null && list.getFirst().equals(stack)) thing = 0f;
            if (stack == null) continue;
            mc.getRenderItem().renderItemAndEffectIntoGUI(
                    stack,
                    0,
		            (int) yOffset
            );
            mc.getRenderItem().renderItemOverlayIntoGUI(
                    mc.fontRendererObj,
                    stack,
                    0,
                    (int) (yOffset),
                    null
            );
            GlStateManager.disableDepth();
            Fonts.nunito14.drawString(String.valueOf(++index), iconSize,  yOffset + iconSize - 4f, -1, true);
            GlStateManager.enableDepth();

            yOffset += iconSize + iconPadding + thing;
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        width = iconSize * scale;
        height = yOffset * scale;

        super.onRender(sr);
    }

}
