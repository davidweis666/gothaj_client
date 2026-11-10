package net.minecraft.client.gui;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;

import systems.vajzii.irc.players.IrcPlayer;
import systems.vajzii.irc.util.IRCUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.module.client.ShaderProcessing;
import team.gothaj.client.impl.module.visual.BetterTab;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;

public class GuiPlayerTabOverlay extends Gui
{
    private static final Ordering<NetworkPlayerInfo> field_175252_a = Ordering.from(new GuiPlayerTabOverlay.PlayerComparator());
    private final Minecraft mc;
    private final GuiIngame guiIngame;
    private IChatComponent footer;
    private IChatComponent header;
    private long lastTimeOpened;
    private boolean isBeingRendered;

    @Getter
    private float openScale = 0f;

    public GuiPlayerTabOverlay(Minecraft mcIn, GuiIngame guiIngameIn)
    {
        this.mc = mcIn;
        this.guiIngame = guiIngameIn;
    }

    public String getPlayerName(NetworkPlayerInfo networkPlayerInfoIn) {

        if (IrcPlayer.getByIngameName(networkPlayerInfoIn.getGameProfile().getName()) != null) {
            return networkPlayerInfoIn.getDisplayName() != null ? IRCUtil.getIrcPrefix(networkPlayerInfoIn.getGameProfile()) + "§l" + networkPlayerInfoIn.getDisplayName().getFormattedText() + " §7(" + IrcPlayer.getByIngameName(networkPlayerInfoIn.getGameProfile().getName()).getIrcNick() + ")" : IRCUtil.getIrcPrefix(networkPlayerInfoIn.getGameProfile()) + ScorePlayerTeam.formatPlayerName(networkPlayerInfoIn.getPlayerTeam(), "§l" + networkPlayerInfoIn.getGameProfile().getName() + " §7(" + IrcPlayer.getByIngameName(networkPlayerInfoIn.getGameProfile().getName()).getIrcNick() + ")");
        } else {
            return networkPlayerInfoIn.getDisplayName() != null ? networkPlayerInfoIn.getDisplayName().getFormattedText() : ScorePlayerTeam.formatPlayerName(networkPlayerInfoIn.getPlayerTeam(), networkPlayerInfoIn.getGameProfile().getName());
        }
    }

    public void updatePlayerList(boolean willBeRendered)
    {
        if (willBeRendered && !this.isBeingRendered)
        {
            this.lastTimeOpened = Minecraft.getSystemTime();
            openScale = 0.0f;
        }

        this.isBeingRendered = willBeRendered;
    }

