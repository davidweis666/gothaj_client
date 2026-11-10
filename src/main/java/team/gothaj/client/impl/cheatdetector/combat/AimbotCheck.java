package team.gothaj.client.impl.cheatdetector.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class AimbotCheck extends Check implements Utils {

    private final Map<Integer, PlayerRotationData> playerData = new HashMap<>();

    private static class PlayerRotationData {
        float lastYaw = 0f;
        float lastPitch = 0f;
        float lastYawDelta = 0f;
        float lastPitchDelta = 0f;
        long lastUpdateTime = 0;
        int constantSpeedTicks = 0;
        int snapCount = 0;
        int perfectTrackingTicks = 0;
        float[] recentSpeeds = new float[20];
        int speedIndex = 0;
        float lastGcdYaw = 0f;
        float lastGcdPitch = 0f;
        int invalidGcdCount = 0;
    }

    public AimbotCheck() {
        super("Aimbot", 4, 12);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null) return false;

        int playerId = player.getEntityId();
        PlayerRotationData data = playerData.computeIfAbsent(playerId, k -> new PlayerRotationData());

        long now = System.currentTimeMillis();
        if (data.lastUpdateTime == 0) {
            data.lastYaw = player.rotationYawHead;
            data.lastPitch = player.rotationPitch;
            data.lastUpdateTime = now;
            return false;
        }

        long deltaTime = now - data.lastUpdateTime;
        if (deltaTime < 10) return false;

        float yawDelta = MathHelper.wrapAngleTo180_float(player.rotationYawHead - data.lastYaw);
        float pitchDelta = player.rotationPitch - data.lastPitch;

        float absYawDelta = Math.abs(yawDelta);
        float absPitchDelta = Math.abs(pitchDelta);
        float totalSpeed = (float) Math.sqrt(absYawDelta * absYawDelta + absPitchDelta * absPitchDelta);

        boolean flagged = false;

        if (totalSpeed > 45f && deltaTime < 60) {
            data.snapCount++;
            if (data.snapCount > 3) {
                flagged = true;
            }
        } else {
            data.snapCount = Math.max(0, data.snapCount - 1);
        }

        data.recentSpeeds[data.speedIndex] = totalSpeed;
        data.speedIndex = (data.speedIndex + 1) % data.recentSpeeds.length;

        if (totalSpeed > 2f) {
            float avgSpeed = 0f;
            int validSamples = 0;
            for (float s : data.recentSpeeds) {
                if (s > 0.5f) {
                    avgSpeed += s;
                    validSamples++;
                }
            }

            if (validSamples > 10) {
                avgSpeed /= validSamples;

                float variance = 0f;
                for (float s : data.recentSpeeds) {
                    if (s > 0.5f) {
                        variance += Math.abs(s - avgSpeed);
                    }
                }
                variance /= validSamples;

                if (variance < 0.3f && avgSpeed > 3f) {
                    data.constantSpeedTicks++;
                    if (data.constantSpeedTicks > 15) {
                        flagged = true;
                    }
                } else {
                    data.constantSpeedTicks = Math.max(0, data.constantSpeedTicks - 2);
                }
            }
        }

        float accel = Math.abs(totalSpeed - data.lastYawDelta);
        if (accel > 20f && totalSpeed > 5f && data.lastYawDelta > 5f) {
            data.perfectTrackingTicks++;
        } else {
            data.perfectTrackingTicks = Math.max(0, data.perfectTrackingTicks - 1);
        }

        if (data.perfectTrackingTicks > 8) {
            flagged = true;
        }

        float gcdYaw = gcd(absYawDelta, data.lastGcdYaw);

        if (absYawDelta > 0.5f && gcdYaw < 0.01f) {
            data.invalidGcdCount++;
            if (data.invalidGcdCount > 20) {
                flagged = true;
            }
        } else {
            data.invalidGcdCount = Math.max(0, data.invalidGcdCount - 1);
        }

        data.lastGcdYaw = absYawDelta;
        data.lastGcdPitch = absPitchDelta;

        data.lastYaw = player.rotationYawHead;
        data.lastPitch = player.rotationPitch;
        data.lastYawDelta = totalSpeed;
        data.lastPitchDelta = absPitchDelta;
        data.lastUpdateTime = now;

        return flagged;
    }

    private float gcd(float a, float b) {
        if (a < 0.001f) return b;
        if (b < 0.001f) return a;
        while (b > 0.001f) {
            float temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}
