package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.gui.widget.WidgetScreen;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.impl.module.visual.TargetHUD;
import team.gothaj.client.main.GothajClient;

import java.awt.*;

import static java.lang.Integer.MIN_VALUE;
import static net.minecraft.client.renderer.GlStateManager.*;

public final class TargetHUDWidget extends Widget {
    private EntityPlayer target;
    private float currentHealthWidth = 0;
    private float lastHealthWidth = 0;
    private final Frustum frustum = new Frustum();
    private TargetHUD targetHud;

    public TargetHUDWidget() {
        super("TargetHUD", MIN_VALUE, MIN_VALUE, 100, 27, Type.MODULE);
        targetHud = GothajClient.getInstance().getModuleManager().getAndCast(TargetHUD.class);
    }

    @Override
    public void onShader(ShaderOverlayEvent event) {
        if (target == null) return;

        boolean follow = GothajClient.getInstance().getModuleManager().getAndCast(TargetHUD.class).follow.getProperty();
        float x = (float) this.x;
        float y = (float) this.y;
        final float width = (float) this.width;
        final float height = (float) this.height;

        if (follow) {
            frustum.setPosition(mc.getRenderManager().renderPosX, mc.getRenderManager().renderPosY, mc.getRenderManager().renderPosZ);
            if (!frustum.isBoundingBoxInFrustum(target.getEntityBoundingBox())) return;
            final double[] coords = new double[4];
            RenderUtil.convertBox(coords, target);
            if (Double.isNaN(coords[0]) || Double.isNaN(coords[1]) || Double.isNaN(coords[3])) return;
            x = (float) coords[0];
            y = (float) ((coords[1] + coords[3]) / 2f);
        }

        switch (targetHud.mode.getProperty()) {
            case "Gothaj" -> {
                RoundedUtil.drawShaderRound(x, y, width, height, 5, Color.BLACK);
            }

            case "Novoline" -> {
                float rectLength = 35 + mc.fontRendererCrack.getStringWidth(target.getName()) + 40;


                RenderUtil.drawRect(x, y - 1.5f, rectLength - 6, 39, Color.BLACK);
            }

            case "Exhibition" -> {
                float renderWidth = Math.max(120, 40 + mc.fontRendererCrack.getStringWidth(target.getName()));
                RenderUtil.drawRect(x, y - 1.5f, renderWidth, 44, Color.BLACK);
            }
        }

    }

