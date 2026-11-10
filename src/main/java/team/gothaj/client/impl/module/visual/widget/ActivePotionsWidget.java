package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.optifine.CustomColors;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.gui.widget.WidgetScreen;
import team.gothaj.client.impl.module.visual.Hud;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.Color.RGBtoHSB;
import static java.lang.Integer.MIN_VALUE;
import static net.minecraft.src.Config.isCustomColors;

/**
 * @author vajzii
 * @since 2025
 */
public class ActivePotionsWidget extends Widget {

    public ActivePotionsWidget() {
        super("Active Potions", 8, 132, 120, 50);
    }

    @Override
    public void onRender(ScaledResolution sr) {
        final IFontRenderer font = Fonts.nunito14;

        final List<PotionEffect> potionEffects = mc.thePlayer.getActivePotionEffects()
                .stream()
                .sorted(Comparator.comparingInt(PotionEffect::getDuration))
                .toList();

        float iconSize = 11;
        float iconPadding = 2;
        float padding = 5;
        float topPadding = 3;

        double calculatedHeight = topPadding;
        double maxWidth = 0;


        if (potionEffects.isEmpty()) {
            return;
        }

        if (!getShaderProcessing().isToggled() || !getShaderProcessing().blur.getProperty() || getShaderProcessing().blurBackground.getProperty())
            RoundedUtil.drawRound((float) x, (float) y, (float) width, (float) height, 6f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));

        RoundedUtil.drawGradientLine((float) (x + 5), (float) (y + 16), (float) (width - 10), 0.7F, 1, Color.GRAY, Color.GRAY);

        Fonts.nunito18.drawCenteredStringWithShadow("Active Potions", (float) (x + width / 2f), (float) (y + 5f), -1);

        for (final PotionEffect effect : potionEffects) {
            final String amplifier = convertAmplifierToString(effect.getAmplifier()),
                    name = I18n.format(effect.getEffectName()) + amplifier + "§7: ",
                    duration = MathUtil.convertSecondsToHMmSs((effect.getDuration() * 50L) / 1000L);

            double textWidth = font.getStringWidth(name + "§7" + duration) + iconSize + iconPadding + padding * 2;
            maxWidth = Math.max(maxWidth, textWidth);
            calculatedHeight += font.getHeight() + padding + 0.6;
        }

        height = Math.max((float) (calculatedHeight + 18f), 36f);
        width = Math.max((float) maxWidth, 120);

        double yPos = y + 16.5f;
        double xPos = x;

        yPos += topPadding;

        for (final PotionEffect effect : potionEffects) {
            final String amplifier = convertAmplifierToString(effect.getAmplifier()),
                    name = I18n.format(effect.getEffectName()) + amplifier + "§7: ",
                    duration = MathUtil.convertSecondsToHMmSs((effect.getDuration() * 50L) / 1000L);

            final int normalColor = Potion.potionTypes[effect.getPotionID()].getLiquidColor();
            final Color liquidColor = new Color(isCustomColors() ? CustomColors.getPotionColor(effect.getPotionID(), normalColor) : normalColor);
            final Color result = Color.getHSBColor(RGBtoHSB(liquidColor.getRed(), liquidColor.getGreen(), liquidColor.getBlue(), new float[3])[0], 0.64f, 1f);

            mc.getTextureManager().bindTexture(GuiContainer.inventoryBackground);
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion != null && potion.hasStatusIcon()) {
                int iconIndex = potion.getStatusIconIndex();
                int u = (iconIndex % 8) * 18;
                int v = 198 + (iconIndex / 8) * 18;
                float drawX = (float) xPos + padding;
                float drawY = (float) yPos;

                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();

                float scale = iconSize / 18.0f;
                GlStateManager.translate(drawX, drawY, 0);
                GlStateManager.scale(scale, scale, 1);
                RenderUtil.resetColor();
                Gui.drawModalRectWithCustomSizedTexture(
                        0f, 0f, u, v,
                        18f, 18f,
                        256f, 256f
                );

                GlStateManager.popMatrix();
            }

            font.drawString(
                    name + "§7" + duration,
                    (float) (xPos + iconSize + iconPadding + padding),
                    (float) yPos + 3.5f,
                    result.getRGB(),
                    true
            );

            yPos += font.getHeight() + padding;
        }

        super.onRender(sr);
    }

    @Override
    public void onShader(ShaderOverlayEvent event) {
        final IFontRenderer font = Fonts.nunito14;

        final List<PotionEffect> potionEffects = mc.thePlayer.getActivePotionEffects()
                .stream()
                .sorted(Comparator.comparingInt(PotionEffect::getDuration))
                .toList();

        if (potionEffects.isEmpty()) return;

        float iconSize = 11;
        float iconPadding = 2;
        float padding = 5;
        float topPadding = 3;

        double calculatedHeight = topPadding;
        double maxWidth = 0;

        for (final PotionEffect effect : potionEffects) {
            final String amplifier = convertAmplifierToString(effect.getAmplifier()),
                    name = I18n.format(effect.getEffectName()) + amplifier + "§7: ",
                    duration = MathUtil.convertSecondsToHMmSs((effect.getDuration() * 50L) / 1000L);

            double textWidth = font.getStringWidth(name + "§7" + duration) + iconSize + iconPadding + padding * 2;
            maxWidth = Math.max(maxWidth, textWidth);
            calculatedHeight += font.getHeight() + padding + 0.6;
        }

        height = Math.max((float) (calculatedHeight + 18f), 36f);
        width = Math.max((float) maxWidth, 120);

        double yPos = y + 16.5f;
        double xPos = x;


        RoundedUtil.drawShaderRound((float) x, (float) y, (float) width, (float) height, 6f, Color.BLACK);

      //  RoundedUtil.drawShaderRound((float) (x + 5), (float) (y + 17), (float) (width - 10), 0.5F, 1, Color.BLACK);

        super.onShader(event);
    }

    private static final String[] amplifiers = new String[] { "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X", " XI", " XII", " XIII" };

    private String convertAmplifierToString(final int level) {
        if(level >= amplifiers.length) return "";
        if(level <= -1) return " CCLV";
        return amplifiers[level];
    }
}