    public void renderPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn) {
        renderPlayerlist(width, scoreboardIn, scoreObjectiveIn, false);
    }

    public void renderPlayerlistShader(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn) {
        renderPlayerlist(width, scoreboardIn, scoreObjectiveIn, true);
    }

    private void renderPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, boolean shader) {
        float targetScale = isBeingRendered ? 1.0f : 0f;
        openScale += (targetScale - openScale) * 0.05f;

        if (Math.abs(openScale - targetScale) < 0.001f) {
            openScale = targetScale;
        }

        BetterTab betterTab = GothajClient.getInstance().getModuleManager().getAndCast(BetterTab.class);
        ShaderProcessing shaderProcessing = GothajClient.getInstance().getModuleManager().getAndCast(ShaderProcessing.class);

        FontRenderer mcFont = this.mc.fontRendererObj;
        IFontRenderer customFont = Fonts.nunito18;
        boolean useCustomFont = betterTab.isToggled() && betterTab.tabOpt.getOptionState("Custom Font");

        NetHandlerPlayClient nethandlerplayclient = this.mc.thePlayer.sendQueue;
        List<NetworkPlayerInfo> list = field_175252_a.<NetworkPlayerInfo>sortedCopy(nethandlerplayclient.getPlayerInfoMap());
        int i = 0;
        int j = 0;

        for (NetworkPlayerInfo networkplayerinfo : list) {
            int k = useCustomFont ? (int) customFont.getStringWidth(this.getPlayerName(networkplayerinfo)) : mcFont.getStringWidth(this.getPlayerName(networkplayerinfo));
            i = Math.max(i, k);

            if (scoreObjectiveIn != null && scoreObjectiveIn.getRenderType() != IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
                k = useCustomFont ? (int) customFont.getStringWidth(" " + scoreboardIn.getValueFromObjective(networkplayerinfo.getGameProfile().getName(), scoreObjectiveIn).getScorePoints()) : mcFont.getStringWidth(" " + scoreboardIn.getValueFromObjective(networkplayerinfo.getGameProfile().getName(), scoreObjectiveIn).getScorePoints());
                j = Math.max(j, k);
            }
        }

        list = list.subList(0, Math.min(list.size(), 80));
        int l3 = list.size();
        int i4 = l3;
        int j4;

        for (j4 = 1; i4 > 20; i4 = (l3 + j4 - 1) / j4) {
            ++j4;
        }

        boolean flag = this.mc.isIntegratedServerRunning() || (this.mc.getNetHandler().getNetworkManager().getIsencrypted() || (betterTab.isToggled() && betterTab.tabOpt.getOptionState("Always Head")));
        int l;

        if (scoreObjectiveIn != null) {
            if (scoreObjectiveIn.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
                l = 90;
            } else {
                l = j;
            }
        } else {
            l = 0;
        }

        int i1 = Math.min(j4 * ((flag ? 9 : 0) + i + l + (useCustomFont ? 20 : 13)), width - 50) / j4;
        int j1 = width / 2 - (i1 * j4 + (j4 - 1) * 5) / 2;
        int k1 = 10;
        int l1 = i1 * j4 + (j4 - 1) * 5;
        List<String> list1 = null;
        List<String> list2 = null;

        if (this.header != null) {
            list1 = mcFont.listFormattedStringToWidth(this.header.getFormattedText(), width - 50);

            for (String s : list1) {
                l1 = (int) Math.max(l1, useCustomFont ? customFont.getStringWidth(s) : mcFont.getStringWidth(s));
            }
        }

        if (this.footer != null) {
            list2 = mcFont.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

            for (String s2 : list2) {
                l1 = (int) Math.max(l1, useCustomFont ? customFont.getStringWidth(s2) : mcFont.getStringWidth(s2));
            }
        }

        int fontHeight = useCustomFont ? customFont.getHeight() : mcFont.FONT_HEIGHT;

        int totalHeight = 0;
        int headerStartY = k1 - 1;

        if (list1 != null) {
            totalHeight += list1.size() * fontHeight + 1;
        }

        totalHeight += i4 * 9;

        if (list2 != null) {
            totalHeight += list2.size() * fontHeight + 1;
        }

        float slideOffset = (-totalHeight - 12) * (1.0f - openScale);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, slideOffset, 0);

        if (betterTab.isToggled() && betterTab.tabOpt.getOptionState("Rounded")) {
            if (shader) {
                RoundedUtil.drawShaderRound(width / 2f - l1 / 2f - 1, headerStartY, l1 + 2, totalHeight, 6f, Color.BLACK);
            } else {
                if (!betterTab.tabOpt.getOptionState("Shaders") || (!shaderProcessing.isToggled() || !shaderProcessing.blur.getProperty() || shaderProcessing.blurBackground.getProperty())) {
                    RoundedUtil.drawRound(width / 2f - l1 / 2f - 1, headerStartY, l1 + 2, totalHeight, 6f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
                }
            }
        } else {
            if (list1 != null) {
                if (shader) {
                    drawRect(width / 2f - l1 / 2f - 1, k1 - 1, width / 2f + l1 / 2f + 1, k1 + list1.size() * fontHeight, Color.BLACK.getRGB());
                } else {
                    if (!betterTab.tabOpt.getOptionState("Shaders") || (!shaderProcessing.isToggled() || !shaderProcessing.blur.getProperty() || shaderProcessing.blurBackground.getProperty())) {
                        drawRect(width / 2f - l1 / 2f - 1, k1 - 1, width / 2f + l1 / 2f+ 1, k1 + list1.size() * fontHeight, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()).getRGB());
                    }
                }
            }
        }

        if (list1 != null && !shader) {
            for (String s3 : list1) {
                int i2 = useCustomFont ? (int) customFont.getStringWidth(s3) : mcFont.getStringWidth(s3);

                if (useCustomFont) {
                    drawTabLineWithAlpha(customFont, s3, (float) (width / 2f- i2 / 2f), (float) k1, 255);
                } else {
                    mcFont.drawStringWithShadow(s3, (float) (width / 2f- i2 / 2f), (float) k1, -1);
                }
                k1 += fontHeight;
            }
            ++k1;
        } else if (list1 != null) {
            k1 += list1.size() * fontHeight + 1;
        }

        if (!betterTab.isToggled() || !betterTab.tabOpt.getOptionState("Rounded")) {
            if (shader) {
                drawRect(width / 2f - l1 / 2f - 1, k1 - 1, width / 2f + l1 / 2f + 1, k1 + i4 * 9, Color.BLACK.getRGB());
            } else {
                if (!betterTab.tabOpt.getOptionState("Shaders") || (!shaderProcessing.isToggled() || !shaderProcessing.blur.getProperty() || shaderProcessing.blurBackground.getProperty())) {
                    drawRect(width / 2f - l1 / 2f - 1, k1 - 1, width / 2f + l1 / 2f + 1, k1 + i4 * 9, Integer.MIN_VALUE);
                }
            }
        }

        if (!shader) {
            for (int k4 = 0; k4 < l3; ++k4) {
                int l4 = k4 / i4;
                int i5 = k4 % i4;
                int j2 = j1 + l4 * i1 + l4 * 5;
                int k2 = k1 + i5 * 9;
                drawRect(j2, k2, j2 + i1, k2 + 8, useCustomFont ? new Color(60, 60, 60, 80).getRGB() : 553648127);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                if (k4 < list.size()) {
                    NetworkPlayerInfo networkplayerinfo1 = (NetworkPlayerInfo) list.get(k4);
                    String s1 = this.getPlayerName(networkplayerinfo1);
                    GameProfile gameprofile = networkplayerinfo1.getGameProfile();

                    if (flag) {
                        EntityPlayer entityplayer = this.mc.theWorld.getPlayerEntityByUUID(gameprofile.getId());
                        boolean flag1 = entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.CAPE) && (gameprofile.getName().equals("Dinnerbone") || gameprofile.getName().equals("Grumm"));
                        this.mc.getTextureManager().bindTexture(networkplayerinfo1.getLocationSkin());
                        int l2 = 8 + (flag1 ? 8 : 0);
                        int i3 = 8 * (flag1 ? -1 : 1);
                        Gui.drawScaledCustomSizeModalRect(j2, k2, 8.0F, (float) l2, 8, i3, 8, 8, 64.0F, 64.0F);

                        if (entityplayer != null && entityplayer.isWearing(EnumPlayerModelParts.HAT)) {
                            int j3 = 8 + (flag1 ? 8 : 0);
                            int k3 = 8 * (flag1 ? -1 : 1);
                            Gui.drawScaledCustomSizeModalRect(j2, k2, 40.0F, (float) j3, 8, k3, 8, 8, 64.0F, 64.0F);
                        }

                        j2 += 9;
                    }

                    if (networkplayerinfo1.getGameType() == WorldSettings.GameType.SPECTATOR) {
                        s1 = EnumChatFormatting.ITALIC + s1;

                        if (useCustomFont) {
                            drawTabLineWithAlpha(customFont, s1, (float) j2, (float) k2, 109);
                        } else {
                            mcFont.drawStringWithShadow(s1, (float) j2, (float) k2, -1862270977);
                        }
                    } else {
                        if (useCustomFont) {
                            drawTabLineWithAlpha(customFont, s1, (float) j2, (float) k2, 255);
                        } else {
                            mcFont.drawStringWithShadow(s1, (float) j2, (float) k2, -1);
                        }
                    }

                    if (scoreObjectiveIn != null && networkplayerinfo1.getGameType() != WorldSettings.GameType.SPECTATOR) {
                        int k5 = j2 + i + 1;
                        int l5 = k5 + l;

                        if (l5 - k5 > 5) {
                            if (useCustomFont) {
                                this.drawScoreboardValues(scoreObjectiveIn, k2, gameprofile.getName(), k5, l5, networkplayerinfo1, customFont);
                            } else {
                                this.drawScoreboardValues(scoreObjectiveIn, k2, gameprofile.getName(), k5, l5, networkplayerinfo1, mcFont);
                            }
                        }
                    }

                    this.drawPing(i1, j2 - (flag ? 9 : 0), k2, networkplayerinfo1);
                }
            }
        }

        if (list2 != null) {
            k1 = k1 + i4 * 9 + 1;

            if (!betterTab.isToggled() || !betterTab.tabOpt.getOptionState("Rounded")) {
                if (shader) {
                    drawRect(width / 2f- l1 / 2f- 1, k1 - 1, width / 2f+ l1 / 2f+ 1, k1 + list2.size() * fontHeight, Color.BLACK.getRGB());
                } else {
                    if (!betterTab.tabOpt.getOptionState("Shaders") || (!shaderProcessing.isToggled() || !shaderProcessing.blur.getProperty() || shaderProcessing.blurBackground.getProperty())) {
                        drawRect(width / 2f- l1 / 2f- 1, k1 - 1, width / 2f+ l1 / 2f+ 1, k1 + list2.size() * fontHeight, Integer.MIN_VALUE);
                    }
                }
            }

            if (!shader) {
                for (String s4 : list2) {
                    int j5 = useCustomFont ? (int) customFont.getStringWidth(s4) : mcFont.getStringWidth(s4);
                    if (useCustomFont) {
                        drawTabLineWithAlpha(customFont, s4, (float) (width / 2f- j5 / 2), (float) k1, 255);
                    } else {
                        mcFont.drawStringWithShadow(s4, (float) (width / 2f- j5 / 2), (float) k1, -1);
                    }
                    k1 += fontHeight;
                }
            }
        }

        GlStateManager.popMatrix();
    }

    private void drawTabLineWithAlpha(IFontRenderer fontRenderer, String text, float x, float y, int alpha) {
        if (text == null || text.isEmpty()) return;

        StringBuilder currentSegment = new StringBuilder();
        int baseColor = 0xFFFFFF;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                if (!currentSegment.isEmpty()) {
                    int flags = 0;
                    if (bold) flags |= 1;
                    if (italic) flags |= 2;
                    if (underline) flags |= 4;
                    if (strikethrough) flags |= 8;
                    if (obfuscated) flags |= 16;

                    float width = drawFormattedString(fontRenderer, currentSegment.toString(),
                            x, y, baseColor | (alpha << 24), flags);
                    x += width;
                    currentSegment.setLength(0);
                }

                char formatCode = Character.toLowerCase(text.charAt(i + 1));

                int colorIndex = "0123456789abcdef".indexOf(formatCode);
                if (colorIndex >= 0) {
                    int[] mcColors = {
                            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
                            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
                            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
                    };
                    baseColor = mcColors[colorIndex];
                    bold = italic = underline = strikethrough = obfuscated = false;
                }
                // Formatting codes
                else if (formatCode == 'k') {
                    obfuscated = true;
                } else if (formatCode == 'l') {
                    bold = true;
                } else if (formatCode == 'm') {
                    strikethrough = true;
                } else if (formatCode == 'n') {
                    underline = true;
                } else if (formatCode == 'o') {
                    italic = true;
                } else if (formatCode == 'r') {
                    baseColor = 0xFFFFFF;
                    bold = italic = underline = strikethrough = obfuscated = false;
                }

                i++;
            } else {
                currentSegment.append(c);
            }
        }

        if (!currentSegment.isEmpty()) {
            int flags = 0;
            if (bold) flags |= 1;
            if (italic) flags |= 2;
            if (underline) flags |= 4;
            if (strikethrough) flags |= 8;
            if (obfuscated) flags |= 16;

            drawFormattedString(fontRenderer, currentSegment.toString(), x, y, baseColor | (alpha << 24), flags);
        }
    }

    private float drawFormattedString(IFontRenderer fontRenderer, String text, float x, float y, int color, int flags) {
        boolean bold = (flags & 1) != 0;
        boolean italic = (flags & 2) != 0;
        boolean underline = (flags & 4) != 0;
        boolean strikethrough = (flags & 8) != 0;
        boolean obfuscated = (flags & 16) != 0;

        String renderText = text;
        if (obfuscated) {
            StringBuilder obfuscatedText = new StringBuilder();
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == ' ') {
                    obfuscatedText.append(' ');
                } else {
                    obfuscatedText.append(chars.charAt((int)(Math.random() * chars.length())));
                }
            }
            renderText = obfuscatedText.toString();
        }

        float width = fontRenderer.getStringWidth(renderText);

        if (bold && italic) {
            fontRenderer.drawStringWithShadow(renderText, x, y, color);
        } else if (bold) {
            fontRenderer.drawStringWithShadow(renderText, x, y, color);
        } else if (italic) {
            fontRenderer.drawStringWithShadow(renderText, x, y, color);
        } else {
            fontRenderer.drawStringWithShadow(renderText, x, y, color);
        }

        if (underline) {
            int underlineY = (int)(y + fontRenderer.getHeight() - 1);
            drawHorizontalLine((int) x, (int) (x + width), underlineY, color);
        }

        if (strikethrough) {
            int strikeY = (int)(y + fontRenderer.getHeight() / 2f);
            drawHorizontalLine((int) x, (int) (x + width), strikeY, color);
        }

        return width;
    }

    private void drawScoreboardValues(ScoreObjective p_175247_1_, int p_175247_2_, String p_175247_3_, int p_175247_4_, int p_175247_5_, NetworkPlayerInfo p_175247_6_, IFontRenderer fontRenderer)
    {
        int i = p_175247_1_.getScoreboard().getValueFromObjective(p_175247_3_, p_175247_1_).getScorePoints();

        if (p_175247_1_.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS)
        {
            this.mc.getTextureManager().bindTexture(icons);

            if (this.lastTimeOpened == p_175247_6_.func_178855_p())
            {
                if (i < p_175247_6_.func_178835_l())
                {
                    p_175247_6_.func_178846_a(Minecraft.getSystemTime());
                    p_175247_6_.func_178844_b((long)(this.guiIngame.getUpdateCounter() + 20));
                }
                else if (i > p_175247_6_.func_178835_l())
                {
                    p_175247_6_.func_178846_a(Minecraft.getSystemTime());
                    p_175247_6_.func_178844_b((long)(this.guiIngame.getUpdateCounter() + 10));
                }
            }

            if (Minecraft.getSystemTime() - p_175247_6_.func_178847_n() > 1000L || this.lastTimeOpened != p_175247_6_.func_178855_p())
            {
                p_175247_6_.func_178836_b(i);
                p_175247_6_.func_178857_c(i);
                p_175247_6_.func_178846_a(Minecraft.getSystemTime());
            }

            p_175247_6_.func_178843_c(this.lastTimeOpened);
            p_175247_6_.func_178836_b(i);
            int j = MathHelper.ceiling_float_int((float)Math.max(i, p_175247_6_.func_178860_m()) / 2.0F);
            int k = Math.max(MathHelper.ceiling_float_int((float)(i / 2)), Math.max(MathHelper.ceiling_float_int((float)(p_175247_6_.func_178860_m() / 2)), 10));
            boolean flag = p_175247_6_.func_178858_o() > (long)this.guiIngame.getUpdateCounter() && (p_175247_6_.func_178858_o() - (long)this.guiIngame.getUpdateCounter()) / 3L % 2L == 1L;

            if (j > 0)
            {
                float f = Math.min((float)(p_175247_5_ - p_175247_4_ - 4) / (float)k, 9.0F);

                if (f > 3.0F)
                {
                    for (int l = j; l < k; ++l)
                    {
                        this.drawTexturedModalRect((float)p_175247_4_ + (float)l * f, (float)p_175247_2_, flag ? 25 : 16, 0, 9, 9);
                    }

                    for (int j1 = 0; j1 < j; ++j1)
                    {
                        this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, flag ? 25 : 16, 0, 9, 9);

                        if (flag)
                        {
                            if (j1 * 2 + 1 < p_175247_6_.func_178860_m())
                            {
                                this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, 70, 0, 9, 9);
                            }

                            if (j1 * 2 + 1 == p_175247_6_.func_178860_m())
                            {
                                this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, 79, 0, 9, 9);
                            }
                        }

                        if (j1 * 2 + 1 < i)
                        {
                            this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, j1 >= 10 ? 160 : 52, 0, 9, 9);
                        }

                        if (j1 * 2 + 1 == i)
                        {
                            this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, j1 >= 10 ? 169 : 61, 0, 9, 9);
                        }
                    }
                }
                else
                {
                    float f1 = MathHelper.clamp((float)i / 20.0F, 0.0F, 1.0F);
                    int i1 = (int)((1.0F - f1) * 255.0F) << 16 | (int)(f1 * 255.0F) << 8;
                    String s = "" + (float)i / 2.0F;

                    if (p_175247_5_ - fontRenderer.getStringWidth(s + "hp") >= p_175247_4_)
                    {
                        s = s + "hp";
                    }

                    fontRenderer.drawStringWithShadow(s, (float)((p_175247_5_ + p_175247_4_) / 2f- fontRenderer.getStringWidth(s) / 2), (float)p_175247_2_, i1);
                }
            }
        }
        else
        {
            String s1 = EnumChatFormatting.YELLOW + "" + i;
            fontRenderer.drawStringWithShadow(s1, (float)(p_175247_5_ - fontRenderer.getStringWidth(s1)), (float)p_175247_2_, 16777215);
        }
    }

    private void drawScoreboardValues(ScoreObjective p_175247_1_, int p_175247_2_, String p_175247_3_, int p_175247_4_, int p_175247_5_, NetworkPlayerInfo p_175247_6_, FontRenderer fontRenderer)
    {
        int i = p_175247_1_.getScoreboard().getValueFromObjective(p_175247_3_, p_175247_1_).getScorePoints();

        if (p_175247_1_.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS)
        {
            this.mc.getTextureManager().bindTexture(icons);

            if (this.lastTimeOpened == p_175247_6_.func_178855_p())
            {
                if (i < p_175247_6_.func_178835_l())
                {
                    p_175247_6_.func_178846_a(Minecraft.getSystemTime());
                    p_175247_6_.func_178844_b((long)(this.guiIngame.getUpdateCounter() + 20));
                }
                else if (i > p_175247_6_.func_178835_l())
                {
                    p_175247_6_.func_178846_a(Minecraft.getSystemTime());
                    p_175247_6_.func_178844_b((long)(this.guiIngame.getUpdateCounter() + 10));
                }
            }

            if (Minecraft.getSystemTime() - p_175247_6_.func_178847_n() > 1000L || this.lastTimeOpened != p_175247_6_.func_178855_p())
            {
                p_175247_6_.func_178836_b(i);
                p_175247_6_.func_178857_c(i);
                p_175247_6_.func_178846_a(Minecraft.getSystemTime());
            }

            p_175247_6_.func_178843_c(this.lastTimeOpened);
            p_175247_6_.func_178836_b(i);
            int j = MathHelper.ceiling_float_int((float)Math.max(i, p_175247_6_.func_178860_m()) / 2.0F);
            int k = Math.max(MathHelper.ceiling_float_int((float)(i / 2)), Math.max(MathHelper.ceiling_float_int((float)(p_175247_6_.func_178860_m() / 2)), 10));
            boolean flag = p_175247_6_.func_178858_o() > (long)this.guiIngame.getUpdateCounter() && (p_175247_6_.func_178858_o() - (long)this.guiIngame.getUpdateCounter()) / 3L % 2L == 1L;

            if (j > 0)
            {
                float f = Math.min((float)(p_175247_5_ - p_175247_4_ - 4) / (float)k, 9.0F);

                if (f > 3.0F)
                {
                    for (int l = j; l < k; ++l)
                    {
                        this.drawTexturedModalRect((float)p_175247_4_ + (float)l * f, (float)p_175247_2_, flag ? 25 : 16, 0, 9, 9);
                    }

                    for (int j1 = 0; j1 < j; ++j1)
                    {
                        this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, flag ? 25 : 16, 0, 9, 9);

                        if (flag)
                        {
                            if (j1 * 2 + 1 < p_175247_6_.func_178860_m())
                            {
                                this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, 70, 0, 9, 9);
                            }

                            if (j1 * 2 + 1 == p_175247_6_.func_178860_m())
                            {
                                this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, 79, 0, 9, 9);
                            }
                        }

                        if (j1 * 2 + 1 < i)
                        {
                            this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, j1 >= 10 ? 160 : 52, 0, 9, 9);
                        }

                        if (j1 * 2 + 1 == i)
                        {
                            this.drawTexturedModalRect((float)p_175247_4_ + (float)j1 * f, (float)p_175247_2_, j1 >= 10 ? 169 : 61, 0, 9, 9);
                        }
                    }
                }
                else
                {
                    float f1 = MathHelper.clamp((float)i / 20.0F, 0.0F, 1.0F);
                    int i1 = (int)((1.0F - f1) * 255.0F) << 16 | (int)(f1 * 255.0F) << 8;
                    String s = "" + (float)i / 2.0F;

                    if (p_175247_5_ - fontRenderer.getStringWidth(s + "hp") >= p_175247_4_)
                    {
                        s = s + "hp";
                    }

                    fontRenderer.drawStringWithShadow(s, (float)((p_175247_5_ + p_175247_4_) / 2f- fontRenderer.getStringWidth(s) / 2), (float)p_175247_2_, i1);
                }
            }
        }
        else
        {
            String s1 = EnumChatFormatting.YELLOW + "" + i;
            fontRenderer.drawStringWithShadow(s1, (float)(p_175247_5_ - fontRenderer.getStringWidth(s1)), (float)p_175247_2_, 16777215);
        }
    }

    protected void drawPing(int p_175245_1_, int p_175245_2_, int p_175245_3_, NetworkPlayerInfo networkPlayerInfoIn) {

        int i = 0;
        int j = 0;
        Color color = Color.WHITE;


        if (networkPlayerInfoIn.getResponseTime() < 0) {
            j = 5;
            color = new Color(0, 255, 0);
        } else if (networkPlayerInfoIn.getResponseTime() < 150) {
            j = 0;
            color = new Color(85, 255, 85);
        } else if (networkPlayerInfoIn.getResponseTime() < 300) {
            j = 1;
            color = new Color(255, 255, 85);
        } else if (networkPlayerInfoIn.getResponseTime() < 600) {
            j = 2;
            color = new Color(255, 170, 0);
        } else if (networkPlayerInfoIn.getResponseTime() < 1000) {
            j = 3;
            color = new Color(255, 85, 85);
        } else {
            j = 4;
            color = new Color(170, 0, 0);
        }

        BetterTab betterTab = GothajClient.getInstance().getModuleManager().getAndCast(BetterTab.class);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (betterTab.isToggled() && betterTab.tabOpt.getOptionState("Custom Font")) {
            Fonts.icon13.drawString("w", p_175245_2_ + p_175245_1_ - 9, p_175245_3_ + Fonts.icon13.getHeight(), color.getRGB());
        } else {
            this.mc.getTextureManager().bindTexture(icons);
            this.zLevel += 100.0F;
            this.drawTexturedModalRect(p_175245_2_ + p_175245_1_ - 11, p_175245_3_, 0 + i * 10, 176 + j * 8, 10, 8);
            this.zLevel -= 100.0F;
        }
    }

    public void setFooter(IChatComponent footerIn)
    {
        this.footer = footerIn;
    }

    public void setHeader(IChatComponent headerIn)
    {
        this.header = headerIn;
    }

    public void resetFooterHeader()
    {
        this.header = null;
        this.footer = null;
    }

    static class PlayerComparator implements Comparator<NetworkPlayerInfo>
    {
        private PlayerComparator()
        {
        }

        public int compare(NetworkPlayerInfo p_compare_1_, NetworkPlayerInfo p_compare_2_)
        {
            ScorePlayerTeam scoreplayerteam = p_compare_1_.getPlayerTeam();
            ScorePlayerTeam scoreplayerteam1 = p_compare_2_.getPlayerTeam();
            return ComparisonChain.start().compareTrueFirst(p_compare_1_.getGameType() != WorldSettings.GameType.SPECTATOR, p_compare_2_.getGameType() != WorldSettings.GameType.SPECTATOR).compare(scoreplayerteam != null ? scoreplayerteam.getRegisteredName() : "", scoreplayerteam1 != null ? scoreplayerteam1.getRegisteredName() : "").compare(p_compare_1_.getGameProfile().getName(), p_compare_2_.getGameProfile().getName()).result();
        }
    }
}
