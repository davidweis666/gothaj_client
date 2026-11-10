package team.gothaj.client.impl.module.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.util.IRCUtil;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.player.SneakSpoofEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author vajzii
 * @since 2025
 */
public class ChinaHat extends Module {

    public final BooleanProperty matchHud = new BooleanProperty("Match HUD", false).setVisible(() -> GothajClient.getInstance().getCurrentPlayer() != null && GothajClient.getInstance().getCurrentPlayer().getRank().getPermissionLevel() != 0);

    private final double[][] position = new double[180 + 1][2];
    private final int[] segmentColours = new int[180 + 1];

    public ChinaHat() {
        super("ChinaHat", "Renders cool hat on client users", ModuleCategory.VISUAL);
        registerProperties(matchHud);
    }

    @Listen
    public void onRender(RenderDimensionEvent event) {
        final List<EntityPlayer> chinaHatOwners = new ArrayList<>();

        mc.theWorld.playerEntities.stream().filter(entity -> entity.isEntityAlive() && IrcPlayer.getByIngameName(entity.getName()) != null || entity instanceof EntityPlayerSP).forEach(chinaHatOwners::add);

        final double rx = mc.getRenderManager().renderPosX;
        final double ry = mc.getRenderManager().renderPosY;
        final double rz = mc.getRenderManager().renderPosZ;

        glTranslated(-rx, -ry, -rz);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_CULL_FACE);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glShadeModel(GL_SMOOTH);
        glLineWidth(2.0f);
        glEnable(GL_BLEND);


        chinaHatOwners.forEach(player -> {
            if (mc.gameSettings.thirdPersonView == 0 && player instanceof EntityPlayerSP) return;

            final float partialTicks = event.getPartialTicks();

            final double x = RenderUtil.interpolate(player.lastTickPosX, player.posX, partialTicks);
            final double y = RenderUtil.interpolate(player.lastTickPosY, player.posY, partialTicks);
            final double z = RenderUtil.interpolate(player.lastTickPosZ, player.posZ, partialTicks);

            final int points = 60;
            final double radius = 0.6;


            final long time = System.currentTimeMillis() / 25;

            for (int i = 0; i < this.segmentColours.length; i++) {

                final Color baseColor = matchHud.getProperty() && player instanceof EntityPlayerSP ? Hud.primaryColor.getProperty() :  IRCUtil.getIrcColor(player.getGameProfile());
                final Color secondColor = matchHud.getProperty() && player instanceof EntityPlayerSP ? Hud.secondaryColor.getProperty() : IRCUtil.getIrcColor(player.getGameProfile()).darker();

                final int cycleLength = points / 2;

                float cyclePos = ((float) ((time + i) % cycleLength) / cycleLength);

                this.segmentColours[i] = ColorUtil.fadeBetween(
                        baseColor.getRGB(),
                        secondColor.getRGB(),
                        (float) Math.abs(Math.sin(cyclePos * Math.PI)));
            }

            float lastYaw = player instanceof EntityPlayerSP ? RotationComponent.lastRotations[0] : player.prevRotationYaw;
            float yaw = player instanceof EntityPlayerSP ? RotationComponent.rotations[0] : player.rotationYaw;

            float lastPitch = player instanceof EntityPlayerSP ? RotationComponent.lastRotations[1] : player.prevRotationPitch;
            float pitch = player instanceof EntityPlayerSP ? RotationComponent.rotations[1] : player.rotationPitch;


            GL11.glPushMatrix();
            final var eventSpoof = new SneakSpoofEvent();
            if (player == Minecraft.getMinecraft().thePlayer) GothajClient.getInstance().getEventBus().fire(eventSpoof);
            glTranslated(x, y + (player.isSneaking() && !eventSpoof.isCancelled() ? 1.5 :2.0 - radius / 3), z);

            glRotated(RenderUtil.interpolate(lastYaw, yaw, event.getPartialTicks()), 0, -1, 0);

            final double pitchInt = RenderUtil.interpolate(lastPitch, pitch, event.getPartialTicks());
            glRotated(pitchInt / 3.d, 1, 0, 0);
            glTranslated(0, 0, pitchInt / 270);

            glBegin(GL_LINE_LOOP);

            for (int i = 0; i < points; i++) {
                final double[] pos = this.position[i];
                RenderUtil.color(this.segmentColours[i] + (0x96 << 24));
                glVertex3d(pos[0], 0, pos[1]);
            }

            RenderUtil.resetColor();

            glEnd();

            glBegin(GL_TRIANGLE_FAN);

            glVertex3d(0, radius / 2, 0);

            for (int i = 0; i <= points; i++) {
                final double[] pos = this.position[i];
                RenderUtil.color(this.segmentColours[i] + (0x96 << 24));
                glVertex3d(pos[0], 0, pos[1]);
            }

            glEnd();
            GL11.glPopMatrix();
        });
            glDisable(GL_BLEND);
            glDepthMask(true);
            glShadeModel(GL_FLAT);
            glDisable(GL_LINE_SMOOTH);
            glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glEnable(GL_TEXTURE_2D);

            glTranslated(rx, ry, rz);
    }

    private void computeChineseHatPoints(final int points, final double radius) {
        for (int i = 0; i <= points; i++) {
            final double circleX = radius * StrictMath.cos(i * Math.PI * 2 / points);
            final double circleZ = radius * StrictMath.sin(i * Math.PI * 2 / points);

            this.position[i][0] = circleX;
            this.position[i][1] = circleZ;
        }
    }

    @Override
    public void onEnable() {
        this.computeChineseHatPoints(60,0.6);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
