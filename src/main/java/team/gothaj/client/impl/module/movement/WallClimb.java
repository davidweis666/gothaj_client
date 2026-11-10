package team.gothaj.client.impl.module.movement;

import net.minecraft.block.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.player.block.BlockCollisionEvent;
import team.gothaj.eventbus.Listen;

public class WallClimb extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Normal", "Normal", "PolarRape");

    private final ListProperty options = new ListProperty("Collides", "Fence", "Wall")
            .setVisible(() -> mode.is("Normal"));

    public WallClimb() {
        super("WallClimb", "Allows you to climb on walls like spider-man", ModuleCategory.MOVEMENT);
        registerProperties(mode, options);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Listen
    public void onMotion(MotionEvent event) {
        if (!mode.is("Normal")) return;
        if (!mc.thePlayer.isCollidedHorizontally) return;

        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        if (options.getOptionState("Fence")) {
            Block blockAbove = mc.theWorld.getBlockState(pos.up()).getBlock();
            if (blockAbove instanceof BlockFence) {
                mc.thePlayer.motionY = 0.6f;
                event.setOnGround(true);
            }
        }

        if (options.getOptionState("Wall")) {
            if (isCollidedWithWall(pos)) {
                mc.thePlayer.motionY = 0.6f;
                event.setOnGround(true);
            }
        }
    }

    @Listen
    public void onBlockCollision(BlockCollisionEvent event) {
        if (!mode.is("PolarRape")) return;
        if (mc.thePlayer == null) return;

        AxisAlignedBB bb = event.getAxisAlignedBB();
        if (bb == null) return;

        if (event.getPos().getY() >= mc.thePlayer.posY
                || (mc.thePlayer.isSneaking() && mc.thePlayer.onGround)) {
            event.setAxisAlignedBB(new AxisAlignedBB(
                    bb.minX + 0.0001, bb.minY, bb.minZ + 0.0001,
                    bb.maxX - 0.0001, bb.maxY, bb.maxZ - 0.0001));
        }
    }

    private boolean isCollidedWithWall(BlockPos pos) {
        for (BlockPos adjacent : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            Block block = mc.theWorld.getBlockState(adjacent).getBlock();
            if (block instanceof BlockWall) return true;
        }
        return false;
    }
}
