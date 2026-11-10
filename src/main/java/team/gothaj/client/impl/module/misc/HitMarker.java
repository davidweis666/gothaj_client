package team.gothaj.client.impl.module.misc;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.eventbus.Listen;

import java.awt.*;

import static org.lwjgl.util.glu.GLU.*;

/**
 * @author vajzii
 * @since 2025
 */
@SuppressWarnings("all")
public class HitMarker extends Module {

    private KillAura killAura;
    private Hud hud;
    private double animationTime = 0;
    private long lastAnimTime = System.currentTimeMillis();

    public final BooleanProperty hitSound = new BooleanProperty("Hit Sound", true);
    private final ModeProperty hitSoundMode = new ModeProperty("Hit Sound Mode", "Skeet", "Skeet", "Neverlose", "Lightsaber").setVisible(hitSound::getProperty);

    public final NumberProperty soundVolume = new NumberProperty("Hit Sound Volume", 0.82f, 0.0f, 1.0f, 0.01f).setVisible(hitSound::getProperty);

    private final BooleanProperty renderMarker = new BooleanProperty("Render Marker", true);
    private final ModeProperty mode = new ModeProperty("Mode", "Spiral", "Spiral", "Crystal").setVisible(renderMarker::getProperty);
    private final NumberProperty spiralCount = new NumberProperty("Spiral Count", 3, 1, 20, 1).setVisible(() -> mode.is("Spiral"));

    public final BooleanProperty visualizeRange = new BooleanProperty("Visualize Range", true);

    private final NumberProperty vrPoints = new NumberProperty("Roundness", 50, 6, 50, 1).setVisible(visualizeRange::getProperty);

    public final BooleanProperty vrOutline = new BooleanProperty("Outline", true).setVisible(visualizeRange::getProperty);



    public HitMarker() {
        super("HitMarker", "Renders animated ring around target", ModuleCategory.MISC);
        registerProperties(hitSound, hitSoundMode, soundVolume, renderMarker, mode, spiralCount, visualizeRange, vrPoints, vrOutline);
    }

    public boolean canPlaySound() {
        return isToggled() && ((mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) || KillAura.target != null) && hitSound.getProperty() && mc.thePlayer.isSwingInProgress;
    }

    public ResourceLocation getSoundLocation(int lightCount) {
        switch (hitSoundMode.getProperty()) {
            case "Skeet":
                return new ResourceLocation("gothaj", "skeet");

            case "Neverlose":
                return new ResourceLocation("gothaj", "neverlose");

            case "Lightsaber":
                return new ResourceLocation("gothaj", "lightsaber.saber" + lightCount);
        }
        return null;
    }

