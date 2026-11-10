package team.gothaj.client.impl.cheatdetector.world;

import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FastPlaceCheck extends Check {

    private static final long MIN_PLACE_INTERVAL_MS = 200L;
    private static final long TOLERANCE_MS = 20L;
    private static final long DECAY_INTERVAL_MS = 2000L;
    private static final long BLOCK_SWING_WINDOW_MS = 200L;
    private static final double MAX_REACH = 4.5;

    private final Map<Integer, Long> lastPlaceTime = new ConcurrentHashMap<>();

    private final Map<Integer, List<long[]>> pendingBlockChanges = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastFlagTime = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> violationLevel = new ConcurrentHashMap<>();

    public FastPlaceCheck() {
        super("FastPlace", 1, 6);
    }

    @Override
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;

        if (event.getPacket() instanceof S23PacketBlockChange blockPkt) {
            if (blockPkt.getBlockState().getBlock() == Blocks.air) return;

            BlockPos pos = blockPkt.getBlockPosition();
            long now = System.currentTimeMillis();

            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) continue;
                double dist = player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (dist > MAX_REACH * MAX_REACH) continue;

                pendingBlockChanges.computeIfAbsent(player.getEntityId(), k -> new ArrayList<>())
                        .add(new long[]{pos.getX(), pos.getY(), pos.getZ(), now});
            }
        }

        if (event.getPacket() instanceof S0BPacketAnimation animPkt) {
            if (animPkt.getAnimationType() != 0) return;

            Entity entity = mc.theWorld.getEntityByID(animPkt.getEntityID());
            if (!(entity instanceof EntityPlayer player) || player == mc.thePlayer) return;

            int entityId = player.getEntityId();
            long now = System.currentTimeMillis();
            List<long[]> blocks = pendingBlockChanges.get(entityId);

            if (blocks == null || blocks.isEmpty()) {
                return;
            }

            blocks.removeIf(b -> now - b[3] > BLOCK_SWING_WINDOW_MS);

            if (blocks.isEmpty()) {
                return;
            }

            long[] matched = blocks.removeFirst();
            long placementTime = matched[3];

            if (lastPlaceTime.containsKey(entityId)) {
                long delta = placementTime - lastPlaceTime.get(entityId);

                // Time-based decay
                long timeSinceFlag = now - lastFlagTime.getOrDefault(entityId, 0L);
                if (timeSinceFlag > DECAY_INTERVAL_MS) {
                    violationLevel.merge(entityId, -1, (a, b) -> Math.max(getMinVL(), a + b));
                }

                if (delta < MIN_PLACE_INTERVAL_MS - TOLERANCE_MS) {
                    violationLevel.merge(entityId, 1, Integer::sum);
                    lastFlagTime.put(entityId, now);
                }
            }

            lastPlaceTime.put(entityId, placementTime);
        }
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {

        boolean detect = violationLevel.getOrDefault(player.getEntityId(), 0) >= getMinVL();

        if (detect) {
            removePlayer(player);
            return true;
        } else {
            return false;
        }
    }

    public void removePlayer(EntityPlayer player) {
        int id = player.getEntityId();
        lastPlaceTime.remove(id);
        pendingBlockChanges.remove(id);
        lastFlagTime.remove(id);
        violationLevel.remove(id);
    }
}
