package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.Minecraft;
import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.util.IRCUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.gif.GifUtil;
import team.gothaj.client.impl.event.player.SneakSpoofEvent;
import team.gothaj.client.impl.module.visual.Capes;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class LayerCape implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer playerRenderer;

    private final GifUtil gifCape = new GifUtil();
    private long loadedGifVersion = -1;

    private ResourceLocation baseCapeTexture;
    private ResourceLocation capeTexture = new ResourceLocation("gothaj", "img/polar_cape.png");

    public LayerCape(RenderPlayer playerRendererIn) {
        this.playerRenderer = playerRendererIn;
    }

    private void checkLoadGif() {
        long currentVersion = Capes.getGifVersion();

        if (loadedGifVersion == currentVersion && gifCape.isLoaded()) {
            return;
        }

        File customGif = Capes.getSelectedGifFile();
        if (customGif != null && customGif.exists()) {
            gifCape.load(customGif, "cape_gif_" + currentVersion, this::createOverlayFrame);
            loadedGifVersion = currentVersion;
        }
    }

    private BufferedImage createOverlayFrame(BufferedImage gifFrame) {
        int fullCapeWidth = 1024;
        int fullCapeHeight = 512;
        BufferedImage overlay = new BufferedImage(fullCapeWidth, fullCapeHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();

        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, fullCapeWidth, fullCapeHeight);

        g.setComposite(AlphaComposite.Src);

        int gifX = 16;
        int gifY = 16;
        int gifWidth = 160;
        int gifHeight = 256;

        g.drawImage(gifFrame, gifX, gifY, gifWidth, gifHeight, null);
        g.dispose();

        return overlay;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {

        IrcPlayer irc = IrcPlayer.getByIngameName(entitylivingbaseIn.getGameProfile().getName());
        boolean capesOn = GothajClient.getInstance().getModuleManager().find(Capes.class).isToggled();
        boolean isSelf = entitylivingbaseIn instanceof EntityPlayerSP;
        boolean hasVanillaCape = entitylivingbaseIn.hasPlayerInfo() &&
                entitylivingbaseIn.isWearing(EnumPlayerModelParts.CAPE) &&
                entitylivingbaseIn.getLocationCape() != null;

        baseCapeTexture = Capes.capeMode.is("Gothaj") ? new ResourceLocation("gothaj", "img/gothaj_cape.png") : new ResourceLocation("gothaj", "img/cape.png");

        if (!entitylivingbaseIn.isInvisible() && ((capesOn && (irc != null || isSelf)) || hasVanillaCape)) {

            if (Capes.useGif.getProperty() && capesOn && isSelf) {
                checkLoadGif();
                gifCape.update();
            }

            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 0.125F);

            double d0 = entitylivingbaseIn.prevChasingPosX + (entitylivingbaseIn.chasingPosX - entitylivingbaseIn.prevChasingPosX) * (double) partialTicks - (entitylivingbaseIn.prevPosX + (entitylivingbaseIn.posX - entitylivingbaseIn.prevPosX) * (double) partialTicks);
            double d1 = entitylivingbaseIn.prevChasingPosY + (entitylivingbaseIn.chasingPosY - entitylivingbaseIn.prevChasingPosY) * (double) partialTicks - (entitylivingbaseIn.prevPosY + (entitylivingbaseIn.posY - entitylivingbaseIn.prevPosY) * (double) partialTicks);
            double d2 = entitylivingbaseIn.prevChasingPosZ + (entitylivingbaseIn.chasingPosZ - entitylivingbaseIn.prevChasingPosZ) * (double) partialTicks - (entitylivingbaseIn.prevPosZ + (entitylivingbaseIn.posZ - entitylivingbaseIn.prevPosZ) * (double) partialTicks);
            float f = entitylivingbaseIn.prevRotationYaw + (entitylivingbaseIn.rotationYaw - entitylivingbaseIn.prevRotationYaw) * partialTicks;
            double d3 = MathHelper.sin(f * (float) Math.PI / 180.0F);
            double d4 = -MathHelper.cos(f * (float) Math.PI / 180.0F);
            float f1 = MathHelper.clamp((float) d1 * 10.0F, -6.0F, 32.0F);
            float f2 = (float) (d0 * d3 + d2 * d4) * 100.0F;
            float f3 = (float) (d0 * d4 - d2 * d3) * 100.0F;

            if (f2 < 0.0F) f2 = 0.0F;
            if (f2 > 165.0F) f2 = 165.0F;
            if (f1 < -5.0F) f1 = -5.0F;

            float f4 = entitylivingbaseIn.prevCameraYaw + (entitylivingbaseIn.cameraYaw - entitylivingbaseIn.prevCameraYaw) * partialTicks;
            f1 = f1 + MathHelper.sin((entitylivingbaseIn.prevDistanceWalkedModified + (entitylivingbaseIn.distanceWalkedModified - entitylivingbaseIn.prevDistanceWalkedModified) * partialTicks) * 6.0F) * 32.0F * f4;

            final var eventSpoof = new SneakSpoofEvent();
            if (entitylivingbaseIn == Minecraft.getMinecraft().thePlayer) GothajClient.getInstance().getEventBus().fire(eventSpoof);
            if (entitylivingbaseIn.isSneaking() && !eventSpoof.isCancelled()) {
                f1 += 25.0F;
                GlStateManager.translate(0.0F, 0.142F, -0.0178F);
            }

            GlStateManager.rotate(6.0F + f2 / 2.0F + f1, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(f3 / 2.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(-f3 / 2.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

            if (capesOn && Capes.useGif.getProperty() && isSelf) {
                RenderUtil.color4f(Capes.baseColor.getProperty());
                playerRenderer.bindTexture(baseCapeTexture);
                this.playerRenderer.getMainModel().renderCape(0.0625F);
                RenderUtil.resetColor();

                ResourceLocation gifTex = gifCape.getCurrentTexture();
                if (gifTex != null) {
                    playerRenderer.bindTexture(gifTex);
                    this.playerRenderer.getMainModel().renderCape(0.0625F);
                }
            } else {
                boolean renderIrcCape = capesOn && (irc != null || isSelf);

                if (renderIrcCape) {
                    int color = IRCUtil.getIrcColor(entitylivingbaseIn.getGameProfile()).getRGB();
                    if (Capes.matchHud.getProperty() && isSelf) {
                        color = Hud.getColor(5);
                    }
                    if (Capes.capeMode.is("Polar")) {
                        RenderUtil.color4f(color);
                    } else {
                        RenderUtil.resetColor();
                    }

                    this.playerRenderer.bindTexture(baseCapeTexture);
                } else {
                    this.playerRenderer.bindTexture(entitylivingbaseIn.getLocationCape());
                }

                this.playerRenderer.getMainModel().renderCape(0.0625F);
                RenderUtil.resetColor();

                if (renderIrcCape && Capes.capeMode.is("Polar")) {
                    this.playerRenderer.bindTexture(capeTexture);
                    RenderUtil.resetColor();
                    this.playerRenderer.getMainModel().renderCape(0.0625F);
                }
            }

            RenderUtil.resetColor();
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
