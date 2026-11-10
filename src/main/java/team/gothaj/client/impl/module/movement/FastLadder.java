package team.gothaj.client.impl.module.movement;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLadder;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.eventbus.Listen;

public class FastLadder extends Module {

    public FastLadder() {
        super("FastLadder", "Speeds up your movement on ladder", ModuleCategory.MOVEMENT);
    }


    @Listen
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer.isOnLadder() && mc.thePlayer.isCollidedHorizontally && mc.thePlayer.motionY > 0.01) {

            BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

            if (mc.theWorld.getBlockState(pos.up()).getBlock() instanceof BlockLadder) {
                mc.thePlayer.motionY = 0.6f;
                event.setOnGround(true);
            }
        }
    }


}