    @Override
    public void onRender(ScaledResolution sr) {

        if (targetHud == null)
            targetHud = GothajClient.getInstance().getModuleManager().getAndCast(TargetHUD.class);

        boolean follow = GothajClient.getInstance().getModuleManager().getAndCast(TargetHUD.class).follow.getProperty();
        boolean initPosition = x == MIN_VALUE || y == MIN_VALUE;

        if (initPosition) {
            x = sr.getScaledWidth() / 2F;
            y = sr.getScaledHeight() / 2F;
        }

        if (mc.currentScreen instanceof WidgetScreen) {
            target = mc.thePlayer;
        } else if (GothajClient.getInstance().getModuleManager().find(KillAura.class).isToggled() && KillAura.target != null) {
            if (KillAura.target instanceof EntityOtherPlayerMP player) {
                target = player;
            }
        } else {
            target = null;
        }

        if (target == null) return;

        if (follow) {
            frustum.setPosition(mc.getRenderManager().renderPosX, mc.getRenderManager().renderPosY, mc.getRenderManager().renderPosZ);
            if (!frustum.isBoundingBoxInFrustum(target.getEntityBoundingBox())) return;
            final double[] coords = new double[4];
            RenderUtil.convertBox(coords, target);
            if (Double.isNaN(coords[0]) || Double.isNaN(coords[1]) || Double.isNaN(coords[3])) return;
            x = (float) coords[0];
            y = (float) ((coords[1] + coords[3]) / 2f);
        }

        switch (targetHud.mode.getProperty()) {
            case "Gothaj" -> {
                final float x = (float) this.x, y = (float) this.y, width = (float) this.width, height = (float) this.height;
                RoundedUtil.drawShaderRound(x - 0.5F, y - 0.5F, width + 1, height + 0.75F, 5, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));

                final int padding = 3;
                RenderUtil.drawFace(target, (int) x + padding, (int) y + padding, (int) height - padding * 2, (int) height - padding * 2, 2);

                final int titleColor = new Color(210, 210, 210).getRGB();
                Fonts.rubik18Bold.drawString(target.getName(), x + height + 0.5F, y + padding + 1.5F, titleColor);

                renderArmor(target, x + height, y + padding + 9);

                final int healthSize = 2;
                final float maxHealth = target.getMaxHealth();
                final float currentHealth = Math.max(0, Math.min(target.getHealth(), maxHealth));
                final float healthPercentage = currentHealth / maxHealth;
                final float healthX = x + height;
                final float fullHealthWidth = (x + width) - healthX - padding;
                final float targetHealthWidth = fullHealthWidth * healthPercentage;

                if (lastHealthWidth == 0) {
                    lastHealthWidth = targetHealthWidth;
                }

                currentHealthWidth = RenderUtil.interpolate(currentHealthWidth, targetHealthWidth, 0.1f);

                if (targetHealthWidth < lastHealthWidth) {
                    lastHealthWidth = RenderUtil.interpolate(lastHealthWidth, targetHealthWidth, 0.015f);
                } else if (targetHealthWidth > lastHealthWidth) {
                    lastHealthWidth = targetHealthWidth;
                }

                RoundedUtil.drawShaderRound(healthX, y + height - (padding + healthSize), fullHealthWidth, healthSize, 1, new Color(25, 25, 25, 140));

                if (lastHealthWidth > currentHealthWidth) {
                    RoundedUtil.drawShaderRound(
                            healthX + currentHealthWidth, y + height - (padding + healthSize),
                            lastHealthWidth - currentHealthWidth, healthSize, 1,
                            new Color(255, 110, 110, 80));
                }

                RoundedUtil.drawShaderGradientRound(
                        healthX, y + height - (padding + healthSize), currentHealthWidth, healthSize, 1,
                        Hud.primaryColor.getProperty(),
                        Hud.primaryColor.getProperty(),
                        Hud.secondaryColor.getProperty(),
                        Hud.secondaryColor.getProperty());

                this.width = Math.max(Fonts.rubik18Bold.getStringWidth(target.getName()), 100) + 5;
                this.height = 27;
            }

            case "Novoline" -> {
                GL11.glPushMatrix();
                float hp = target.getHealth() + target.getAbsorptionAmount();
                final float maxHP = target.getMaxHealth() + target.getAbsorptionAmount() - 0.05f;

                float rectLength = 35 + mc.fontRendererCrack.getStringWidth(target.getName()) + 40, health = (float) (Math.round(hp * 100.0) / 100.0);

                if (health > maxHP) {
                    health *= maxHP / health;
                }

                float amplifier = 100 / maxHP, percent = health * amplifier, space = (rectLength - 50) / 100;

                mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());

                RenderUtil.drawBorderedRect(x - 1f, y - 1.5f, rectLength - 4.5f, 39, (float) 1, new Color(0, 0, 0, 50).getRGB(), new Color(29, 29, 29, 255).getRGB());
                RenderUtil.drawRect(x + 1, y, rectLength - 8, 36, new Color(40, 40, 40, 255).getRGB());

                GL11.glPushMatrix();
                RenderUtil.resetColor();
                Gui.drawScaledCustomSizeModalRect((int) (x + 1), (int) y, 8, 8, 8, 8, 36, 36, 64.0f, 64.0f);

                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Gui.drawScaledCustomSizeModalRect((int) (x + 1), (int) y, 40, 8, 8, 8, 36, 36, 64.0f, 64.0f);

                GL11.glPopMatrix();

                RenderUtil.drawRect(x + 40, y + 16.5f, 100 * space, 12.8f, new Color(0, 0, 0, 50).getRGB());
                RenderUtil.drawRect(x + 40, y + 16.5f, percent * space, 12.8f, Hud.getColor(5));
                RenderUtil.drawRect(x + 40 + currentHealthWidth, y + 16.5f, Math.max(0, lastHealthWidth - currentHealthWidth), 12.8f, new Color(Hud.getColor(7)).darker().getRGB());

                String text = String.format("%.1f", percent) + "%";
                mc.fontRendererCrack.drawString(text, (float) (x + 40 + 50 * space - mc.fontRendererCrack.getStringWidth(text) / 2f),
                        (float) (y + 19f), 0xffffffff, true);
                mc.fontRendererCrack.drawString(target.getName(), (float) (x + 40), (float) (y + 4), 0xffffffff, true);

                GL11.glPopMatrix();

                this.width = rectLength;
                this.height = rectLength / 3 + 3;
            }

