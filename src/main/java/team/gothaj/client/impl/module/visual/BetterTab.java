package team.gothaj.client.impl.module.visual;

import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class BetterTab extends Module {

    public final ListProperty tabOpt = new ListProperty("Tab Options", "Rounded", "Shaders", "Custom Font", "Always Head");

    private boolean shouldRender = false;
    private Scoreboard cachedScoreboard;
    private ScoreObjective cachedObjective;

    public BetterTab() {
        super("BetterTab", "", ModuleCategory.VISUAL);
        registerProperties(tabOpt);
    }

    private void updateTabState() {
        cachedScoreboard = this.mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = null;
        ScorePlayerTeam scoreplayerteam = cachedScoreboard.getPlayersTeam(mc.thePlayer.getName());

        if (scoreplayerteam != null) {
            int i1 = scoreplayerteam.getChatFormat().getColorIndex();

            if (i1 >= 0) {
                scoreobjective = cachedScoreboard.getObjectiveInDisplaySlot(3 + i1);
            }
        }

        cachedObjective = scoreobjective != null ? scoreobjective : cachedScoreboard.getObjectiveInDisplaySlot(1);
        cachedObjective = cachedScoreboard.getObjectiveInDisplaySlot(0);

        boolean isKeyDown = mc.gameSettings.keyBindPlayerList.isKeyDown() &&
                (!mc.isIntegratedServerRunning() || mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1 || cachedObjective != null);

        mc.ingameGUI.getTabList().updatePlayerList(isKeyDown);

        shouldRender = isKeyDown || mc.ingameGUI.getTabList().getOpenScale() > 0.0f;
    }

    @Listen
    public void onRender(RenderOverlayEvent event) {
        updateTabState();

        if (shouldRender) {
            mc.ingameGUI.getTabList().renderPlayerlist((int) event.getScaledResolution().getScaledWidth(), cachedScoreboard, cachedObjective);
        }
    }

    @Listen
    public void onShader(ShaderOverlayEvent event) {
        if (tabOpt.getOptionState("Shaders") && shouldRender) {
            mc.ingameGUI.getTabList().renderPlayerlistShader(event.getScaledResolution().getScaledWidth(), cachedScoreboard, cachedObjective);
        }
    }
}
