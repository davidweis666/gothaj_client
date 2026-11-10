package team.gothaj.client.impl.module.visual;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.render.RenderEntityEvent;
import team.gothaj.client.impl.module.client.Targets;
import team.gothaj.eventbus.Listen;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class Chams extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Legacy", "Legacy");

    public static final BooleanProperty throughWalls = new BooleanProperty("Through Walls", true);

    public static final ListProperty elements = new ListProperty("Elements", "Visible", "Occluded");

    public static final ModeProperty vColorMode = new ModeProperty("Visible Color Mode", "Hud", "Hud", "Custom", "Team").setVisible(() -> elements.getOptionState("Visible"));

    public static final ColorProperty vColor = new ColorProperty("Visible Color", Color.WHITE).setVisible(() -> vColorMode.is("Custom")).withDependency(vColorMode);

    private static final NumberProperty vAlpha = new NumberProperty("Visible Alpha", 90, 1, 255, 1).setVisible(() -> elements.getOptionState("Visible") && !vColorMode.is("Custom")).withDependency(vColorMode);

    public static final ModeProperty oColorMode = new ModeProperty("Occluded Color Mode", "Hud", "Hud", "Custom", "Team").setVisible(() -> elements.getOptionState("Occluded"));

    public static final ColorProperty oColor = new ColorProperty("Occluded Color", Color.WHITE).setVisible(() -> oColorMode.is("Custom")).withDependency(oColorMode);

    private static final NumberProperty oAlpha = new NumberProperty("Occluded Alpha", 90, 1, 255, 1).setVisible(() -> elements.getOptionState("Occluded") && !oColorMode.is("Custom")).withDependency(oColorMode);

    private static final BooleanProperty hudAnimated = new BooleanProperty("Animated Color", true).setVisible(() -> vColorMode.is("Hud") || oColorMode.is("Hud"));


    public Chams() {
        super("Chams", "", ModuleCategory.VISUAL);
        registerProperties(mode, throughWalls, elements, vColorMode, vColor, vAlpha, oColorMode, oColor, oAlpha, hudAnimated);
    }


    @Listen
    public void onRenderEntity(RenderEntityEvent event) {
        if (mode.is("Legacy") && throughWalls.getProperty()) {
            if (event.getEntity() instanceof EntityLivingBase ent) {
                if (isValid(ent)) {
                    event.setPreRenderCallback(this::preRenderCallback);
                    event.setPostRenderCallback(this::postRenderCallback);
                }
            }
        }
    }

    public static void preRender(EntityLivingBase entity) {
        if (isValid(entity)) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LIGHTING);
            OpenGlHelper.setLightmapTextureCoords(1, 240.0f, 240.0f);
            GL11.glDepthMask(false);
            int color = oColorMode.is("Team") ? ColorUtil.getTeamColor(entity)
                    : oColorMode.is("Hud") ? hudAnimated.getProperty() ? Hud.getColor(5) : Hud.primaryColor.getProperty().getRGB()
                    : oColor.getProperty().getRGB();
            RenderUtil.color(ColorUtil.setAlpha(color, oAlpha.getProperty().intValue()));
        }
    }

    public static void render(EntityLivingBase entity) {
        if (isValid(entity)) {
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_LIGHTING);
            int color = vColorMode.is("Team") ? ColorUtil.getTeamColor(entity)
                    : vColorMode.is("Hud") ? hudAnimated.getProperty() ? Hud.getColor(7) : Hud.secondaryColor.getProperty().getRGB()
                    : vColor.getProperty().getRGB();
            RenderUtil.color(ColorUtil.setAlpha(color, vAlpha.getProperty().intValue()));
        }
    }

    public static void postRender(EntityLivingBase entity) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }


    private void preRenderCallback() {
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1, -1_000_000);
    }

    private void postRenderCallback() {
        glPolygonOffset(1, 1_000_000);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    public static boolean isValid(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer || entity.deathTime > 0 || !entity.isEntityAlive() || entity.getHealth() == 0) return false;

        if (entity instanceof EntityArmorStand) return false;

        String name = entity.getDisplayName().getUnformattedText();
        if (name.contains("Shop") || name.contains("Keeper")) return false;

        if (entity instanceof EntityPlayer pl) {
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView != 0) return true;
            if (!Targets.visualTargets.getOptionState("Players")) return false;
            if (EntityUtil.isBot(pl) && !Targets.visualTargets.getOptionState("Bots")) return false;
        }

        if (entity instanceof IMob && !Targets.visualTargets.getOptionState("Monsters")) return false;

        Team entityTeam = entity.getTeam();
        Team playerTeam = mc.thePlayer.getTeam();
        boolean isTeammate = EntityUtil.isTeammate(entity) || (entityTeam != null && playerTeam != null && entityTeam.isSameTeam(playerTeam));

        if (isTeammate && !Targets.visualTargets.getOptionState("Teammates")) {
            return false;
        }

        if ((entity instanceof IAnimals || entity instanceof EntityAgeable) && !Targets.visualTargets.getOptionState("Animals")) return false;

        return !entity.isInvisible() || Targets.visualTargets.getOptionState("Invisibles");
    }
}
