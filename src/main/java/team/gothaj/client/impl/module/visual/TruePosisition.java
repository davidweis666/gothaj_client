package team.gothaj.client.impl.module.visual;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.scoreboard.Team;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.client.Targets;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class TruePosisition extends Module {

    // Stores the raw server-authoritative position per player (entity ID → xyz)
    private final Map<Integer, double[]> truePositions = new HashMap<>();

    public final ModeProperty colorMode = new ModeProperty("Color Mode", "Hud", "Hud", "Custom", "Team");
    public final ColorProperty customColor = new ColorProperty("Custom Color", Color.WHITE).setVisible(() -> colorMode.is("Custom"));
    public final NumberProperty lineWidth = new NumberProperty("Width", 1.0f, 0.1f, 3.0f, 0.1f);

    private static final float BOX_W = 0.3F;
    private static final float BOX_H = 1.8F;

    public TruePosisition() {
        super("TruePosition", "", ModuleCategory.VISUAL);
        registerProperties(colorMode, customColor, lineWidth);
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        truePositions.clear();
    }

    /**
     * Hook incoming position packets to record the raw server-side position
     * before the client interpolation pipeline moves the entity smoothly.
     *
     * S14PacketEntity      → relative move (delta encoded as fixed-point /32)
     * S18PacketEntityTeleport → absolute teleport
     */
    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE) return;
        if (mc.theWorld == null) return;

        if (event.getPacket() instanceof S14PacketEntity pkt) {
            net.minecraft.entity.Entity ent = pkt.getEntity(mc.theWorld);
            if (!(ent instanceof EntityPlayer)) return;

            // S14 carries delta position in 1/32-block fixed point
            double[] cur = truePositions.getOrDefault(ent.getEntityId(),
                    new double[]{ent.posX, ent.posY, ent.posZ});

            // Only update axes that actually moved (posRotating flag)
            double nx = cur[0] + pkt.func_149062_c() / 32.0;
            double ny = cur[1] + pkt.func_149061_d() / 32.0;
            double nz = cur[2] + pkt.func_149064_e() / 32.0;

            truePositions.put(ent.getEntityId(), new double[]{nx, ny, nz});

        } else if (event.getPacket() instanceof S18PacketEntityTeleport pkt) {
            net.minecraft.entity.Entity ent = mc.theWorld.getEntityByID(pkt.getEntityId());
            if (!(ent instanceof EntityPlayer)) return;

            // S18 carries absolute position in 1/32-block fixed point
            double nx = pkt.getX() / 32.0;
            double ny = pkt.getY() / 32.0;
            double nz = pkt.getZ() / 32.0;

            truePositions.put(pkt.getEntityId(), new double[]{nx, ny, nz});
        }
    }

    @Listen
    public void onRenderDimensions(RenderDimensionEvent event) {
        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isValid(player)) continue;

            double[] pos = truePositions.get(player.getEntityId());
            if (pos == null) continue; // no packet received yet for this player

            int color = colorMode.is("Team") ? ColorUtil.getTeamColor(player)
                    : colorMode.is("Hud") ? Hud.primaryColor.getProperty().getRGB()
                      : customColor.getProperty().getRGB();

            glLineWidth(lineWidth.getProperty());
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_LINE_SMOOTH);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_TEXTURE_2D);
            glDepthMask(false);
            RenderUtil.color(color);

            float h = player.isSneaking() ? 1.5F : BOX_H;

            glPushMatrix();
            glTranslated(
                    pos[0] - RenderManager.renderPosX,
                    pos[1] - RenderManager.renderPosY,
                    pos[2] - RenderManager.renderPosZ
            );
            drawBox(-BOX_W, 0, -BOX_W, BOX_W, h, BOX_W);
            glPopMatrix();

            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            glDisable(GL_LINE_SMOOTH);
            glEnable(GL_DEPTH_TEST);
        }
    }

    private void drawBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        // Bottom face
        glBegin(GL_LINE_STRIP);
        glVertex3f(x1, y1, z1); glVertex3f(x2, y1, z1);
        glVertex3f(x2, y1, z2); glVertex3f(x1, y1, z2);
        glVertex3f(x1, y1, z1);
        glEnd();
        // Top face
        glBegin(GL_LINE_STRIP);
        glVertex3f(x1, y2, z1); glVertex3f(x2, y2, z1);
        glVertex3f(x2, y2, z2); glVertex3f(x1, y2, z2);
        glVertex3f(x1, y2, z1);
        glEnd();
        // Vertical edges
        glBegin(GL_LINES);
        glVertex3f(x1, y1, z1); glVertex3f(x1, y2, z1);
        glVertex3f(x2, y1, z1); glVertex3f(x2, y2, z1);
        glVertex3f(x2, y1, z2); glVertex3f(x2, y2, z2);
        glVertex3f(x1, y1, z2); glVertex3f(x1, y2, z2);
        glEnd();
    }

    public static boolean isValid(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer || entity.deathTime > 0
                || !entity.isEntityAlive() || entity.getHealth() == 0) return false;
        if (entity instanceof EntityArmorStand) return false;

        String name = entity.getDisplayName().getUnformattedText();
        if (name.contains("Shop") || name.contains("Keeper")) return false;

        if (entity instanceof EntityPlayer pl) {
            if (!Targets.visualTargets.getOptionState("Players")) return false;
            if (EntityUtil.isBot(pl) && !Targets.visualTargets.getOptionState("Bots")) return false;
        }
        if (entity instanceof IMob && !Targets.visualTargets.getOptionState("Monsters")) return false;

        Team entityTeam = entity.getTeam();
        Team playerTeam = mc.thePlayer.getTeam();
        boolean isTeammate = EntityUtil.isTeammate(entity)
                || (entityTeam != null && playerTeam != null && entityTeam.isSameTeam(playerTeam));
        if (isTeammate && !Targets.visualTargets.getOptionState("Teammates")) return false;

        if ((entity instanceof IAnimals || entity instanceof EntityAgeable)
                && !Targets.visualTargets.getOptionState("Animals")) return false;

        return !entity.isInvisible() || Targets.visualTargets.getOptionState("Invisibles");
    }

    @Override
    public void onDisable() {
        truePositions.clear();
        super.onDisable();
    }
}