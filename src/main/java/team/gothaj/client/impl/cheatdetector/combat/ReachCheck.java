package team.gothaj.client.impl.cheatdetector.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.rotation.RotationUtil;

import java.util.HashMap;
import java.util.Map;

public class ReachCheck extends Check implements Utils {

    private static final double MAX_LEGIT_REACH = 3;

    private final Map<Integer, PlayerReachData> playerData = new HashMap<>();

    private static class PlayerReachData {
        double[] hitDistances = new double[15];
        int hitIndex = 0;
        int suspiciousHits = 0;
        long lastHitTime = 0;
    }

    public ReachCheck() {
        super("Reach", 5, 15);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null) return false;

        int playerId = player.getEntityId();
        PlayerReachData data = playerData.computeIfAbsent(playerId, k -> new PlayerReachData());

        if (!player.isSwingInProgress) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < 100) return false;

        EntityPlayer target = null;
        double closestDist = Double.MAX_VALUE;

        for (EntityPlayer other : mc.theWorld.playerEntities) {
            if (other == player) continue;

            double dist = player.getDistanceToEntity(other);
            if (dist < closestDist && dist < 8.0) {
                float[] rots = RotationUtil.getAngles(player, other);
                float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(player.rotationYawHead - rots[0]));
                float pitchDiff = Math.abs(player.rotationPitch - rots[1]);

                if (yawDiff < 30f && pitchDiff < 30f) {
                    target = other;
                    closestDist = dist;
                }
            }
        }

        if (target == null) return false;

        double hitDistance = calculateHitDistance(player, target);

        data.hitDistances[data.hitIndex] = hitDistance;
        data.hitIndex = (data.hitIndex + 1) % data.hitDistances.length;
        data.lastHitTime = now;

        boolean flagged = false;

        if (hitDistance > MAX_LEGIT_REACH && target.hurtTime > 0) {
            data.suspiciousHits++;

            if (data.suspiciousHits > 5) {
                double avgDist = 0;
                int validHits = 0;
                for (double d : data.hitDistances) {
                    if (d > 0) {
                        avgDist += d;
                        validHits++;
                    }
                }

                if (validHits > 5) {
                    avgDist /= validHits;

                    if (avgDist > 3.3) {
                        flagged = true;
                    }
                }
            }
        } else {
            data.suspiciousHits = Math.max(0, data.suspiciousHits - 1);
        }

        return flagged;
    }

    private double calculateHitDistance(EntityPlayer attacker, EntityPlayer target) {
        double eyeX = attacker.posX;
        double eyeY = attacker.posY + attacker.getEyeHeight();
        double eyeZ = attacker.posZ;

        double closestX = MathHelper.clamp(eyeX, target.getEntityBoundingBox().minX, target.getEntityBoundingBox().maxX);
        double closestY = MathHelper.clamp(eyeY, target.getEntityBoundingBox().minY, target.getEntityBoundingBox().maxY);
        double closestZ = MathHelper.clamp(eyeZ, target.getEntityBoundingBox().minZ, target.getEntityBoundingBox().maxZ);

        double dx = eyeX - closestX;
        double dy = eyeY - closestY;
        double dz = eyeZ - closestZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
