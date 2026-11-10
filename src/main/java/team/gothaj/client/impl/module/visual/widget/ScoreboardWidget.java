package team.gothaj.client.impl.module.visual.widget;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collection;
import java.util.List;

import static java.lang.Integer.MIN_VALUE;
import static net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName;

import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.widget.Resolution;
import team.gothaj.client.api.widget.Widget;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

public class ScoreboardWidget extends Widget {

    Minecraft mc = Minecraft.getMinecraft();

    public ScoreboardWidget() {
        super("Scoreboard", MIN_VALUE, MIN_VALUE, 120, 120);
    }

    @Override
    public void onRender(ScaledResolution sr) {
        renderScoreboard(false);
        super.onRender(sr);
    }

    @Override
    public void onShader(ShaderOverlayEvent event) {
        if (Hud.scoreboardOpt.getOptionState("Shaders")) {
            renderScoreboard(true);
        }
        super.onShader(event);
    }

    private void renderScoreboard(boolean shader) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution resolution = new ScaledResolution(mc);
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (scoreobjective == null) return;

        Collection<Score> scores = scoreboard.getSortedScores(scoreobjective);
        List<Score> validScores = Lists.newArrayList(Iterables.filter(scores, score ->
                score.getPlayerName() != null && !score.getPlayerName().startsWith("#")));
        if (validScores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(validScores, validScores.size() - 15));
        } else {
            scores = validScores;
        }

        boolean useCustomFont = Hud.scoreboardOpt.getOptionState("Custom Font");
        FontRenderer fontRenderer = mc.fontRendererObj;
        IFontRenderer customFont = Fonts.nunito18;
        float scoreboardWidth = useCustomFont ? (int) customFont.getStringWidth(scoreobjective.getDisplayName()) : fontRenderer.getStringWidth(scoreobjective.getDisplayName());
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String entry = formatPlayerName(team, score.getPlayerName());
            scoreboardWidth = (useCustomFont ? 0.3f + Math.max(scoreboardWidth, customFont.getStringWidth(entry)) : 0.3F + Math.max(scoreboardWidth, fontRenderer.getStringWidth(entry)));
        }

        boolean shouldInitPosition = x == MIN_VALUE || y == MIN_VALUE;
        int overallHeight = scores.size() * fontRenderer.FONT_HEIGHT;
        float calculatedX;
        float calculatedY;

        if (shouldInitPosition) {
            calculatedX = resolution.getScaledWidth() - scoreboardWidth - 2;
            calculatedY = (resolution.getScaledHeight() / 2f + overallHeight / 3f) - scores.size() * fontRenderer.FONT_HEIGHT;
            x = calculatedX;
            y = calculatedY;
        } else {
            calculatedX = (float) x;
            calculatedY = (float) y;
        }

        float endX = calculatedX + scoreboardWidth + 4;
        float endY = calculatedY + overallHeight + fontRenderer.FONT_HEIGHT + 2;
        width = endX - calculatedX;
        height = endY - calculatedY + 1;

        if (Hud.scoreboardOpt.getOptionState("Rounded")) {
            if (shader) {
                RoundedUtil.drawShaderRound(calculatedX, calculatedY, (float) width, (float) height, 6f, Color.BLACK);
            } else {
                if (!getShaderProcessing().isToggled() || !getShaderProcessing().blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                    RoundedUtil.drawRound(calculatedX, calculatedY, (float) width, (float) height, 6f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
                }
            }
        } else {
            if (shader) {
                Gui.drawRect((int) calculatedX, (int) calculatedY, (int) endX, (int) endY, Color.BLACK.getRGB());
            } else {
                if (!getShaderProcessing().isToggled() || !getShaderProcessing().blur.getProperty() || getShaderProcessing().blurBackground.getProperty()) {
                    Gui.drawRect((int) calculatedX, (int) calculatedY, (int) endX, (int) endY, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()).getRGB());
                }
            }
        }

        if (!shader) {
            String title = scoreobjective.getDisplayName();
            GL11.glColor4f(1f, 1f, 1f, 1f);
            float titleWidth = useCustomFont ? customFont.getStringWidth(title) : mc.fontRendererObj.getStringWidth(title);

            if (useCustomFont) {

                if (titleWidth >= 60) {
                    customFont.drawString(title, calculatedX + 3, (int) calculatedY + 3, -1);
                } else {
                    customFont.drawCenteredString(title, calculatedX + scoreboardWidth / 2f, (int) calculatedY + 3, -1);
                }
            } else {
                if (titleWidth >= 60) {
                    fontRenderer.drawString(title, (int) calculatedX + 3, (int) calculatedY + 3, 0xFFFFFF);
                } else {
                    fontRenderer.drawCenteredString(title, (int) (calculatedX + scoreboardWidth / 2f), (int) calculatedY + 3, 0xFFFFFF);
                }

            }
            int drawnScores = 0;
            for (Score score : scores) {
                ++drawnScores;
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String entry = formatPlayerName(team, score.getPlayerName());
                final float textY = endY - drawnScores * (useCustomFont ? Fonts.nunito18.getHeight() : fontRenderer.FONT_HEIGHT);

                if (drawnScores == 1 && entry.contains(".")) {
                    if (useCustomFont) {
                        FontUtil.drawHorizontalGradient(customFont, GothajClient.getInstance().getFancyName(), calculatedX + 3, textY, 7);
                    } else {
                        FontUtil.drawHorizontalGradient(fontRenderer, GothajClient.getInstance().getFancyName(), calculatedX + 3, textY, 7);
                    }
                } else {
                    if (useCustomFont) {
                        StringBuilder currentSegment = new StringBuilder();
                        float currentX = (int) calculatedX + 2;
                        float textYInt = (int) textY;
                        int color = new Color(208, 208, 208).getRGB();

                        for (int i = 0; i < entry.length(); i++) {
                            char c = entry.charAt(i);

                            if (c == '✔' || c == '✖') {
                                if (!currentSegment.isEmpty()) {
                                    customFont.drawString(currentSegment.toString(), currentX, textYInt, color);
                                    currentX += customFont.getStringWidth(currentSegment.toString());
                                    currentSegment = new StringBuilder();
                                }

                                fontRenderer.drawString((c == '✔' ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + String.valueOf(c), (int) currentX, (int) textYInt, 0xD0D0D0);
                                currentX += fontRenderer.getStringWidth(String.valueOf(c));
                            } else {
                                currentSegment.append(c);
                            }
                        }

                        if (!currentSegment.isEmpty()) {
                            customFont.drawString(currentSegment.toString(), currentX, textYInt, color);
                        }
                    } else {
                        fontRenderer.drawString(entry, (int) calculatedX + 2, (int) textY, 0xD0D0D0);
                    }

                }
            }
        }
    }

    @Override
    public void onFullScreen(float w, float h) {
        super.onFullScreen(w, h);
    }

    @Override
    public void onResize(ScaledResolution sr) {
        super.onResize(sr);
    }
}
