package team.gothaj.client.impl.cheatdetector.player;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.cheatdetector.Check;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakeLagCheck extends Check {

    private final Map<UUID, Long> lastPacketTime = new HashMap<>();

    private static final long MAX_DELAY_MS = 150;
    private static final double MIN_BURST_DISTANCE = 4.0;

    public FakeLagCheck() {
        super("FakeLag", 3, 5);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {

        UUID uid = player.getUniqueID();
        long now = System.currentTimeMillis();

        long lastTime = lastPacketTime.getOrDefault(uid, now);
        long delay = now - lastTime;

        lastPacketTime.put(uid, now);

        if (delay > MAX_DELAY_MS) {
            double deltaX = player.posX - player.lastTickPosX;
            double deltaY = player.posY - player.lastTickPosY;
            double deltaZ = player.posZ - player.lastTickPosZ;

            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            if (distance > MIN_BURST_DISTANCE) {


                NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(uid);

                if (info == null) return false;

                int ping = info.getResponseTime();
                 if (ping < 100) return true;
                
                return true;
            }
        }

        return false;
    }
}
