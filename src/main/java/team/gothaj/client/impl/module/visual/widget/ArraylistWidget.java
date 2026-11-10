package team.gothaj.client.impl.module.visual.widget;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.MIN_VALUE;

public class ArraylistWidget extends Widget {

    IFontRenderer font;

    public ArraylistWidget() {
        super("Arraylist", MIN_VALUE, MIN_VALUE, 120.01f, 120);
    }

    private static double snap(double v) {
        return v;
    }


    @Override
    public void onShader(ShaderOverlayEvent event) {
        if (Hud.arraylistOpt.getOptionState("Shaders")) {
            font = Hud.cFont.getProperty();
            renderList(true, event.getType());
        }
        super.onShader(event);
    }

    @Override
    public void onRender(ScaledResolution sr) {
        font = Hud.cFont.getProperty();
        renderList(false, null);
        super.onRender(sr);
    }

    private void renderList(boolean isShader, ShaderOverlayEvent.Type shaderType) {
        ScaledResolution sr = new ScaledResolution(mc);
        final boolean useCFont = Hud.arraylistOpt.getOptionState("Custom Font");

        List<Module> mods = getSortedModules(useCFont);
        if (mods.isEmpty()) return;

        int maxWidth = mods.stream()
                .filter(Module::isToggled)
                .mapToInt(m -> getModuleWidth(m, useCFont))
                .max()
                .orElseGet(() -> mods.stream().mapToInt(m -> getModuleWidth(m, useCFont)).max().orElse(0));

        int padding = 6;
        float prevWidth = width;
        width = maxWidth + padding;
        height = (useCFont ? font.getHeight() : mc.fontRendererObj.FONT_HEIGHT) * (mods.size()) + (int) (Hud.moduleSpacing.getProperty() * (mods.size()));

        boolean initPosition = x == MIN_VALUE || y == MIN_VALUE;
        if (initPosition) {
            x = sr.getScaledWidth() - maxWidth - 8;
            y = 3;
        } else if (!isShader) {
            if(prevWidth != 120.01f)x += prevWidth - width;
        }

        int fontHeight = useCFont ? font.getHeight() : mc.fontRendererObj.FONT_HEIGHT;
        final float spacing = Hud.moduleSpacing.getProperty();
        final float fullLineHeight = fontHeight + spacing;

        boolean hasBackground = Hud.arraylistOpt.getOptionState("Background");
        boolean hasOutline = Hud.arraylistOpt.getOptionState("Outline");
        boolean hasHorizontalColor = Hud.arraylistOpt.getOptionState("Horizontal Color");
        boolean hasSyncedWave = Hud.syncWave.getProperty();
        boolean hasLeft = Hud.lineOpt.getOptionState("Left");
        boolean hasRight = Hud.lineOpt.getOptionState("Right");
        boolean hasTop = Hud.lineOpt.getOptionState("Top");
        boolean hasBottom = Hud.lineOpt.getOptionState("Bottom");

        boolean isRounded = Hud.arraylistOpt.getOptionState("Rounded");

        float drawY = (float) y;

        int[] widths = new int[mods.size()];

        for (int i = 0; i < mods.size(); i++) {
            String n = Hud.caseOpt.is("Normal")
                    ? mods.get(i).getDisplayName(Hud.arraylistOpt.getOptionState("Split Name")) : Hud.caseOpt.is("Upper")
                    ? mods.get(i).getDisplayName(Hud.arraylistOpt.getOptionState("Split Name")).toUpperCase() : Hud.caseOpt.is("Lower")
                    ? mods.get(i).getDisplayName(Hud.arraylistOpt.getOptionState("Split Name")).toLowerCase() : mods.get(i).getDisplayName(Hud.arraylistOpt.getOptionState("Split Name"));

            widths[i] = useCFont ? (int) font.getStringWidth(n) : mc.fontRendererObj.getStringWidth(n);
        }

        for (int i = 0; i < mods.size(); i++) {
            Module module = mods.get(i);
            String name = getFormattedName(module);
            int textWidth = getModuleWidth(module, useCFont);

            if (!isShader) {
                module.getTranslate().animateSlide(module.isToggled());
            }

            double animProgress = module.getTranslate().getArrayListAnimation().getValue();

            double clampedProgress = Math.clamp(animProgress, 0.0, 1.0);

            double dynamicHeight = fullLineHeight * clampedProgress;

            double xHiddenOffset = (textWidth + 5) * (1.0 - clampedProgress);

            float targetX = (float) (x + width - textWidth);

            if (isShader) {
                module.getTranslate().setX(targetX);
                module.getTranslate().setY(drawY);
            } else if (!isDragging()) {
                module.getTranslate().animate(targetX, drawY);
            } else {
                module.getTranslate().setX(targetX);
                module.getTranslate().setY(drawY);
            }

            double animX = module.getTranslate().getX() + xHiddenOffset;
            double animY = module.getTranslate().getY();

            double rowX = animX + Hud.xOffset.getProperty();
            double rowY = animY + Hud.yOffset.getProperty();

            if (animProgress > 0.01 && rowX < sr.getScaledWidth()) {

                double bgLeft = rowX - 2;
                double bgRight = rowX + textWidth + 2;

                double bgTop = rowY - 2;
                double bgBottom = rowY - 2 + fullLineHeight;

                if (hasBackground) {
                    boolean isFirst = (i == 0);
                    boolean isLast = (i == mods.size() - 1);

                    float radius = isRounded ? 3f : 0f;
                    float sameThreshold = 1f;

                    boolean widerThanPrev = !isFirst && (textWidth - widths[i - 1]) > sameThreshold;
                    boolean widerThanNext = !isLast  && (textWidth - widths[i + 1]) > sameThreshold;

                    float rtl = (isFirst || widerThanPrev) ? radius : 0f;
                    float rtr = isFirst ? radius : 0f;
                    float rbl = (isLast  || widerThanNext) ? radius : 0f;
                    float rbr = isLast  ? radius : 0f;

                    float bx = (float) snap(bgLeft);
                    float by = (float) snap(bgTop);
                    float bw = (float) snap(bgRight - bgLeft);
                    float bh = (float) snap(bgBottom - bgTop) - 1.3f;


                    if (isShader) {
                        if (shaderType == ShaderOverlayEvent.Type.BLUR || shaderType == ShaderOverlayEvent.Type.BLOOM) {
                            if (isRounded) {
                                RoundedUtil.drawRound((float) bgLeft, (float) bgTop, (float) (bgRight - bgLeft), (float) (bgBottom - bgTop),
                                        rtl, rtr, rbl, rbr, Color.BLACK);
                            } else {
                                RenderUtil.rect(bgLeft, bgTop, bgRight, bgBottom, Color.BLACK);
                            }
                        }
                    } else {
                        Color bgColor = new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue());
                        if (isRounded) {
                            RoundedUtil.drawRound(bx, by, bw, bh, rtl, rtr, rbl, rbr, bgColor);
                        } else {
                            RenderUtil.rect(snap(bgLeft), snap(bgTop), snap(bgRight), (float) snap(bgBottom), bgColor);
                        }
                    }
                }

                if (!isShader) {
                    int color = Hud.getColor(i);

                    if (hasOutline) {

                        if (hasLeft) {
                            RenderUtil.rect(snap(bgLeft - 1), snap(bgTop), snap(bgLeft), snap(bgBottom), color);
                        }

                        if (hasRight) {
                            RenderUtil.rect(snap(bgRight), snap(bgTop), snap(bgRight + 1), snap(bgBottom), color);
                        }


                        if (hasTop) {
                            if (i == 0) {
                                double capLeft  = hasLeft  ? bgLeft - 1 : bgLeft;
                                double capRight = hasRight ? bgRight + 1 : bgRight;
                                RenderUtil.rect(snap(capLeft), snap(bgTop - 1), snap(capRight), snap(bgTop), color);
                            } else {
                                int prevW = widths[i - 1];
                                if (textWidth > prevW) {
                                    double prevLeft = bgRight - prevW;
                                    RenderUtil.rect(snap(bgLeft), snap(bgTop - 1), snap(prevLeft), snap(bgTop), color);
                                }
                            }
                        }

                        if (hasBottom) {
                            if (i == mods.size() - 1) {
                                RenderUtil.rect(snap(bgLeft - 1), snap(bgBottom), snap(bgRight + 1), snap(bgBottom + 1), color);
                            } else {
                                int nextW = widths[i + 1];
                                if (textWidth > nextW) {
                                    double nextLeft = bgRight - nextW;
                                    RenderUtil.rect(snap(bgLeft - 1), snap(bgBottom), snap(nextLeft - 5), snap(bgBottom + 1), color);
                                }
                            }
                        }
                    }

                    float textX = (float) rowX - 1;
                    float textY = (float) rowY - 1;

                    if (hasHorizontalColor) {
                        if (useCFont) {
                            if (hasSyncedWave) FontUtil.drawSyncedGradient(font, name, textX, textY, i);
                            else FontUtil.drawHorizontalGradient(font, name, textX, textY, i);
                        } else {
                            if (hasSyncedWave) FontUtil.drawSyncedGradient(mc.fontRendererObj, name, textX, textY, i);
                            else FontUtil.drawHorizontalGradient(mc.fontRendererObj, name, textX, textY, i);
                        }
                    } else {
                        if (useCFont) font.drawStringWithShadow(name, textX, textY, color);
                        else mc.fontRendererObj.drawStringWithShadow(name, textX, textY, color);
                    }
                }
            }

            drawY += (float) dynamicHeight;
        }
    }

    private List<Module> getSortedModules(boolean useCFont) {
        return GothajClient.getInstance().getModuleManager().getItems()
                .stream()
                .filter(mod -> !mod.isHidden() && (mod.isToggled() || (mod.getTranslate().getArrayListAnimation() != null && !mod.getTranslate().getArrayListAnimation().isFinished())))
                .sorted(Comparator.comparingDouble(m -> -getModuleWidth(m, useCFont)))
                .collect(Collectors.toList());
    }

    private String getFormattedName(Module m) {
        String n = m.getDisplayName(Hud.arraylistOpt.getOptionState("Split Name"));
        if (Hud.caseOpt.is("Upper")) return n.toUpperCase();
        if (Hud.caseOpt.is("Lower")) return n.toLowerCase();
        return n;
    }

    private int getModuleWidth(Module m, boolean useCFont) {
        if (this.font == null && useCFont) return 0;
        String n = getFormattedName(m);
        return useCFont ? (int) font.getStringWidth(n) : mc.fontRendererObj.getStringWidth(n);
    }
}
