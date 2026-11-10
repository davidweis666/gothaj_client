package team.gothaj.client.impl.module.misc;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.eventbus.Listen;
import team.gothaj.eventbus.Priority;

/**
 * @author vajzii
 * @since 2025
 */
public class HealthFix extends Module {

    public HealthFix() {
        super("HealthFix", "", ModuleCategory.MISC);
    }

    @Listen(priority = Priority.CRITICAL)
    public void onRender(RenderOverlayEvent event) {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = scoreboard.getObjectiveInDisplaySlot(0);
        if (mc.theWorld != null && scoreobjective != null) {
            for (EntityPlayer ep : mc.theWorld.playerEntities) {
                if (ep instanceof EntityOtherPlayerMP player) {

                    if (player.isEntityAlive() && (mc.getCurrentServerData().serverIP.contains("jartex") || mc.getCurrentServerData().serverIP.contains("gamster") || mc.getCurrentServerData().serverIP.contains("pika"))) {
                        int scoreHealth = scoreobjective.getScoreboard().getValueFromObjective(player.getGameProfile().getName(), scoreobjective).getScorePoints();

                        if (scoreHealth > 0) {
                            player.setHealth(scoreHealth);
                        }
                    }
                }
            }
        }
    }
}
