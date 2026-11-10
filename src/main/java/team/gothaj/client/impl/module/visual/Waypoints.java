package team.gothaj.client.impl.module.visual;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vector3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.event.input.MiddleClickEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import javax.vecmath.Vector4d;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.util.glu.GLU.GLU_FILL;
import static org.lwjgl.util.glu.GLU.GLU_INSIDE;
import static org.lwjgl.util.glu.GLU.GLU_OUTSIDE;

public class Waypoints extends Module {

    public Waypoints() {
        super("Waypoints", "", ModuleCategory.VISUAL);
    }

    @Listen
    public void onMiddleClick(MiddleClickEvent event) {
        if (!mc.isSingleplayer() && mc.getCurrentServerData() != null) {
            MovingObjectPosition ray = mc.thePlayer.rayTrace(250.0D, mc.timer.renderPartialTicks);
            if (ray != null && ray.getBlockPos() != null) {
                GothajClient.getInstance().getWaypointManager().add("Test", mc.getCurrentServerData().serverIP, ray.getBlockPos().getX() - 0.5f, ray.getBlockPos().getY() + 1f, ray.getBlockPos().getZ() - 0.5f, mc.theWorld.provider.getDimensionId(), GothajClient.getInstance().getUser().getUsername());
            }
        }
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        team.gothaj.client.api.util.render.RenderUtil.checkGLErr("Waypoints.enter");

        if (!mc.isSingleplayer() && mc.getCurrentServerData() != null) {
            GothajClient.getInstance().getWaypointManager().getItems().forEach(waypoint -> {
                if (mc.getCurrentServerData().serverIP.equals(waypoint.getServer()) && mc.thePlayer.dimension == waypoint.getDimension()) {
                    double posX = waypoint.getX();
                    double posY = waypoint.getY();
                    double posZ = waypoint.getZ();
                    AxisAlignedBB bb = new AxisAlignedBB(0.0625, 0.0, 0.0625, 0.94, 0.875, 0.94).offset(posX - mc.getRenderManager().renderPosX, posY - mc.getRenderManager().renderPosY, posZ - mc.getRenderManager().renderPosZ);
                    List<Vector3d> vectors = Arrays.asList(new Vector3d(posX + bb.minX - bb.maxX + 1 / 2.0f, posY, posZ + bb.minZ - bb.maxZ + 1 / 2.0f), new Vector3d(posX + bb.maxX - bb.minX - 1 / 2.0f, posY, posZ + bb.minZ - bb.maxZ + 1 / 2.0f), new Vector3d(posX + bb.minX - bb.maxX + 1 / 2.0f, posY, posZ + bb.maxZ - bb.minZ - 1 / 2.0f), new Vector3d(posX + bb.maxX - bb.minX - 1 / 2.0f, posY, posZ + bb.maxZ - bb.minZ - 1 / 2.0f), new Vector3d(posX + bb.minX - bb.maxX + 1 / 2.0f, posY + bb.maxY - bb.minY, posZ + bb.minZ - bb.maxZ + 1 / 2.0f), new Vector3d(posX + bb.maxX - bb.minX - 1 / 2.0f, posY + bb.maxY - bb.minY, posZ + bb.minZ - bb.maxZ + 1 / 2.0f), new Vector3d(posX + bb.minX - bb.maxX + 1 / 2.0f, posY + bb.maxY - bb.minY, posZ + bb.maxZ - bb.minZ - 1 / 2.0f), new Vector3d(posX + bb.maxX - bb.minX - 1 / 2.0f, posY + bb.maxY - bb.minY, posZ + bb.maxZ - bb.minZ - 1 / 2.0f));
                    mc.entityRenderer.setupCameraTransform(event.getPartialTicks(), 0);
                    Vector4d position = null;
                    for (Vector3d vector : vectors) {
                        vector = RenderUtil.project(vector.x - mc.getRenderManager().viewerPosX, vector.y - mc.getRenderManager().viewerPosY, vector.z - mc.getRenderManager().viewerPosZ);
                        if (vector != null && vector.z >= 0.0 && vector.z < 1.0) {
                            if (position == null) {
                                position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
                            }
                            position.x = Math.min(vector.x, position.x);
                            position.y = Math.min(vector.y, position.y);
                            position.z = Math.max(vector.x, position.z);
                            position.w = Math.max(vector.y, position.w);
                        }
                    }
                    mc.entityRenderer.setupOverlayRendering();
                    if (position != null) {
                        GL11.glPushMatrix();
                        try {
                            float x = (float) position.x - 70;
                            float y = (float) position.y + 13;
                            float z = (float) position.z;

                            String nameLine = "Name: " + waypoint.getLabel();
                            String distanceLine = "Distance: " + Math.round(mc.thePlayer.getDistance(waypoint.getX(), waypoint.getY(), waypoint.getZ())) + "m";
                            String creatorLine = "Creator: " + GothajClient.getInstance().getUser().getUsername();

                            int textHeight = Fonts.nunito14.getHeight();
                            int lineSpacing = 2;
                            int totalTextHeight = (textHeight * 3) + (lineSpacing * 2);

                            float maxWidth = Math.max(
                                    Math.max(Fonts.nunito14.getStringWidth(nameLine),
                                            Fonts.nunito14.getStringWidth(distanceLine)),
                                    Fonts.nunito14.getStringWidth(creatorLine)
                            );

                            float centerX = x + (z - x) / 2;
                            float textStartX = centerX - (maxWidth / 2.0f) - 1;
                            float textStartY = y - totalTextHeight - 7;

                            float rectX = centerX - (maxWidth / 2.0f) - 4;
                            float rectY = y - totalTextHeight - 10;
                            float rectWidth = maxWidth + 8;
                            float rectHeight = totalTextHeight + 5;

                            RoundedUtil.drawRound(
                                    rectX,
                                    rectY,
                                    rectWidth,
                                    rectHeight,
                                    4,
                                    new Color(0, 0, 0, 120)
                            );


                            Fonts.nunito14.drawStringWithShadow(nameLine, textStartX, textStartY, -1);
                            Fonts.nunito14.drawStringWithShadow(distanceLine, textStartX, textStartY + textHeight + lineSpacing, -1);
                            Fonts.nunito14.drawStringWithShadow(creatorLine, textStartX, textStartY + (textHeight + lineSpacing) * 2, -1);

                        } finally {
                            GL11.glPopMatrix();
                        }
                    }
                }

            });
        }
    }

