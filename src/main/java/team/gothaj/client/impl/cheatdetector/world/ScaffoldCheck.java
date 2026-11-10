package team.gothaj.client.impl.cheatdetector.world;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ScaffoldCheck extends Check implements Utils {

    private static final float MIN_SUSPICIOUS_PITCH = 82f;
    private static final int PITCH_FLAG_TICKS = 25;
    private static final int YAW_PATTERN_TICKS = 15;

    private final Map<Integer, ScaffoldData> playerData = new HashMap<>();

    private static class ScaffoldData {
        float lastYaw;
        int highPitchTicks = 0;
        int consistentYawTicks = 0;
        float[] yawDeltas = new float[20];
        int yawDeltaIndex = 0;
        int yawDeltaCount = 0;
    }

    public ScaffoldCheck() {
        super("Scaffold", 3, 8);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null || player == mc.thePlayer) return false;

        int id = player.getEntityId();
        ScaffoldData data = playerData.computeIfAbsent(id, k -> new ScaffoldData());

        float pitch = player.rotationPitch;
        float yaw = player.rotationYawHead;
        float yawDelta = MathHelper.wrapAngleTo180_float(yaw - data.lastYaw);
        float absYawDelta = Math.abs(yawDelta);

        boolean flagged = false;

        if (pitch > MIN_SUSPICIOUS_PITCH) {
            data.highPitchTicks++;
        } else {
            data.highPitchTicks = Math.max(0, data.highPitchTicks - 2);
        }

        if (data.highPitchTicks > PITCH_FLAG_TICKS) {
            flagged = true;
        }

        if (absYawDelta > 0.5f) {
            data.yawDeltas[data.yawDeltaIndex] = absYawDelta;
            data.yawDeltaIndex = (data.yawDeltaIndex + 1) % data.yawDeltas.length;
            data.yawDeltaCount = Math.min(data.yawDeltaCount + 1, data.yawDeltas.length);
        }

        if (data.yawDeltaCount >= 10 && data.highPitchTicks > 10) {
            float avgDelta = 0;
            float maxDelta = 0;
            float minDelta = Float.MAX_VALUE;
            for (int i = 0; i < data.yawDeltaCount; i++) {
                float d = data.yawDeltas[i];
                avgDelta += d;
                if (d > maxDelta) maxDelta = d;
                if (d < minDelta) minDelta = d;
            }
            avgDelta /= data.yawDeltaCount;

            if (maxDelta - minDelta < 2f && avgDelta > 1f && avgDelta < 10f) {
                data.consistentYawTicks++;
            } else {
                data.consistentYawTicks = Math.max(0, data.consistentYawTicks - 1);
            }

            if (data.consistentYawTicks > YAW_PATTERN_TICKS) {
                flagged = true;
            }
        }

        data.lastYaw = yaw;

        return flagged;
    }
}