    @Listen
    public void onRender(RenderDimensionEvent event) {
        if (killAura != null && killAura.isToggled()) {

            if (visualizeRange.getProperty()) {
                mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 0);
                drawRangeVisualizer();
            }

            if (!renderMarker.getProperty() || killAura.target == null) return;

            EntityLivingBase target = killAura.target;

            GlStateManager.pushMatrix();
            mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 0);
            if (mode.is("Spiral")) {
                renderAnimatedRing(target);
            } else {
                drawCrystalAbove(target, event.getPartialTicks());
            }
            GlStateManager.popMatrix();
        }
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!renderMarker.getProperty()) return;

        RenderUtil.checkGLErr("HitMarker.enter");
        if (killAura != null && killAura.isToggled() && killAura.target != null && killAura.target.hurtTime > 0) {
            drawCrosshairLines((int) event.getScaledResolution().getScaledWidth(), (int) event.getScaledResolution().getScaledHeight());
        }
        RenderUtil.checkGLErr("HitMarker.exit");
    }
    
    public void drawCrystalAbove(EntityLivingBase target, float partialTicks) {
        float xPos = (float) (RenderUtil.interpolate(target.lastTickPosX, target.posX, partialTicks) - mc.getRenderManager().renderPosX);
        float yPos = (float) (RenderUtil.interpolate(target.lastTickPosY, target.posY, partialTicks) - mc.getRenderManager().renderPosY);
        float zPos = (float) (RenderUtil.interpolate(target.lastTickPosZ, target.posZ, partialTicks) - mc.getRenderManager().renderPosZ);
        GL11.glPushMatrix();
        RenderUtil.pre3D();
        GL11.glLineWidth(1.0f);
        GL11.glTranslated(xPos, (yPos + 2.2f), zPos);
        GL11.glScaled(1.5, 1.5, 1.5);
        final Cylinder c = new Cylinder();
        Color c1 = new Color(ColorUtil.getTeamColor(target));
        int color1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 191).getRGB();
        RenderUtil.color(color1);
        GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
        c.draw(0.0f, 0.175f, 0.3f, 4, 1);
        GL11.glTranslated(0.0, 0.0, 0.3);
        c.draw(0.175f, 0.0f, 0.3f, 4, 1);
        GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
        GL11.glTranslated(0.0, 0.0, -0.3);
        GL11.glColor4f(0.15f, 0.25f, 0.5f, 1.0f);
        c.setDrawStyle(GLU_LINE);
        c.setDrawStyle(GLU_SMOOTH);
        c.setDrawStyle(GLU_FILL);
        c.setOrientation(GLU_INSIDE);
        c.draw(0.0f, 0.175f, 0.3f, 4, 1);
        GL11.glTranslated(0.0, 0.0, 0.3);
        c.draw(0.175f, 0.0f, 0.3f, 4, 1);
        RenderUtil.post3D();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    public void drawCrosshairLines(int screenWidth, int screenHeight) {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        int gap = 2;
        int length = 4;
        float thickness = 2.5f;
        Color color = new Color(210, 210, 210, 150);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableTexture2D();

        float diagGap = gap;
        float diagLength = length;

        drawDiagonalLine(centerX - diagGap, centerY - diagGap, -diagLength, -diagLength, thickness, color);

        drawDiagonalLine(centerX + diagGap, centerY - diagGap, diagLength, -diagLength, thickness, color);

        drawDiagonalLine(centerX - diagGap, centerY + diagGap, -diagLength, diagLength, thickness, color);

        drawDiagonalLine(centerX + diagGap, centerY + diagGap, diagLength, diagLength, thickness, color);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawDiagonalLine(float x, float y, float width, float height, float thickness, Color color) {
        GL11.glLineWidth(thickness);

        RenderUtil.color(color);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glEnd();
    }

    public void renderAnimatedRing(Entity target) {
        if (target == null || !target.isEntityAlive()) return;

        float height = target.height;
        int spiralTurns = spiralCount.getProperty().intValue();
        int points = 60;

        animationTime += 0.02;

        float alpha = 1.0f;

        int colorStart = Hud.getColor(5);
        int colorEnd = Hud.getColor(7);

        float startA = (float)((colorStart >> 24) & 0xFF) / 255f * alpha;
        float startR = (float)((colorStart >> 16) & 0xFF) / 255f;
        float startG = (float)((colorStart >> 8) & 0xFF) / 255f;
        float startB = (float)(colorStart & 0xFF) / 255f;

        float endA = (float)((colorEnd >> 24) & 0xFF) / 255f * alpha;
        float endR = (float)((colorEnd >> 16) & 0xFF) / 255f;
        float endG = (float)((colorEnd >> 8) & 0xFF) / 255f;
        float endB = (float)(colorEnd & 0xFF) / 255f;

        GL11.glPushMatrix();
        GL11.glTranslated(
                target.lastTickPosX + (target.posX - target.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX,
                target.lastTickPosY + (target.posY - target.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY,
                target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ
        );

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);

        GL11.glLineWidth(6.5F);
        GL11.glColor4f(0f, 0f, 0f, alpha);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < points; i++) {
            float t = (float)i / points;
            double angle = t * Math.PI * 2 * spiralTurns + animationTime;
            double y = height * t;
            double radius = 0.5F;

            GL11.glVertex3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glLineWidth(5);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < points; i++) {
            float t = (float)i / points;

            float a = (startA + t * (endA - startA));
            float r = startR + t * (endR - startR);
            float g = startG + t * (endG - startG);
            float b = startB + t * (endB - startB);

            GL11.glColor4f(r, g, b, a);

            double angle = t * Math.PI * 2 * spiralTurns + animationTime;
            double y = height * t;
            double radius = 0.5F;

            GL11.glVertex3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GL11.glPopMatrix();
    }
    
    public void drawRangeVisualizer() {
        GL11.glPushMatrix();
        //GL11.glDepthMask(true);
        GL11.glTranslated(
                mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX,
                mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY,
                mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glRotatef(90, 1, 0, 0);

        double step = 360.0 / vrPoints.getProperty();

        if (vrOutline.getProperty()) {

            GL11.glLineWidth(4f);
            GL11.glBegin(GL11.GL_LINE_LOOP);

            for (int index = 0; index < vrPoints.getProperty(); index++) {
                double radian = Math.toRadians(index * step);

                RenderUtil.color(Color.BLACK);

                GL11.glVertex2d(Math.cos(radian) * 3.0, Math.sin(radian) * 3.0);
            }
            GL11.glEnd();
        }

        GL11.glLineWidth(2);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int index = 0; index < vrPoints.getProperty(); index++) {
            double radian = Math.toRadians(index * step);

            int colorIndex = index;
            if (colorIndex > 255) colorIndex = 0;

            RenderUtil.color(hud.getColor(colorIndex));

            GL11.glVertex2d(Math.cos(radian) * 3.0, Math.sin(radian) * 3.0);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glPopMatrix();
        GlStateManager.resetColor();
    }




    @Override
    public void onEnable() {
        killAura = getCastedModule(KillAura.class);
        hud = getCastedModule(Hud.class);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}