    @Listen
    public void onDimensionRender(RenderDimensionEvent event) {
        if (!mc.isSingleplayer() && mc.getCurrentServerData() != null) {
            GothajClient.getInstance().getWaypointManager().getItems().forEach(waypoint -> {
                if (mc.getCurrentServerData().serverIP.equals(waypoint.getServer()) && mc.thePlayer.dimension == waypoint.getDimension()) {
                    double posX = waypoint.getX() - mc.getRenderManager().renderPosX;
                    double posY = waypoint.getY() - mc.getRenderManager().renderPosY;
                    double posZ = waypoint.getZ() - mc.getRenderManager().renderPosZ;

                    GL11.glPushMatrix();
                    try {
                        RenderUtil.pre3D();
                        GL11.glLineWidth(1.0f);
                        GL11.glTranslated(posX, (posY + 0.5f), posZ);
                        GL11.glScaled(2, 2, 2);

                        // Create cylinder once and reuse
                        final Cylinder c = new Cylinder();
                        Color c1 = new Color(Hud.getColor(5));
                        int color1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 191).getRGB();

                        // First cylinder part
                        GL11.glPushMatrix();
                        try {
                            RenderUtil.color4f(color1);
                            GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                            c.setDrawStyle(GLU_FILL);
                            c.setOrientation(GLU_OUTSIDE);
                            c.draw(0.0f, 0.175f, 0.3f, 4, 1);

                            GL11.glTranslated(0.0, 0.0, 0.3);
                            c.draw(0.175f, 0.0f, 0.3f, 4, 1);
                        } finally {
                            GL11.glPopMatrix();
                        }

                        // Second cylinder part (inside)
                        GL11.glPushMatrix();
                        try {
                            GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
                            RenderUtil.color4f(color1);
                            c.setDrawStyle(GLU_FILL);
                            c.setOrientation(GLU_INSIDE);
                            c.draw(0.0f, 0.175f, 0.3f, 4, 1);

                            GL11.glTranslated(0.0, 0.0, 0.3);
                            c.draw(0.175f, 0.0f, 0.3f, 4, 1);
                        } finally {
                            GL11.glPopMatrix();
                        }

                        RenderUtil.post3D();
                        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                    } finally {
                        GL11.glPopMatrix();
                    }
                }
            });
        }
    }

}
