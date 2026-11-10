package team.gothaj.client.impl.cheatdetector.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class KillAuraCheck extends Check implements Utils {

    private final Map<Integer, KillAuraData> playerData = new HashMap<>();

    private static class KillAuraData {
        float lastYaw;
        float lastPitch;
        long lastUpdate;
        int lockedTicks = 0;
        int multiTargetTicks = 0;
        int pitchUnresponsiveTicks = 0;
        int oscillationStreak = 0;
        float lastYawDeltaDir = 0;
        boolean lastSign = false;
        int oscillationCount = 0;
    }

    public KillAuraCheck() {
        super("KillAura", 4, 10);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null || player == mc.thePlayer) return false;

        int id = player.getEntityId();
        KillAuraData data = playerData.computeIfAbsent(id, k -> new KillAuraData());

        long now = System.currentTimeMillis();
        if (data.lastUpdate == 0) {
            data.lastYaw = player.rotationYawHead;
            data.lastPitch = player.rotationPitch;
            data.lastUpdate = now;
            return false;
        }

        long deltaTime = now - data.lastUpdate;
        if (deltaTime < 5) return false;

        float yaw = player.rotationYawHead;
        float pitch = player.rotationPitch;
        float yawDelta = MathHelper.wrapAngleTo180_float(yaw - data.lastYaw);
        float pitchDelta = pitch - data.lastPitch;
        float absYawDelta = Math.abs(yawDelta);
        float absPitchDelta = Math.abs(pitchDelta);

        int nearbyEntities = 0;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == player || entity == mc.thePlayer || !(entity instanceof EntityPlayer)) continue;
            if (player.getDistanceToEntity(entity) < 8.0) {
                nearbyEntities++;
            }
        }

        boolean flagged = false;

        if (nearbyEntities == 0) {
            data.lockedTicks = 0;
            data.multiTargetTicks = 0;
            data.pitchUnresponsiveTicks = 0;
            data.oscillationCount = 0;
            data.lastYaw = yaw;
            data.lastPitch = pitch;
            data.lastUpdate = now;
            return false;
        }

        if (absYawDelta < 0.3f && absPitchDelta < 0.3f) {
            data.lockedTicks++;
        } else {
            data.lockedTicks = Math.max(0, data.lockedTicks - 1);
        }

        if (data.lockedTicks > 30) {
            flagged = true;
        }

        if (absYawDelta > 10f && absPitchDelta < 0.3f) {
            data.pitchUnresponsiveTicks++;
        } else {
            data.pitchUnresponsiveTicks = Math.max(0, data.pitchUnresponsiveTicks - 1);
        }

        if (data.pitchUnresponsiveTicks > 8) {
            flagged = true;
        }

        if (absYawDelta > 15f) {
            boolean currentSign = yawDelta > 0;
            if (data.lastYawDeltaDir > 15f) {
                boolean lastSign = data.lastSign;
                if (currentSign != lastSign) {
                    data.oscillationStreak++;
                    data.oscillationCount++;
                } else {
                    data.oscillationStreak = 0;
                }
            } else {
                data.oscillationStreak = 0;
            }
            data.lastYawDeltaDir = absYawDelta;
            data.lastSign = currentSign;
        } else {
            data.oscillationStreak = 0;
        }

        if (data.oscillationStreak > 4 && nearbyEntities >= 2) {
            flagged = true;
        }

        data.lastYaw = yaw;
        data.lastPitch = pitch;
        data.lastUpdate = now;

        return flagged;
    }
}
