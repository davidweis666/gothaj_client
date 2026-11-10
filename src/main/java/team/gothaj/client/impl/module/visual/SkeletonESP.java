package team.gothaj.client.impl.module.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.player.SneakSpoofEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.client.Targets;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author vajzii
 * @since 2025
 */
public class SkeletonESP extends Module {

    public final Map<EntityPlayer, float[][]> modelPlayerMap = new HashMap<>();

    public final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    public final ModeProperty colorMode = new ModeProperty("Color Mode", "Hud", "Hud", "Custom", "Team");

    public final ColorProperty customColor = new ColorProperty("Custom Color", Color.WHITE).setVisible(() -> colorMode.is("Custom"));

    public final NumberProperty skeletonWidth = new NumberProperty("Width", 0.7f, 0.1f, 2.0f, 0.1f);

    public SkeletonESP() {
        super("SkeletonESP", "", ModuleCategory.VISUAL);
        registerProperties(colorMode, customColor, skeletonWidth);
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        modelPlayerMap.clear();
    }

    @Listen
    public void onRenderDimensions(RenderDimensionEvent event) {

        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isValid(player) || (mc.gameSettings.thirdPersonView == 0 && player == mc.thePlayer)) {
                continue;
            }

            int color = colorMode.is("Team") ? ColorUtil.getTeamColor(player)
                    : colorMode.is("Hud") ? Hud.primaryColor.getProperty().getRGB()
                    : customColor.getProperty().getRGB();

