package team.gothaj.client.impl.cheatdetector.combat;

import net.minecraft.entity.player.EntityPlayer;
import team.gothaj.client.api.cheatdetector.Check;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.game.MovementUtil;

/**
 * @author vajzii
 * @since 2025
 */
public class AutoBlockCheck extends Check implements Utils {


    private int blockTime = 0;
    private int releaseTime = 0;
    private long lastBlockStart = 0;

    public AutoBlockCheck() {
        super("AutoBlock", 2, 6);
    }


    @Override
    public boolean checkPlayer(EntityPlayer player) {
        boolean isBlocking = player.isBlocking();
        boolean isSwing = player.isSwingInProgress;

        if (isBlocking) {
            if (blockTime == 0) {
                lastBlockStart = System.currentTimeMillis();
            }
            blockTime++;
            releaseTime = 0;
        } else {
            releaseTime++;
            if (releaseTime > 6) {
                blockTime = 0;
            }
        }

        boolean instantBlock = false;
        if (isSwing && isBlocking) {
            if (blockTime <= 2) {
                instantBlock = true;
            }
        }


        boolean autoblockPattern = blockTime > 5 && isBlocking && isSwing;

        boolean longBlock = blockTime > 30 && isBlocking;

        return MovementUtil.getSpeed(player) > MovementUtil.getBaseMoveSpeed(player) && (autoblockPattern || longBlock);
    }

}
