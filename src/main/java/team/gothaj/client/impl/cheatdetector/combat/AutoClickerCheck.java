package team.gothaj.client.impl.cheatdetector.combat;

import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class AutoClickerCheck extends Check implements Utils {

    private final Map<Integer, PlayerClickData> playerData = new HashMap<>();

    private static class PlayerClickData {
        long[] swingTimes = new long[30];
        int swingIndex = 0;
        int swingCount = 0;
        boolean wasSwinging = false;
        int consistentIntervalCount = 0;
        long lastSwingTime = 0;
        long[] intervals = new long[20];
        int intervalIndex = 0;
    }

    public AutoClickerCheck() {
        super("AutoClicker", 4, 10);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null) return false;

        int playerId = player.getEntityId();
        PlayerClickData data = playerData.computeIfAbsent(playerId, k -> new PlayerClickData());

        long now = System.currentTimeMillis();
        boolean flagged = false;

        if (player.isSwingInProgress && !data.wasSwinging) {
            data.swingTimes[data.swingIndex] = now;
            data.swingIndex = (data.swingIndex + 1) % data.swingTimes.length;
            data.swingCount++;

            if (data.lastSwingTime > 0) {
                long interval = now - data.lastSwingTime;
                data.intervals[data.intervalIndex] = interval;
                data.intervalIndex = (data.intervalIndex + 1) % data.intervals.length;
            }
            data.lastSwingTime = now;
        }
        data.wasSwinging = player.isSwingInProgress;

        int clicksInLastSecond = 0;
        for (long time : data.swingTimes) {
            if (time > 0 && now - time < 1000) {
                clicksInLastSecond++;
            }
        }

        if (clicksInLastSecond > 20) {
            flagged = true;
        }

        if (data.swingCount > 15) {
            long avgInterval = 0;
            int validIntervals = 0;
            for (long interval : data.intervals) {
                if (interval > 0 && interval < 500) {
                    avgInterval += interval;
                    validIntervals++;
                }
            }

            if (validIntervals > 10) {
                avgInterval /= validIntervals;

                long variance = 0;
                for (long interval : data.intervals) {
                    if (interval > 0 && interval < 500) {
                        variance += Math.abs(interval - avgInterval);
                    }
                }
                variance /= validIntervals;

                if (variance < 5 && avgInterval < 150) {
                    data.consistentIntervalCount++;
                } else {
                    data.consistentIntervalCount = Math.max(0, data.consistentIntervalCount - 1);
                }

                if (data.consistentIntervalCount > 8) {
                    flagged = true;
                }
            }
        }

        if (player.isSwingInProgress) {
            if (player.isUsingItem() && clicksInLastSecond > 10) {
                flagged = true;
            }
        }

        return flagged;
    }
}