            glLineWidth(skeletonWidth.getProperty());
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_LINE_SMOOTH);
            RenderUtil.color(color);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_TEXTURE_2D);
            glDepthMask(false);

            drawSkeleton(event.getPartialTicks(), player);

            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            glDisable(GL_LINE_SMOOTH);
            glEnable(GL_DEPTH_TEST);

        }

    }


    private void drawSkeleton(float pt, EntityPlayer player) {
        float[][] entPos;
        if ((entPos = modelPlayerMap.get(player)) != null) {
            glPushMatrix();
            float x = (float) (RenderUtil.interpolate(player.lastTickPosX, player.posX, pt) - RenderManager.renderPosX);
            float y = (float) (RenderUtil.interpolate(player.lastTickPosY, player.posY, pt) - RenderManager.renderPosY);
            float z = (float) (RenderUtil.interpolate(player.lastTickPosZ, player.posZ, pt) - RenderManager.renderPosZ);
            glTranslated(x, y, z);
            final var eventSpoof = new SneakSpoofEvent();
            if (player == Minecraft.getMinecraft().thePlayer) GothajClient.getInstance().getEventBus().fire(eventSpoof);
            boolean sneaking = player.isSneaking() && !eventSpoof.isCancelled();

            final float xOff = RenderUtil.interpolate(player.prevRenderYawOffset, player.renderYawOffset, pt);
            float yOff = sneaking ? 0.6F : 0.75F;
            glRotatef(-xOff, 0.0F, 1.0F, 0.0F);
            glTranslatef(0.0F, 0.0F, sneaking ? -0.235F : 0.0F);

            // Right leg
            glPushMatrix();
            glTranslatef(-0.125F, yOff, 0.0F);
            if (entPos[3][0] != 0.0F)
                glRotatef(entPos[3][0] * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
            if (entPos[3][1] != 0.0F)
                glRotatef(entPos[3][1] * RAD_TO_DEG, 0.0F, 1.0F, 0.0F);
            if (entPos[3][2] != 0.0F)
                glRotatef(entPos[3][2] * RAD_TO_DEG, 0.0F, 0.0F, 1.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3i(0, 0, 0);
            glVertex3f(0.0F, -yOff, 0.0F);
            glEnd();
            glPopMatrix();

            // Left leg
            glPushMatrix();
            glTranslatef(0.125F, yOff, 0.0F);
            if (entPos[4][0] != 0.0F)
                glRotatef(entPos[4][0] * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
            if (entPos[4][1] != 0.0F)
                glRotatef(entPos[4][1] * RAD_TO_DEG, 0.0F, 1.0F, 0.0F);
            if (entPos[4][2] != 0.0F)
                glRotatef(entPos[4][2] * RAD_TO_DEG, 0.0F, 0.0F, 1.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3i(0, 0, 0);
            glVertex3f(0.0F, -yOff, 0.0F);
            glEnd();
            glPopMatrix();

            glTranslatef(0.0F, 0.0F, sneaking ? 0.25F : 0.0F);
            glPushMatrix();
            glTranslatef(0.0F, sneaking ? -0.05F : 0.0F, sneaking ? -0.01725F : 0.0F);

            // Right arm
            glPushMatrix();
            glTranslatef(-0.375F, yOff + 0.55F, 0.0F);
            if (entPos[1][0] != 0.0F)
                glRotatef(entPos[1][0] * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
            if (entPos[1][1] != 0.0F)
                glRotatef(entPos[1][1] * RAD_TO_DEG, 0.0F, 1.0F, 0.0F);
            if (entPos[1][2] != 0.0F)
                glRotatef(-entPos[1][2] * RAD_TO_DEG, 0.0F, 0.0F, 1.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3i(0, 0, 0);
            glVertex3f(0.0F, -0.5F, 0.0F);
            glEnd();
            glPopMatrix();

            // Left arm
            glPushMatrix();
            glTranslatef(0.375F, yOff + 0.55F, 0.0F);
            if (entPos[2][0] != 0.0F)
                glRotatef(entPos[2][0] * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
            if (entPos[2][1] != 0.0F)
                glRotatef(entPos[2][1] * RAD_TO_DEG, 0.0F, 1.0F, 0.0F);
            if (entPos[2][2] != 0.0F)
                glRotatef(-entPos[2][2] * RAD_TO_DEG, 0.0F, 0.0F, 1.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3i(0, 0, 0);
            glVertex3f(0.0F, -0.5F, 0.0F);
            glEnd();
            glPopMatrix();

            glRotatef(xOff - player.rotationYawHead, 0.0F, 1.0F, 0.0F);

            // Head
            glPushMatrix();
            glTranslatef(0.0F, yOff + 0.55F, 0.0F);
            if (entPos[0][0] != 0.0F)
                glRotatef(entPos[0][0] * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3i(0, 0, 0);
            glVertex3f(0.0F, 0.3F, 0.0F);
            glEnd();
            glPopMatrix();

            glPopMatrix();

            glRotatef(sneaking ? 25.0F : 0.0F, 1.0F, 0.0F, 0.0F);
            glTranslatef(0.0F, sneaking ? -0.16175F : 0.0F, sneaking ? -0.48025F : 0.0F);

            // Pelvis
            glPushMatrix();
            glTranslated(0.0F, yOff, 0.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3f(-0.125F, 0.0F, 0.0F);
            glVertex3f(0.125F, 0.0F, 0.0F);
            glEnd();
            glPopMatrix();

            // Body
            glPushMatrix();
            glTranslatef(0.0F, yOff, 0.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3i(0, 0, 0);
            glVertex3f(0.0F, 0.55F, 0.0F);
            glEnd();
            glPopMatrix();

            // Chest
            glPushMatrix();
            glTranslatef(0.0F, yOff + 0.55F, 0.0F);
            glBegin(GL_LINE_STRIP);
            glVertex3f(-0.375F, 0.0F, 0.0F);
            glVertex3f(0.375F, 0.0F, 0.0F);
            glEnd();
            glPopMatrix();

            glPopMatrix();
        }
    }


    public void addEntity(EntityPlayer e, ModelPlayer model) {
        modelPlayerMap.put(e, new float[][]{
                {model.bipedHead.rotateAngleX, model.bipedHead.rotateAngleY, model.bipedHead.rotateAngleZ},
                {model.bipedRightArm.rotateAngleX, model.bipedRightArm.rotateAngleY, model.bipedRightArm.rotateAngleZ},
                {model.bipedLeftArm.rotateAngleX, model.bipedLeftArm.rotateAngleY, model.bipedLeftArm.rotateAngleZ},
                {model.bipedRightLeg.rotateAngleX, model.bipedRightLeg.rotateAngleY, model.bipedRightLeg.rotateAngleZ},
                {model.bipedLeftLeg.rotateAngleX, model.bipedLeftLeg.rotateAngleY, model.bipedLeftLeg.rotateAngleZ}
        });
    }

    public boolean canDrawModel(EntityPlayer entity) {
        return isValid(entity) && isToggled();
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

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        modelPlayerMap.clear();
        super.onDisable();
    }
}
