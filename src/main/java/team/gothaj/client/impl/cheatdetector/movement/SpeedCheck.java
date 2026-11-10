package team.gothaj.client.impl.cheatdetector.movement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class SpeedCheck extends Check implements Utils {

    private final Map<Integer, SpeedData> playerData = new HashMap<>();

    private static class SpeedData {
        double lastPosX;
        double lastPosY;
        double lastPosZ;
        boolean lastOnGround;
        int groundSpeedTicks = 0;
        int airSpeedTicks = 0;
        int timerFlagTicks = 0;
        int consecutiveHighSpeed = 0;
    }

    public SpeedCheck() {
        super("Speed", 3, 8);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null || player == mc.thePlayer) return false;

        int id = player.getEntityId();
        SpeedData data = playerData.computeIfAbsent(id, k -> new SpeedData());

        double deltaX = player.posX - data.lastPosX;
        double deltaZ = player.posZ - data.lastPosZ;
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double maxGroundSpeed = 0.34;
        double maxAirSpeed = 0.46;

        if (player.isPotionActive(Potion.moveSpeed)) {
            int amp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            double multiplier = 1.0 + (0.2 * (amp + 1));
            maxGroundSpeed *= multiplier;
            maxAirSpeed *= multiplier;
        }

        if (player.isSprinting()) {
            maxGroundSpeed *= 1.3;
        }

        boolean onGround = player.onGround;
        boolean flagged = false;

        if (onGround) {
            if (horizontalSpeed > maxGroundSpeed * 1.2) {
                data.groundSpeedTicks++;
            } else {
                data.groundSpeedTicks = Math.max(0, data.groundSpeedTicks - 1);
            }

            if (data.groundSpeedTicks > 3) {
                flagged = true;
            }
        } else {
            if (horizontalSpeed > maxAirSpeed * 1.2) {
                data.airSpeedTicks++;
            } else {
                data.airSpeedTicks = Math.max(0, data.airSpeedTicks - 1);
            }

            if (data.airSpeedTicks > 3) {
                flagged = true;
            }
        }

        if (data.lastOnGround && !onGround) {
            double airSpeed = horizontalSpeed;
            double lastGroundSpeed = Math.sqrt(
                Math.pow(data.lastPosX - player.lastTickPosX, 2) +
                Math.pow(data.lastPosZ - player.lastTickPosZ, 2)
            );

            if (lastGroundSpeed > 0.1 && airSpeed > 0.1) {
                double ratio = airSpeed / lastGroundSpeed;
                if (ratio > 1.3 && ratio < 2.0) {
                    data.timerFlagTicks++;
                } else {
                    data.timerFlagTicks = Math.max(0, data.timerFlagTicks - 1);
                }
            }

            if (data.timerFlagTicks > 10) {
                flagged = true;
            }
        }

        if (!flagged) {
            data.consecutiveHighSpeed = Math.max(0, data.consecutiveHighSpeed - 1);
        }

        data.lastPosX = player.posX;
        data.lastPosY = player.posY;
        data.lastPosZ = player.posZ;
        data.lastOnGround = onGround;

        return flagged;
    }
}
