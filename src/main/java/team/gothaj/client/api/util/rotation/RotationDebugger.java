package team.gothaj.client.api.util.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.module.combat.KillAura;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author vajzii + AI
 * @since 29.10.2025
 */
@SuppressWarnings("all")
public class RotationDebugger implements Utils {

    private static final ArrayList<RotationPoint> rotationHistory = new ArrayList<>();
    private static final long MAX_AGE_MS = 1000;

    public static class RotationPoint {
        public float yaw;
        public float pitch;
        public Vec3 lookVector;
        public Vec3 worldPosition;
        public Vec3 targetPosition;
        public long timestamp;

        public RotationPoint(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = System.currentTimeMillis();
            this.lookVector = mc.thePlayer.getVectorForRotation(pitch, yaw);

            Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
            this.worldPosition = eyes;

            double distance = EntityUtil.getDistanceToEntityBox(KillAura.target);
            this.targetPosition = new Vec3(eyes.xCoord + lookVector.xCoord * distance, eyes.yCoord + lookVector.yCoord * distance, eyes.zCoord + lookVector.zCoord * distance);
        }

        public float getAge() {
            return (System.currentTimeMillis() - timestamp) / 1000f;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > MAX_AGE_MS;
        }
    }

    public static void recordRotation(float yaw, float pitch) {
        rotationHistory.removeIf(RotationPoint::isExpired);
        rotationHistory.add(new RotationPoint(yaw, pitch));

        if (rotationHistory.size() > 200) {
            rotationHistory.removeFirst();
        }
    }

    public static void render3D() {
        if (rotationHistory.isEmpty() || mc.thePlayer == null) return;

        rotationHistory.removeIf(RotationPoint::isExpired);

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(2.0f);

        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (int i = 0; i < rotationHistory.size(); i++) {
            RotationPoint point = rotationHistory.get(i);
            float age = point.getAge();
            float alpha = Math.max(0, 1 - (age / 10f));

            Color color = Color.getHSBColor(age / 20f, 0.8f, 1.0f);
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                    color.getBlue() / 255f, alpha * 0.8f);

            double renderX = point.targetPosition.xCoord - mc.getRenderManager().renderPosX;
            double renderY = point.targetPosition.yCoord - mc.getRenderManager().renderPosY;
            double renderZ = point.targetPosition.zCoord - mc.getRenderManager().renderPosZ;

            GL11.glVertex3d(renderX, renderY, renderZ);
        }

        GL11.glEnd();

        GL11.glPointSize(4.0f);
        GL11.glBegin(GL11.GL_POINTS);

        for (RotationPoint point : rotationHistory) {
            float age = point.getAge();
            float alpha = Math.max(0, 1 - (age / 10f));

            Color color = Color.getHSBColor(age / 20f, 0.8f, 1.0f);
            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                    color.getBlue() / 255f, alpha);

            double renderX = point.targetPosition.xCoord - mc.getRenderManager().renderPosX;
            double renderY = point.targetPosition.yCoord - mc.getRenderManager().renderPosY;
            double renderZ = point.targetPosition.zCoord - mc.getRenderManager().renderPosZ;

            GL11.glVertex3d(renderX, renderY, renderZ);
        }

        GL11.glEnd();


        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }


    public static void clear() {
        rotationHistory.clear();
    }
}