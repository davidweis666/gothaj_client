package team.gothaj.client.impl.module.movement;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.BlockPos;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.eventbus.Listen;

public class Clipper extends Module {

    private final NumberProperty speed = new NumberProperty("Speed", 0.5f, 0.1f, 2f, 0.05f);

    public Clipper() {
        super("Clipper", "Clip through floors and ceilings", ModuleCategory.MOVEMENT);
        registerProperties(speed);
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        boolean down = mc.gameSettings.keyBindSneak.isKeyDown();
        boolean up = mc.gameSettings.keyBindJump.isKeyDown();

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        if (down && canClipDown(x, y, z)) {
            mc.thePlayer.setPosition(x, y - speed.getProperty(), z);
            mc.thePlayer.noClip = true;
        } else if (up && canClipUp(x, y, z)) {
            mc.thePlayer.setPosition(x, y + speed.getProperty(), z);
            mc.thePlayer.noClip = true;
        }
    }

    @Listen
    public void onRender(RenderOverlayEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        boolean canUp = canClipUp(x, y, z);
        boolean canDown = canClipDown(x, y, z);
        if (!canUp && !canDown) return;

        ScaledResolution sr = event.getScaledResolution();
        float centerX = sr.getScaledWidth() / 2f;
        int color = Hud.getColor(0);

        if (canUp) {
            Fonts.nunito14.drawCenteredStringWithShadow(
                    "Press SPACE to clip UP", centerX, sr.getScaledHeight() - 34, color);
        }
        if (canDown) {
            Fonts.nunito14.drawCenteredStringWithShadow(
                    "Press SHIFT to clip DOWN", centerX, sr.getScaledHeight() - 24, color);
        }
    }

    private boolean canClipDown(double x, double y, double z) {
        return isSolid(x, y - 0.2, z) || isSolid(x, y + 0.2, z) || isSolid(x, y + 0.8, z);
    }

    private boolean canClipUp(double x, double y, double z) {
        return isSolid(x, y + 1.9, z) || isSolid(x, y + 1.2, z);
    }

    private boolean isSolid(double x, double y, double z) {
        Block block = mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
        return block.isFullBlock();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.noClip = false;
        }
        super.onDisable();
    }
}
