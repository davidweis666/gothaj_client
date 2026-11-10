package team.gothaj.client.impl.module.movement;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.block.BlockCollisionEvent;
import team.gothaj.eventbus.Listen;

public class Phase extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Full", "Full", "One Layer");
    private final NumberProperty verticalSpeed = new NumberProperty("Vertical Speed", 0.3f, 0.1f, 2f, 0.05f)
            .setVisible(() -> mode.is("Full"));

    public Phase() {
        super("Phase", "Walk through blocks", ModuleCategory.MOVEMENT);
        registerProperties(mode, verticalSpeed);
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mode.is("One Layer")) {
            mc.thePlayer.noClip = false;
            return;
        }

        if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.thePlayer.motionY = -verticalSpeed.getProperty();
        } else if (mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.thePlayer.motionY = verticalSpeed.getProperty();
        } else {
            mc.thePlayer.motionY = 0;
        }

        mc.thePlayer.noClip = true;
        mc.thePlayer.setSprinting(false);
    }

    @Listen
    public void onBlockCollision(BlockCollisionEvent event) {
        if (!mode.is("One Layer")) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        AxisAlignedBB bb = event.getAxisAlignedBB();
        if (bb == null) return;

        if (!hasMovementInput()) return;
        if (!isBodyLayer(event.getPos())) return;

        AxisAlignedBB playerBox = mc.thePlayer.getEntityBoundingBox();

        if (bb.maxY <= playerBox.minY + 0.05 || bb.minY >= playerBox.maxY - 0.05) return;

        double expand = 0.1;
        boolean xAdjacent = bb.maxX >= playerBox.minX - expand && bb.minX <= playerBox.maxX + expand;
        boolean zAdjacent = bb.maxZ >= playerBox.minZ - expand && bb.minZ <= playerBox.maxZ + expand;
        if (!xAdjacent || !zAdjacent) return;

        event.setAxisAlignedBB(null);
    }

    private boolean isBodyLayer(BlockPos pos) {
        int feet = MathHelper.floor_double(mc.thePlayer.posY);
        int head = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.height - 0.1);
        return pos.getY() >= feet && pos.getY() <= head;
    }

    private boolean hasMovementInput() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.noClip = mode.is("Full");
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.noClip = false;
            mc.thePlayer.motionY = 0;
        }
        super.onDisable();
    }
}