package team.gothaj.client.api.util.render;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.glu.GLU;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.impl.module.visual.Hud;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Supplier;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

public class RenderUtil implements Utils {

    public static long initTime;

    private static final Frustum FRUSTUM = new Frustum();

    private final static IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);
    private final static FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);
    private final static FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);

    public static boolean canRenderBB(AxisAlignedBB aabb) {
        FRUSTUM.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        return FRUSTUM.isBoundingBoxInFrustum(aabb);
    }

    public static ResourceLocation uploadBufferedImageAsTexture(BufferedImage img, String name) {
        DynamicTexture dynTex = new DynamicTexture(img);
        ResourceLocation resLoc = new ResourceLocation("gothaj_gif", name);
        mc.getTextureManager().loadTexture(resLoc, dynTex);
        return resLoc;
    }

    public static void color(int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, alpha);
    }

    public static void color(int red, int green, int blue, int alpha) {
        GL11.glColor4f(red / 255f, green / 255f, blue / 255f, alpha / 255f);
    }

    public static void color(final Color color) {
        final float red = color.getRed() / 255F;
        final float green = color.getGreen() / 255F;
        final float blue = color.getBlue() / 255F;
        final float alpha = color.getAlpha() / 255F;

        GlStateManager.color(red, green, blue, alpha);
    }

    public static void color4f(final Color color) {
        GL11.glColor4f(
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f
        );
    }

    public static float interpolate(float start, float end, float fraction) {
        return start + fraction * (end - start);
    }

    public static double interpolate(double start, double end, double fraction) {

        return start + fraction * (end - start);
    }


    public static void color4f(final int colorVal) {
        Color color = new Color(colorVal, true);
        GL11.glColor4f(
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f
        );
    }

    public static AxisAlignedBB interpolateRenderBB(final Entity entity) {
        final double minX = entity.lastTickPosX - entity.width / 2. + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX,
                minY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY,
                minZ = entity.lastTickPosZ - entity.width / 2. + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ,
                maxX = entity.lastTickPosX + entity.width / 2. + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosX,
                maxY = entity.lastTickPosY + entity.height + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosY,
                maxZ = entity.lastTickPosZ + entity.width / 2. + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.getRenderManager().renderPosZ;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void convertBox(double[] coordsOut, Entity entity) {
        final AxisAlignedBB interpolatedBB = interpolateRenderBB(entity);
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        coordsOut[0] = Double.MAX_VALUE;
        coordsOut[1] = Double.MAX_VALUE;
        coordsOut[2] = Double.MIN_VALUE;
        coordsOut[3] = Double.MIN_VALUE;
        for (double x = interpolatedBB.minX; x <= interpolatedBB.minX + entity.width; x += entity.width) {
            for (double z = interpolatedBB.minZ; z <= interpolatedBB.minZ + entity.width; z += entity.width) {
                for (double y = interpolatedBB.minY; y <= interpolatedBB.minY + entity.height; y += entity.height) {
                    coordsOut[0] = Math.min(coordsOut[0], convertCoord(x, y, z, scaledResolution)[0]);
                    coordsOut[1] = Math.min(coordsOut[1], scaledResolution.getScaledHeight_double() - convertCoord(x, y, z, scaledResolution)[1]);
                    coordsOut[2] = Math.max(coordsOut[2], convertCoord(x, y, z, scaledResolution)[0]);
                    coordsOut[3] = Math.max(coordsOut[3], scaledResolution.getScaledHeight_double() - convertCoord(x, y, z, scaledResolution)[1]);
                }
            }
        }
    }

    private static double[] convertCoord(double x, double y, double z, final ScaledResolution scaledResolution) {
        final boolean success = GLU.gluProject((float) x, (float) y, (float) z,
                ActiveRenderInfo.MODELVIEW, ActiveRenderInfo.PROJECTION, ActiveRenderInfo.VIEWPORT, ActiveRenderInfo.OBJECTCOORDS);
        if (success)
            return new double[]{ActiveRenderInfo.OBJECTCOORDS.get(0) / scaledResolution.getScaleFactor(),
                    ActiveRenderInfo.OBJECTCOORDS.get(1) / scaledResolution.getScaleFactor()};
        return new double[0];
    }

    public static Vector3d project(double x, double y, double z) {
        FloatBuffer vector = GLAllocation.createDirectFloatBuffer(4);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetIntegerv(GL_VIEWPORT, viewport);
        if (GLU.gluProject((float) x, (float) y, (float) z, modelview, projection, viewport, vector)) {
            return new Vector3d(vector.get(0) / sr.getScaleFactor(),
                    (Display.getHeight() - vector.get(1)) / sr.getScaleFactor(),
                    vector.get(2));
        }
        return null;
    }

    public static void drawGradientRect(double x, double y, double width, double height, int firstColor, int secondColor) {
        Gui.drawGradientRect((int) x, (int) y, (int) (x + width), (int) (y + height), firstColor, secondColor);
    }

    public static void drawGradientRectHorizontal(double x, double y, double width, double height, int firstColor, int secondColor) {
        Gui.drawHorizontalGradientRect(x, y,  x + width, y + height, firstColor, secondColor);
    }

    public static void color(final int hex) {
        final float alpha = (hex >> 24 & 0xFF) / 255F;
        final float red = (hex >> 16 & 0xFF) / 255F;
        final float green = (hex >> 8 & 0xFF) / 255F;
        final float blue = (hex & 0xFF) / 255F;

        GlStateManager.color(red, green, blue, alpha);
    }

    public static void resetColor() {
        GL11.glColor4f(1,1,1,1);
    }

    // TEMP DEBUG: OpenGL error probe for pinpointing GL_INVALID_VALUE sources during overlays
    public static void checkGLErr(String tag) {
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            //System.out.println("[GL_ERR] " + tag + " -> " + err);
        }
    }


    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
        }
        return framebuffer;
    }

    public static void drawEntityBox(Entity entity, int color) {
        final RenderManager renderManager = mc.getRenderManager();
        final Timer timer = mc.timer;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glDepthMask(false);

        final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX;
        final double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY;
        final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ;

        final AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        final AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                entityBox.minX - entity.posX + x - 0.05D,
                entityBox.minY - entity.posY + y,
                entityBox.minZ - entity.posZ + z - 0.05D,
                entityBox.maxX - entity.posX + x + 0.05D,
                entityBox.maxY - entity.posY + y + 0.15D,
                entityBox.maxZ - entity.posZ + z + 0.05D
        ).expand(0.1, 0.1, 0.1);
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        float alpha = (color >> 24 & 255) / 255F;

        GlStateManager.color(red, green, blue, alpha);
        drawAxis(axisAlignedBB);
        resetColor();
        GL11.glDepthMask(true);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public static void renderBlock(BlockPos blockPos, int color, boolean outline, boolean shade) {
        renderBox(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1, 1, 1, color, outline, shade);
    }

    public static void renderBlock(BlockPos blockPos, int color, boolean outline, boolean shade, float progress) {
        renderBox(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1, 1, 1, color, outline, shade, progress);
    }

    public static void renderBox(int x, int y, int z, double x2, double y2, double z2, int color, boolean outline, boolean shade) {
        double xPos = x - mc.getRenderManager().viewerPosX;
        double yPos = y - mc.getRenderManager().viewerPosY;
        double zPos = z - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(xPos, yPos, zPos, xPos + x2, yPos + y2, zPos + z2);
        drawAxisAlignedBB(axisAlignedBB, shade, outline,color);
    }

    public static void renderBox(int x, int y, int z, double x2, double y2, double z2, int color, boolean outline, boolean shade, float progress) {
        double xPos = x - mc.getRenderManager().viewerPosX;
        double yPos = y - mc.getRenderManager().viewerPosY;
        double zPos = z - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(xPos, yPos, zPos, xPos + x2, yPos + y2, zPos + z2);
        drawSelectionBoundingBox(axisAlignedBB, shade, outline,color, progress);
    }

    public static void drawAxisAlignedBB(AxisAlignedBB axisAlignedBB, boolean outline, int color) {
        drawAxisAlignedBB(axisAlignedBB,outline,false,color);
    }

    public static void drawAxisAlignedBB(AxisAlignedBB axisAlignedBB,boolean filled, boolean outline, int color) {
        drawSelectionBoundingBox(axisAlignedBB, outline, filled,color);
    }

    public static void drawOutlineBoundingBox(final AxisAlignedBB bb,Color color) {
        RenderGlobal.drawOutlinedBoundingBox(bb,color.getRed(),color.getGreen(),color.getBlue(),color.getAlpha());
    }

    public static void drawAxis(AxisAlignedBB axisAlignedBB){
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();

        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        tessellator.draw();
    }

    public static void drawFilledBoundingBox(final AxisAlignedBB bb,Color color) {

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        tessellator.draw();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        tessellator.draw();
    }

    public static void drawSelectionBoundingBox(final AxisAlignedBB bb, final boolean outline, final boolean filled,int color) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GL11.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GL11.glDisable(2929);
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();

        if (outline) {

            glEnable(GL_LINE_SMOOTH);

            drawOutlineBoundingBox(bb,new Color(color,true));

            glDisable(GL_LINE_SMOOTH);
        }
        if (filled) {
            drawFilledBoundingBox(bb,new Color(color,true));
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GL11.glEnable(2929);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawSelectionBoundingBox(final AxisAlignedBB bb, final boolean outline, final boolean filled, int color, float progress) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GL11.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GL11.glDisable(2929);
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();

        if (outline) {
            glEnable(GL_LINE_SMOOTH);
            drawOutlineBoundingBox(bb, new Color(color, true));
            glDisable(GL_LINE_SMOOTH);
        }

        if (filled) {
            progress = Math.max(0.0f, Math.min(1.0f, progress));

            double progressHeight = bb.minY + ((bb.maxY - bb.minY) * progress);

            AxisAlignedBB progressBB = new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, progressHeight, bb.maxZ);

            drawFilledBoundingBox(progressBB, new Color(color, true));
        }

        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GL11.glEnable(2929);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }


    public static void bindTexture(int texture) {
        GlStateManager.bindTexture(texture);
    }

    public static void drawImage(ResourceLocation image, float x, float y, float width, float height, boolean shader) {
       drawImage(image, x, y, width, height, 255, shader);
    }

    public static void drawImage(ResourceLocation image, float x, float y, float width, float height, float alpha, boolean shader) {

        pushMatrix();
        disableDepth();
        depthMask(false);
        enableBlend();
        disableLighting();
        enableTexture2D();

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        if (shader) {
            color(Color.BLACK.getRGB(), alpha / 255.0f);
        } else {
            color(Color.WHITE.getRGB(), alpha / 255.0f);
        }
        mc.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture((int) x, (int) y, 0f, 0f, (int) width, (int) height, width, height);

        depthMask(true);
        enableDepth();
        disableBlend();
        popMatrix();
    }

    public static void drawHitMaker(double x,double y) {
        GL11.glPushMatrix();
        glTranslated( x,  y,  0);
        GL11.glRotatef(45, 0, 0, 1.0f);
        glTranslated(-x,-y,  0);
        drawBorderedRectOther(x - 1, y + 8, x + 7, y + 12, 1, Color.black.getRGB(), Color.white.getRGB());
        drawBorderedRectOther(x + 11, y + 8, x + 19, y + 12, 1, Color.black.getRGB(), Color.white.getRGB());
        drawBorderedRectOther(x + 7, y, x + 11, y + 8, 1, Color.black.getRGB(), Color.white.getRGB());
        drawBorderedRectOther(x + 7, y + 12, x + 11, y + 20, 1, Color.black.getRGB(), Color.white.getRGB());
        GL11.glPopMatrix();
    }


    public static void rect(final float x, final float y, final float x2, final float y2, final int color) {
        Gui.drawRect(x,y,x2,y2, color);
    }

    public static void rect(final float x, final float y, final float x2, final float y2, final Color color) {
        Gui.drawRect(x,y,x2,y2,color.getRGB());
    }

    public static void rect(final double x, final double y, final double x2, final double y2, final Color color) {
        Gui.drawRect(x,y,x2,y2,color.getRGB());
    }

    public static void rect(final double x, final double y, final double x2, final double y2, final int color) {
        Gui.drawRect(x,y,x2,y2,color);
    }

    public static void drawRect(final float x, final float y, final float width, final float height, final int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    public static void drawRect(final float x, final float y, final float width, final float height, final Color color) {
        Gui.drawRect(x, y, x + width, y + height, color.getRGB());
    }

    public static void drawRect(final double x, final double y, final double width, final double height, final Color color) {
        Gui.drawRect(x, y, x + width, y + height, color.getRGB());
    }

    public static void drawRect(final double x, final double y, final double width, final double height, final int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }


    public static void scissor(double x, double y, double width, double height, Runnable data) {
        glEnable(GL_SCISSOR_TEST);
        scissor(x, y, width, height);
        data.run();
        glDisable(GL_SCISSOR_TEST);
    }

    public static void prepareScissorBox(float x1, float y1, float x2, float y2) {
        ScaledResolution sr = new ScaledResolution(mc);
        final int scale = sr.getScaleFactor();
        GL11.glScissor(
                (int) (x1 * scale),
                (int) ((sr.getScaledHeight_double() - y2) * scale),
                (int) ((x2 - x1) * scale),
                (int) ((y2 - y1) * scale));
    }

    public static void startScissorBox(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor(
                x * sf,
                (sr.getScaledHeight() - (y + height)) * sf,
                width * sf,
                height * sf);
    }

    public static void drawBorderedRect(double x, double y, double width, double height, double lineSize, int borderColor, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
        Gui.drawRect(x, y, x + width, y + lineSize, borderColor);
        Gui.drawRect(x, y, x + lineSize, y + height, borderColor);
        Gui.drawRect(x + width, y, x + width - lineSize, y + height, borderColor);
        Gui.drawRect(x, y + height, x + width, y + height - lineSize, borderColor);
    }

    public static void drawBorderedRectOther(final double x, final double y, final double width, final double height, final float lineSize, final int borderColor, final int color) {
        RenderUtil.drawBorderedRect(x, y, width - x, height - y, lineSize, borderColor, color);
    }

    public static void glRect(float x, float y, float x2, float y2, float width, int color) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        color(color);
        // Preserve previous line width as float to avoid restoring to 0 and causing GL_INVALID_VALUE
        FloatBuffer fb = BufferUtils.createFloatBuffer(1);
        GL11.glGetFloatv(GL11.GL_LINE_WIDTH, fb);
        float prevWidth = fb.get(0);

        // Ensure we never set a non-positive line width
        GL11.glLineWidth(Math.max(0.1f, width));

        GL11.glBegin(GL_LINE_LOOP);

        glVertex2d(x2, y);
        glVertex2d(x, y);
        glVertex2d(x, y2);
        glVertex2d(x2, y2);

        GL11.glEnd();
        GL11.glLineWidth(prevWidth > 0f ? prevWidth : 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        resetColor();
    }



    public static void scissor(double x, double y, double width, double height) {
        final double scale = sr.getScaleFactor();
        double finalHeight = height * scale;
        double finalY = (sr.getScaledHeight() - y) * scale;
        double finalX = x * scale;
        double finalWidth = width * scale;
        glScissor((int) finalX, (int) (finalY - finalHeight), (int) finalWidth, (int) finalHeight);
    }

    public static void drawTexturedQuad(int texture, int x, int y, int width, int height) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(texture);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        GlStateManager.bindTexture(0);
        GlStateManager.disableBlend();
    }

    public static void pre3D() {
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        glBlendFunc(770, 771);
        GL11.glShadeModel(7425);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glDisable(2929);
        GL11.glDisable(2896);
        GL11.glDepthMask(false);
        GL11.glHint(3154, 4354);
    }

    public static void post3D() {
        GL11.glDepthMask(true);
        GL11.glEnable(2929);
        GL11.glDisable(2848);
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void start2D(int beginMode) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glBegin(beginMode);
    }

    public static void end2D() {
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    public static float animate(float target, float current, float speed) {
        speed = MathHelper.clamp(speed, 0, 1);
        float dif = Math.max(target, current) - Math.min(target, current);
        if (dif < 0.1f) return target;
        float factor = dif * speed;

        if (factor < 0.1f) factor = 0.1f;

        if (target > current) {
            current += factor;
        } else {
            current -= factor;
        }
        return current;
    }

    public static void drawLoadingCircle(float x, float y) {
        for (int i = 0; i < 3; ++i) {
            int rot = (int)(System.nanoTime() / 5000000L * i % 360L);
            drawCircle(x, y, (float)(i * 10), rot - 180, rot, Color.WHITE);
        }
    }

    public static void drawCircle(float x, float y, float radius, int start, int end, Color color) {
        enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        color(color);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (float i = start; i <= end; i += 1.0f) {
            GL11.glVertex2f(
                    x + (float)Math.cos(i * Math.PI / 180.0) * radius * 1.001f,
                    y + (float)Math.sin(i * Math.PI / 180.0) * radius * 1.001f
            );
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        enableTexture2D();
        disableBlend();
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);

        for (int i = 0; i <= 360; i += 5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawCircleOutline(float x, float y, float radius, float lineWidth, Color color) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lineWidth);

        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i <= 360; i += 5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }



    public static void drawFace(final EntityPlayer player, int x, int y, int w, int h, float radius) {

        enableTexture2D();
        enableBlend();

        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        mc.getTextureManager().bindTexture(((AbstractClientPlayer) player).getLocationSkin());

        StencilUtil.init(() -> {
            RoundedUtil.drawShaderRound(x, y, w, h, radius, Color.BLACK);
        });

        glPushMatrix();

        glTranslated(x - 1, y - 1, 0);
        glScaled(0.95F, 0.95F, 0);

        Gui.drawModalRectWithCustomSizedTexture(0F, 0, 24, 24, 24, 24, 192, 192);
        Gui.drawModalRectWithCustomSizedTexture(0F, 0, 120, 24, 24, 24, 192, 192);
        StencilUtil.uninit();

        resetColor();
        disableBlend();
        glPopMatrix();

    }
    public static void drawServerESP(final Entity entity, float width, final Color color, boolean drawOutline, boolean drawFill) {
    	
        double realX = entity.serverPosX / 32.0;
        double realY = entity.serverPosY / 32.0;
        double realZ = entity.serverPosZ / 32.0;

        if (entity instanceof EntityLivingBase livingBase) {
            realX = livingBase.realPosX / 32.0;
            realY = livingBase.realPosY / 32.0;
            realZ = livingBase.realPosZ / 32.0;
        }

        final float x = (float) (realX - mc.getRenderManager().renderPosX);
        final float y = (float) (realY - mc.getRenderManager().renderPosY);
        final float z = (float) (realZ - mc.getRenderManager().renderPosZ);

        drawESPPos(x, y, z, width, entity.height + 0.1f, color, drawOutline, drawFill);
    }

    public static void drawOutlinedCylinder(EntityLivingBase entity) {
        try {
            if (entity.height <= 0 || entity.width <= 0) {
                return;
            }

            double posX = entity.realPosX / 32.0;
            double posY = entity.realPosY / 32.0;
            double posZ = entity.realPosZ / 32.0;

            double lastX = entity.lastTickRealPosX / 32.0;
            double lastY = entity.lastTickRealPosY / 32.0;
            double lastZ = entity.lastTickRealPosZ / 32.0;

            double x = interpolate(lastX, posX, mc.timer.renderPartialTicks);
            double y = interpolate(lastY, posY, mc.timer.renderPartialTicks);
            double z = interpolate(lastZ, posZ, mc.timer.renderPartialTicks);

            x -= mc.getRenderManager().renderPosX;
            y -= mc.getRenderManager().renderPosY;
            z -= mc.getRenderManager().renderPosZ;

            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

            GL11.glTranslated(x, y + entity.height + 0.1F, z);
            GL11.glNormal3d(0.0, 1.0, 0.0);
            GL11.glRotated(90, 1, 0, 0);

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH); // ✅ Smooth lines
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

            GL11.glLineWidth(1.4f);

            Color c2 = entity.getEntityId() == -42069 ? new Color(60, 180, 255).darker() : new Color(Hud.getColor(5));

            GL11.glColor4f(c2.getRed() / 255f, c2.getGreen() / 255f, c2.getBlue() / 255f, c2.getAlpha() / 255f);

            final Cylinder cylinder = new Cylinder();
            cylinder.setDrawStyle(GLU.GLU_LINE);
            cylinder.setOrientation(GLU.GLU_OUTSIDE);

            float height = entity.height + 0.1F;

            if (height > 0) {
                cylinder.draw(0.62f, 0.62f, height, 16, 1);
            }

            Color c = new Color(Hud.getColor(7));
            int alpha = 70;
            if (entity.getEntityId() == -42069) {
                GL11.glColor4f(60/255f, 180/255f, 255/255f, 120/255f);
            } else {
                GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha/255f);
            }

            cylinder.setDrawStyle(GLU.GLU_FILL);
            if (height > 0) {
                cylinder.draw(0.62f, 0.65f, height, 16, 1);
            }

            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            GL11.glPopAttrib();
            GL11.glPopMatrix();

        } catch (Exception exception) {
        }
    }


    public static void drawESPPos(float x, float y, float z, float width, float height, final Color color, boolean drawOutline, boolean drawFill) {
        float[] rgba = color.getRGBColorComponents(null);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        if (drawFill) {
            GL11.glColor4f(rgba[0], rgba[1], rgba[2], 0.2f);
            drawFilledBox(x, y, z, width, height);
        }

        if (drawOutline) {
            GL11.glColor4f(rgba[0], rgba[1], rgba[2], 1.0f);
            GL11.glLineWidth(2);
            drawOutLine(x, y, z, width, height);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawFilledBox(float x, float y, float z, float width, float height) {
        width *= 1.5F;

        float x1 = x - width;
        float z1 = z - width;
        float x2 = x + width;
        float z2 = z - width;
        float x3 = x + width;
        float z3 = z + width;
        float x4 = x - width;
        float z4 = z + width;
        float y2 = y + height;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();

        // Bottom face
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y, z1).endVertex();
        buffer.pos(x2, y, z2).endVertex();
        buffer.pos(x3, y, z3).endVertex();
        buffer.pos(x4, y, z4).endVertex();
        tessellator.draw();

        // Top face
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y2, z1).endVertex();
        buffer.pos(x2, y2, z2).endVertex();
        buffer.pos(x3, y2, z3).endVertex();
        buffer.pos(x4, y2, z4).endVertex();
        tessellator.draw();

        // Sides
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y, z1).endVertex();
        buffer.pos(x2, y, z2).endVertex();
        buffer.pos(x2, y2, z2).endVertex();
        buffer.pos(x1, y2, z1).endVertex();

        buffer.pos(x2, y, z2).endVertex();
        buffer.pos(x3, y, z3).endVertex();
        buffer.pos(x3, y2, z3).endVertex();
        buffer.pos(x2, y2, z2).endVertex();

        buffer.pos(x3, y, z3).endVertex();
        buffer.pos(x4, y, z4).endVertex();
        buffer.pos(x4, y2, z4).endVertex();
        buffer.pos(x3, y2, z3).endVertex();

        buffer.pos(x4, y, z4).endVertex();
        buffer.pos(x1, y, z1).endVertex();
        buffer.pos(x1, y2, z1).endVertex();
        buffer.pos(x4, y2, z4).endVertex();
        tessellator.draw();
    }

    public static void drawOutLine(float x, float y, float z, float width, float height) {
        width *= 1.5F;

        float x1 = x - width;
        float z1 = z - width;
        float x2 = x + width;
        float z2 = z - width;
        float x3 = x + width;
        float z3 = z + width;
        float x4 = x - width;
        float z4 = z + width;
        float y2 = y + height;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();

        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y, z1).endVertex();
        buffer.pos(x2, y, z2).endVertex();
        buffer.pos(x3, y, z3).endVertex();
        buffer.pos(x4, y, z4).endVertex();
        tessellator.draw();

        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y2, z1).endVertex();
        buffer.pos(x2, y2, z2).endVertex();
        buffer.pos(x3, y2, z3).endVertex();
        buffer.pos(x4, y2, z4).endVertex();
        tessellator.draw();

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y, z1).endVertex();
        buffer.pos(x1, y2, z1).endVertex();
        buffer.pos(x2, y, z2).endVertex();
        buffer.pos(x2, y2, z2).endVertex();
        buffer.pos(x3, y, z3).endVertex();
        buffer.pos(x3, y2, z3).endVertex();
        buffer.pos(x4, y, z4).endVertex();
        buffer.pos(x4, y2, z4).endVertex();
        tessellator.draw();
    }
}
