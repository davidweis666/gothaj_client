package team.gothaj.client.impl.cheatdetector.movement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.potion.Potion;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class NoSlowCheck extends Check implements Utils {

    private static final double SLOWDOWN_FACTOR = 0.2;

    private final Map<Integer, PlayerSlowData> playerData = new HashMap<>();

    private static class PlayerSlowData {
        double lastPosX = 0;
        double lastPosZ = 0;
        int noSlowTicks = 0;
        int usingItemTicks = 0;
    }

    public NoSlowCheck() {
        super("NoSlow", 4, 10);
    }

    @Override
    public boolean checkPlayer(EntityPlayer player) {
        if (player == null || mc.thePlayer == null) return false;

        int playerId = player.getEntityId();
        PlayerSlowData data = playerData.computeIfAbsent(playerId, k -> new PlayerSlowData());

        double deltaX = player.posX - data.lastPosX;
        double deltaZ = player.posZ - data.lastPosZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        boolean flagged = false;

        if (player.getHeldItem() == null) return false;

        if (player.isUsingItem() && player.getHeldItem().getItem() instanceof ItemBow) return false;

        boolean shouldBeSlow = player.isUsingItem() || player.isBlocking();

        if (shouldBeSlow) {
            data.usingItemTicks++;

            double maxSlowedSpeed = calculateMaxSlowedSpeed(player);

            if (data.usingItemTicks > 3 && speed > maxSlowedSpeed * 1.3) {
                data.noSlowTicks++;
            } else {
                data.noSlowTicks = Math.max(0, data.noSlowTicks - 1);
            }
        } else {
            data.usingItemTicks = 0;
            data.noSlowTicks = Math.max(0, data.noSlowTicks - 1);
        }

        if (data.noSlowTicks > 5) {
            flagged = true;
        }

        data.lastPosX = player.posX;
        data.lastPosZ = player.posZ;

        return flagged;
    }

    private double calculateMaxSlowedSpeed(EntityPlayer player) {
        double baseSpeed = 0.1;

        if (player.isSprinting()) {
            baseSpeed = 0.13;
        }

        if (player.isPotionActive(Potion.moveSpeed)) {
            int amp = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
            baseSpeed *= 1.0 + (0.2 * amp);
        }

        baseSpeed *= SLOWDOWN_FACTOR;
        baseSpeed *= 1.3;

        return baseSpeed;
    }
}