            case "Exhibition" -> {
                float renderWidth = Math.max(120, 40 + mc.fontRendererCrack.getStringWidth(target.getName()));

                float hp = target.getHealth();
                float maxHp = target.getMaxHealth();
                float absorption = target.getAbsorptionAmount();
                float healthPercent = Math.min(hp / maxHp, 1f);

                RenderUtil.drawRect(x - 0.5f, y - 0.5f, renderWidth + 1, 45, new Color(12, 12, 12).getRGB());
                RenderUtil.drawRect(x, y, renderWidth, 44, new Color(22, 22, 22).getRGB());
                RenderUtil.drawRect(x + 1, y + 1, renderWidth - 2, 42, new Color(32, 32, 32).getRGB());

                RenderUtil.drawRect(x + 2, y + 2, 38, 38, new Color(18, 18, 18).getRGB());
                RenderUtil.drawFace(target, (int) x + 3, (int) y + 3, 36, 36, 2);

                mc.fontRendererCrack.drawString(target.getName(), x + 42, y + 4, -1, true);

                double dist = target.getDistanceToEntity(mc.thePlayer);
                mc.fontRendererCrack.drawString(
                        String.format("HP: %.1f | Dist: %.0f", hp + absorption, dist),
                        x + 42, y + 14, new Color(180, 180, 180).getRGB(), true);

                float barX = x + 42;
                float barY = y + 26;
                float barWidth = renderWidth - 48;
                float barHeight = 4;

                RenderUtil.drawRect(barX, barY, barWidth, barHeight, new Color(40, 40, 40).getRGB());

                int r, g;
                if (healthPercent > 0.5f) {
                    float f = (healthPercent - 0.5f) * 2f;
                    r = (int) (255 * (1f - f));
                    g = 255;
                } else {
                    float f = healthPercent * 2f;
                    r = 255;
                    g = (int) (255 * f);
                }
                Color healthColor = new Color(Math.min(Math.max(r, 0), 255), Math.min(Math.max(g, 0), 255), 0);

                float filledWidth = barWidth * healthPercent;
                RenderUtil.drawRect(barX, barY, filledWidth, barHeight, healthColor.getRGB());

                if (absorption > 0) {
                    float absWidth = barWidth * Math.min(absorption / maxHp, 1f);
                    float absStart = barX + Math.min(filledWidth, barWidth);
                    RenderUtil.drawRect(absStart, barY, Math.min(absWidth, barX + barWidth - absStart), barHeight, new Color(255, 180, 0, 180).getRGB());
                }

                for (int i = 1; i < 10; i++) {
                    float segX = barX + (barWidth / 10f) * i;
                    RenderUtil.drawRect(segX - 0.5f, barY, 1, barHeight, new Color(0, 0, 0, 120).getRGB());
                }

                renderExhibitionArmor(target, x + 42, y + 33);

                this.width = renderWidth;
                this.height = 44;
            }
        }
        super.onRender(sr);
    }

    private void renderArmor(final EntityPlayer player, final float x, final float y) {
        if (player == null || player.inventory == null || player.inventory.armorInventory == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 0; i < player.inventory.armorInventory.length; i++) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (stack == null) continue;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x + (10 * i), y, 0);
            GlStateManager.scale(0.5F, 0.5F, 0.5F);

            try {
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
            } catch (Exception ignored) {}

            GlStateManager.popMatrix();
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void renderExhibitionArmor(final EntityPlayer player, final float x, final float y) {
        if (player == null || player.inventory == null || player.inventory.armorInventory == null) return;

        for (int i = 0; i < player.inventory.armorInventory.length; i++) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (stack == null) continue;

            float itemX = x + (i * 18);

            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();

            try {
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) itemX, (int) y);
            } catch (Exception ignored) {}

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableBlend();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }
    }
}
