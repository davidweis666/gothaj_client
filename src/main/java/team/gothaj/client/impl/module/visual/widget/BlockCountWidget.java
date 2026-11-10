package team.gothaj.client.impl.module.visual.widget;

import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.gui.widget.WidgetScreen;
import team.gothaj.client.impl.module.visual.Hud;

import java.awt.*;

import static java.lang.Integer.MIN_VALUE;

/**
 * @author vajzii
 * @since 2025
 */
public class BlockCountWidget extends Widget {

    private float barWidth = 0f;
    @Setter
    private int spoofedSlot = -1;

    public BlockCountWidget() {
        super("Block Count", MIN_VALUE, MIN_VALUE, 154, 30, Type.MODULE);
    }


    private ItemStack getBlock() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) {
            return null;
        }

        int slot = spoofedSlot;
        if (slot < 0 || slot >= mc.thePlayer.inventory.mainInventory.length || mc.thePlayer.inventory.mainInventory[slot] == null) {
            slot = InventoryUtil.getBlockSlot();
        }

        if (slot < 0 || slot >= mc.thePlayer.inventory.mainInventory.length) {
            return null;
        }

        return mc.thePlayer.inventory.mainInventory[slot];
    }

    @Override
    public void onShader(ShaderOverlayEvent event) {
        ItemStack renderStack = getBlock();

        if ((mc.currentScreen instanceof WidgetScreen) || (renderStack != null && renderStack.getItem() instanceof ItemBlock)) {


            RoundedUtil.drawShaderRound((float) x, (float) y, (float) 154, 30f, 9f, Color.BLACK);

        }

        super.onShader(event);

    }

    @Override
    public void onRender(ScaledResolution sr) {
        int blockCount = InventoryUtil.getBlockCount();

        boolean initPosition = x == MIN_VALUE || y == MIN_VALUE;


        if (initPosition) {
            x = sr.getScaledWidth() / 2.0F - 160;
            y = sr.getScaledHeight() / 2.0F + 13;
            width = 154;
        }


        ItemStack renderStack = getBlock();

        if ((mc.currentScreen instanceof WidgetScreen) || (renderStack != null && renderStack.getItem() instanceof ItemBlock)) {

            if (!getShaderProcessing().isToggled() || !getShaderProcessing().blur.getProperty() || getShaderProcessing().blurBackground.getProperty())
                RoundedUtil.drawRound((float) x, (float) y, (float) 154, 30f, 9f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));

            GlStateManager.pushMatrix();
            GlStateManager.scale(1.2F, 1.2F, 1.2F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();

            float s = 1.2F;
            float adjX = (float) (x / s);
            float adjY = (float) (y / s);

            if (renderStack != null) {
                int index = InventoryUtil.findSlot(renderStack.getItem());
                if (index != -1) {
                    mc.ingameGUI.renderBlockCount(index, (int) adjX + 7, (int) adjY + 5);
                }
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableBlend();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();

            float percentage = Math.min(1, blockCount / 64F);

            Color base = Color.WHITE, darker = Color.WHITE;

            ItemBlock itemBlock = renderStack != null && renderStack.getItem() instanceof ItemBlock
                    ? (ItemBlock) renderStack.getItem()
                    : null;

            if (itemBlock != null) {
                Block blockType = itemBlock.getBlock();
                if (blockType == Blocks.wool) {
                    int meta = renderStack.getMetadata();
                    IBlockState state = blockType.getStateFromMeta(meta);
                    int mapRGB = state.getBlock().getMapColor(state).colorValue;
                    base = new Color(mapRGB).brighter();
                    darker = base.darker().darker().darker();
                } else {
                    IBlockState state = blockType.getDefaultState();
                    int mapRGB = state.getBlock().getMapColor(state).colorValue;
                    base = new Color(mapRGB).brighter();
                    darker = base.darker().darker().darker();
                }
            }



            float maxWidth = 85f;
            float target = maxWidth * percentage;
            float alpha = 0.2f;
            barWidth += (target - barWidth) * alpha;

            RoundedUtil.drawRound((float)(x + 35f), (float)(y + 14), maxWidth, 2.5f, 1.5f, new Color(30,30,30,150));
            RoundedUtil.drawGradientHorizontal((float)(x + 35f), (float)(y + 14), barWidth, 2.5f, 1.7f, darker, base);

            Fonts.nunito18.drawString(String.valueOf(blockCount), (float) (x + 142.5 - Fonts.nunito18.getStringWidth(String.valueOf(blockCount))), (float) (y + 14f - Fonts.nunito18.getHeight() / 2f), Color.WHITE);
            Fonts.nunito14.drawString(blockCount == 1 ? "block" : "blocks", (float) (x + (blockCount == 1 ? 134 : blockCount >= 100 ? 132.5 : 132) - Fonts.nunito18.getStringWidth(String.valueOf(blockCount)) / 2), (float) (y + 21f - Fonts.nunito14.getHeight() / 2f), Color.GRAY);
        }

        super.onRender(sr);
    }
}
