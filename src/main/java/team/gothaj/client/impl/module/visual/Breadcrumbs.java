package team.gothaj.client.impl.module.visual;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author vajzii
 * @since 2025
 */
public class Breadcrumbs extends Module {

    private final List<C03PacketPlayer> crumbs = new ArrayList<>();

    private final BooleanProperty outline = new BooleanProperty("Outline", true);

    private final NumberProperty lineWidth = new NumberProperty("Line Width", 3f, 0.5f, 5f, 0.1f);

    private final NumberProperty size = new NumberProperty("Length", 15, 5, 100, 1);

    public Breadcrumbs() {
        super("Breadcrumbs", "Leaves a trail behind you", ModuleCategory.VISUAL);
        registerProperties(outline, lineWidth, size);
    }

    @Listen
    public void onPacket(PacketEvent event) {


        if (event.getDirection() == PacketEvent.Direction.SEND) {

            if (event.getPacket() instanceof C03PacketPlayer packet) {
                if (packet.isMoving()) {
                    crumbs.add(packet);
                }
            }
        }
    }

    @Listen
    public void onRender(RenderDimensionEvent event) {

        if (!crumbs.isEmpty() && crumbs.size() > size.getProperty()) {
            crumbs.removeFirst();
        }

        mc.entityRenderer.setupCameraTransform(event.getPartialTicks(), 0);
        GlStateManager.pushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        mc.entityRenderer.disableLightmap();

        double renderPosX = mc.getRenderManager().renderPosX;
        double renderPosY = mc.getRenderManager().renderPosY;
        double renderPosZ = mc.getRenderManager().renderPosZ;


        if (outline.getProperty()) {
            GL11.glLineWidth(lineWidth.getProperty() + 1);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            RenderUtil.color(Color.BLACK);

            if (!crumbs.isEmpty()) {
                crumbs.forEach(packet -> GL11.glVertex3d(packet.getPositionX() - renderPosX, packet.getPositionY() - renderPosY, packet.getPositionZ() - renderPosZ));
            }
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glEnd();
        }

        GL11.glLineWidth(lineWidth.getProperty());
        GL11.glBegin(GL11.GL_LINE_STRIP);

        if (!crumbs.isEmpty()) {
            int index = 0;
            for (C03PacketPlayer packet : crumbs) {
                RenderUtil.color(Hud.getColor(index * 2));

                GL11.glVertex3d(packet.getPositionX() - renderPosX, packet.getPositionY() - renderPosY, packet.getPositionZ() - renderPosZ);
                index++;
            }
        }


        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.popMatrix();
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.crumbs.clear();
        super.onDisable();
    }
}
