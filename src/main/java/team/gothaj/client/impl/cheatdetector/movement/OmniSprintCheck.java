package team.gothaj.client.impl.cheatdetector.movement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.cheatdetector.Check;

public class OmniSprintCheck extends Check {

    public OmniSprintCheck() {
        super("OmniSprint", 2, 4);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        return false;

        //TODO:: Correct check

//        if (!player.isSprinting()) {
//            return false;
//        }
//        if (player.isRiding() || player.capabilities.isFlying) {
//            return false;
//        }
//
//        double deltaX = player.posX - player.lastTickPosX;
//        double deltaZ = player.posZ - player.lastTickPosZ;
//
//        double speed = Math.hypot(deltaX, deltaZ);
//
//        if (speed < 0.3) {
//            return false;
//        }
//
//        double moveYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
//
//        double diff = Math.abs(MathHelper.wrapAngleTo180_double(moveYaw - player.rotationYaw));
//
//        return diff > 90.0;
    }
}
