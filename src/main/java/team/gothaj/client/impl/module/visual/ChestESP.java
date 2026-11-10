package team.gothaj.client.impl.module.visual;

import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.game.WorldUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.impl.event.player.block.RightClickBlockEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.*;

import static net.minecraft.client.renderer.OpenGlHelper.glUniform1;

/**
 * @author vajzii
 * @since 03.11.2025
 */
public class ChestESP extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Box", "Outline", "Box");
    private final BooleanProperty enderChests = new BooleanProperty("Ender Chests", true);
    private final ColorProperty chestColor = new ColorProperty("Chest Color", Color.WHITE);
    private final ColorProperty enderChestColor = new ColorProperty("Ender Chest Color", Color.GRAY).setVisible(enderChests::getProperty);


    private final Map<BlockPos, Long> fadingChests = new HashMap<>();
    private final Set<BlockPos> permanentlyOpened = new HashSet<>();


    private static final float BASE_FADE_ALPHA = 0.3f;
    private static final long FADE_DURATION_MS = 1000;

    public ChestESP() {
        super("ChestESP", "Renders overlay over chests", ModuleCategory.VISUAL);
        registerProperties(mode, enderChests, chestColor, enderChestColor);
    }



    @Listen
    public void onRenderDimension(RenderDimensionEvent event) {
        for (TileEntity entity : mc.theWorld.loadedTileEntityList) {
            if (entity == null) continue;
            if (!(entity instanceof TileEntityChest || (entity instanceof TileEntityEnderChest && enderChests.getProperty()))) continue;
            BlockPos pos = entity.getPos();
            float alpha = getChestAlpha(pos);
            if (alpha > 0f) renderChest(entity, alpha);
        }
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        WorldUtil.openedChests.clear();
        fadingChests.clear();
        permanentlyOpened.clear();
    }

    @Listen
    public void onRightClickBlock(RightClickBlockEvent event) {
        boolean isChestBlock =
                mc.theWorld.getBlockState(event.getPos()).getBlock() instanceof BlockChest
                        || mc.theWorld.getBlockState(event.getPos()).getBlock() instanceof BlockEnderChest;

        if (!isChestBlock) return;
        if (!canOpenChest(event.getPos())) return;

        long now = System.currentTimeMillis();
        BlockPos clickedPos = event.getPos();
        if (WorldUtil.openedChests.add(clickedPos)) fadingChests.putIfAbsent(clickedPos, now);

        TileEntity tileEntity = mc.theWorld.getTileEntity(clickedPos);
        if (tileEntity instanceof TileEntityChest clickedChest) {
            TileEntityChest connectedChest = getConnectedChest(clickedChest);
            if (connectedChest != null) {
                BlockPos cpos = connectedChest.getPos();
                if (WorldUtil.openedChests.add(cpos)) fadingChests.putIfAbsent(cpos, now);
            }
        }
    }


    private float getChestAlpha(BlockPos pos) {
        if (permanentlyOpened.contains(pos)) return 0f;
        if (WorldUtil.openedChests.contains(pos)) {
            long start = fadingChests.computeIfAbsent(pos, p -> System.currentTimeMillis());
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= FADE_DURATION_MS) {
                permanentlyOpened.add(pos);
                fadingChests.remove(pos);
                WorldUtil.openedChests.remove(pos);
                return 0f;
            }
            float t = elapsed / (float) FADE_DURATION_MS;
            return Math.max(0f, BASE_FADE_ALPHA * (1f - t));
        }
        return BASE_FADE_ALPHA;
    }

    private boolean canOpenChest(BlockPos pos) {
        if (mc.thePlayer.isSneaking()) return false;
        return mc.theWorld.getBlockState(pos.up()).getBlock() instanceof BlockAir
                || mc.theWorld.getBlockState(pos.up()).getBlock() instanceof BlockChest
                || mc.theWorld.getBlockState(pos.up()).getBlock() instanceof BlockEnderChest;
    }

    private TileEntityChest getConnectedChest(TileEntityChest chest) {
        if (chest.adjacentChestXNeg != null) return chest.adjacentChestXNeg;
        if (chest.adjacentChestXPos != null) return chest.adjacentChestXPos;
        if (chest.adjacentChestZNeg != null) return chest.adjacentChestZNeg;
        if (chest.adjacentChestZPos != null) return chest.adjacentChestZPos;
        return null;
    }

    private void renderChest(TileEntity chest, float alpha) {
        if (alpha <= 0f) return;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 0);
        double x = chest.getPos().getX() - mc.getRenderManager().renderPosX;
        double y = chest.getPos().getY() - mc.getRenderManager().renderPosY;
        double z = chest.getPos().getZ() - mc.getRenderManager().renderPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.94, 0.875, 0.94).offset(x, y, z);

        if (chest instanceof TileEntityChest chestEnt) {
            TileEntityChest connectedChest = getConnectedChest(chestEnt);
            if (connectedChest != null) {
                BlockPos cpos = connectedChest.getPos();
                if (getChestAlpha(cpos) > 0f) {
                    double cx = cpos.getX() - mc.getRenderManager().renderPosX;
                    double cy = cpos.getY() - mc.getRenderManager().renderPosY;
                    double cz = cpos.getZ() - mc.getRenderManager().renderPosZ;
                    AxisAlignedBB other = new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.94, 0.875, 0.94).offset(cx, cy, cz);
                    bb = bb.union(other);
                }
            }
        }

        Color baseColor = (chest instanceof TileEntityEnderChest) ? enderChestColor.getProperty() : chestColor.getProperty();
        int argb = ((int) (255 * alpha) << 24) | (baseColor.getRed() << 16) | (baseColor.getGreen() << 8) | baseColor.getBlue();
        RenderUtil.drawAxisAlignedBB(bb, !mode.is("Outline"), mode.is("Outline"), argb);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

}
