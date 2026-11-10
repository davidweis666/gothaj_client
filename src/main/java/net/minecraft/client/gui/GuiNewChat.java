package net.minecraft.client.gui;

import com.google.common.collect.Lists;

import java.awt.*;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.module.visual.BetterChat;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.impl.module.client.ShaderProcessing;
import team.gothaj.client.main.GothajClient;

public class GuiNewChat extends Gui
{
    private static final Logger logger = LogManager.getLogger();
    private final Minecraft mc;
    private final List<String> sentMessages = Lists.<String>newArrayList();
    private final List<ChatLine> chatLines = Lists.<ChatLine>newArrayList();
    private final List<ChatLine> drawnChatLines = Lists.<ChatLine>newArrayList();
    private int scrollPos;
    private boolean isScrolled;

    private String lastRawMessage = null;
    private int lastCount = 0;
    private int lastLineId = 0;

    public GuiNewChat(Minecraft mcIn)
    {
        this.mc = mcIn;
    }

    public void drawChat(int updateCounter) {
        if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {

            BetterChat betterChat = GothajClient.getInstance().getModuleManager().getAndCast(BetterChat.class);
            ShaderProcessing shaderProcessing = GothajClient.getInstance().getModuleManager().getAndCast(ShaderProcessing.class);

            int i = this.getLineCount();
            boolean flag = false;
            int j = 0;
            int k = this.drawnChatLines.size();
            float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;

            if (k > 0) {
                if (this.getChatOpen()) {
                    flag = true;
                }

                float f1 = this.getChatScale();
                int l = MathHelper.ceiling_float_int((float) this.getChatWidth() / f1);
                GlStateManager.pushMatrix();
                GlStateManager.translate(2.0F, 20.0F, 0.0F);
                GlStateManager.scale(f1, f1, 1.0F);

                int visibleLines = 0;
                for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = (ChatLine) this.drawnChatLines.get(i1 + this.scrollPos);
                    if (chatline != null) {
                        int j1 = updateCounter - chatline.getUpdatedCounter();
                        if (j1 < 200 || flag) {
                            double d0 = (double) j1 / 200.0D;
                            d0 = 1.0D - d0;
                            d0 = d0 * 10.0D;
                            d0 = MathHelper.clamp(d0, 0.0D, 1.0D);
                            d0 = d0 * d0;
                            int l1 = (int) (255.0D * d0);

                            if (flag) {
                                l1 = 255;
                            }

                            l1 = (int) ((float) l1 * f);

                            if (l1 > 3) {
                                visibleLines++;
                            }
                        }
                    }
                }

                if (visibleLines > 0 && betterChat.isToggled() && betterChat.chatOpt.getOptionState("Rounded")) {
                    if (!shaderProcessing.isToggled() || !shaderProcessing.blur.getProperty() || shaderProcessing.blurBackground.getProperty()) {
                        int chatHeight = visibleLines * 9;
                        float topY = -(visibleLines - 1) * 9 - 10.5f;
                        RoundedUtil.drawRound(0, topY, l + 4, chatHeight + 0.5f, 6f, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
                    }
                } else if (visibleLines > 0 && (!betterChat.isToggled() || !betterChat.chatOpt.getOptionState("Rounded"))) {
                    j = 0;
                    for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1) {
                        ChatLine chatline = (ChatLine) this.drawnChatLines.get(i1 + this.scrollPos);
                        if (chatline != null) {
                            int j1 = updateCounter - chatline.getUpdatedCounter();
                            if (j1 < 200 || flag) {
                                double d0 = (double) j1 / 200.0D;
                                d0 = 1.0D - d0;
                                d0 = d0 * 10.0D;
                                d0 = MathHelper.clamp(d0, 0.0D, 1.0D);
                                d0 = d0 * d0;
                                int l1 = (int) (255.0D * d0);

                                if (flag) {
                                    l1 = 255;
                                }

                                l1 = (int) ((float) l1 * f);
                                ++j;

                                if (l1 > 3) {
                                    int i2 = 0;
                                    int j2 = -i1 * 9;
                                    drawRect(i2, j2 - 9, i2 + l + 4, j2, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()).getRGB());
                                }
                            }
                        }
                    }
                }

                j = 0;
                for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = (ChatLine) this.drawnChatLines.get(i1 + this.scrollPos);

                    if (chatline != null) {
                        int j1 = updateCounter - chatline.getUpdatedCounter();

                        if (j1 < 200 || flag) {
                            double d0 = (double) j1 / 200.0D;
                            d0 = 1.0D - d0;
                            d0 = d0 * 10.0D;
                            d0 = MathHelper.clamp(d0, 0.0D, 1.0D);
                            d0 = d0 * d0;
                            int l1 = (int) (255.0D * d0);

                            if (flag) {
                                l1 = 255;
                            }

                            l1 = (int) ((float) l1 * f);
                            ++j;

                            if (l1 > 3) {
                                int i2 = 2;
                                int j2 = -i1 * 9;
                                GlStateManager.enableBlend();
                                String s = chatline.getChatComponent().getFormattedText();

                                if (!betterChat.isToggled() || !betterChat.chatOpt.getOptionState("Custom Font")) {
                                    this.mc.fontRendererObj.drawStringWithShadow(s, (float) i2, (float) (j2 - 8), 16777215 + (l1 << 24));
                                } else {
                                    drawChatLineWithAlpha(s, (float) i2, (float) (j2 - 9), l1);
                                }

                                GlStateManager.disableBlend();
                                GlStateManager.disableAlpha();
                            }
                        }
                    }
                }

                if (flag) {
                    int k2 = !betterChat.isToggled() || !betterChat.chatOpt.getOptionState("Custom Font") ? this.mc.fontRendererObj.FONT_HEIGHT : Fonts.nunito18.getHeight();
                    GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                    int l2 = k * k2 + k;
                    int i3 = j * k2 + j;
                    int j3 = this.scrollPos * i3 / k;
                    int k1 = i3 * i3 / l2;

                    if (l2 != i3) {
                        int k3 = j3 > 0 ? 170 : 96;
                        int l3 = this.isScrolled ? 13382451 : 3355562;
                        drawRect(0, -j3, 2, -j3 - k1, l3 + (k3 << 24));
                        drawRect(2, -j3, 1, -j3 - k1, 13421772 + (k3 << 24));
                    }
                }

                GlStateManager.popMatrix();
            }
        }
    }

    private void drawChatLineWithAlpha(String text, float x, float y, int alpha) {
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

                    float width = drawFormattedString(currentSegment.toString(), x, y, baseColor | (alpha << 24), flags);
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
                } else if (formatCode == 'k') {
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

            drawFormattedString(currentSegment.toString(), x, y, baseColor | (alpha << 24), flags);
        }
    }

    private float drawFormattedString(String text, float x, float y, int color, int flags) {
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

        float width = Fonts.nunito18.getStringWidth(renderText);

        Fonts.nunito18.drawStringWithShadow(renderText, x, y, color);

        if (underline) {
            int underlineY = (int)(y + Fonts.nunito18.getHeight() - 1);
            Gui.drawRect((int)x, underlineY, (int)(x + width), underlineY + 1, color);
        }

        if (strikethrough) {
            int strikeY = (int)(y + Fonts.nunito18.getHeight() / 2f);
            Gui.drawRect((int)x, strikeY, (int)(x + width), strikeY + 1, color);
        }

        return width;
    }


    public void drawChatBox(int updateCounter)
    {
        if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN)
        {
            BetterChat betterChat = GothajClient.getInstance().getModuleManager().getAndCast(BetterChat.class);
            int i = this.getLineCount();
            boolean flag = false;
            int k = this.drawnChatLines.size();
            float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;

            if (k > 0)
            {
                if (this.getChatOpen())
                {
                    flag = true;
                }

                float f1 = this.getChatScale();
                int l = MathHelper.ceiling_float_int((float)this.getChatWidth() / f1);
                GlStateManager.pushMatrix();
                GlStateManager.translate(2.0F, 20.0F, 0.0F);
                GlStateManager.scale(f1, f1, 1.0F);

                int visibleLines = 0;
                for (int i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1)
                {
                    ChatLine chatline = (ChatLine)this.drawnChatLines.get(i1 + this.scrollPos);
                    if (chatline != null)
                    {
                        int j1 = updateCounter - chatline.getUpdatedCounter();
                        if (j1 < 200 || flag)
                        {
                            double d0 = (double)j1 / 200.0D;
                            d0 = 1.0D - d0;
                            d0 = d0 * 10.0D;
                            d0 = MathHelper.clamp(d0, 0.0D, 1.0D);
                            d0 = d0 * d0;
                            int l1 = (int)(255.0D * d0);

                            if (flag)
                            {
                                l1 = 255;
                            }

                            l1 = (int)((float)l1 * f);

                            if (l1 > 3)
                            {
                                visibleLines++;
                            }
                        }
                    }
                }

                if (visibleLines > 0)
                {
                    int chatHeight = visibleLines * 9;
                    float topY = -(visibleLines - 1) * 9 - 10.5f;

                    if (betterChat.isToggled() && betterChat.chatOpt.getOptionState("Rounded"))
                    {
                        RoundedUtil.drawShaderRound(0, topY, l + 4, chatHeight + 0.5F, 6f, Color.BLACK);
                    }
                    else
                    {
                        drawRect(0, topY, l + 4, topY + chatHeight, Color.BLACK.getRGB());
                    }
                }

                GlStateManager.popMatrix();
            }

            if (this.getChatOpen()) {
                if (betterChat.isToggled() && betterChat.chatOpt.getOptionState("Rounded")) {
                  //  RoundedUtil.drawShaderRound(2, mc.displayHeight - 14, mc.displayWidth - 4, 12, 6f, Color.BLACK);
                } else {
                   // drawRect(2, mc.displayHeight - 14, mc.displayWidth - 2, mc.displayHeight - 2, Color.BLACK.getRGB());
                }
            }
        }
    }


    public void clearChatMessages()
    {
        this.drawnChatLines.clear();
        this.chatLines.clear();
        this.sentMessages.clear();
        this.lastRawMessage = null;
        this.lastCount = 0;
        this.lastLineId = 0;
    }

    public void printChatMessage(IChatComponent chatComponent)
    {
        this.printChatMessageWithOptionalDeletion(chatComponent, 0);
    }

    public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId)
    {
        String raw = chatComponent.getUnformattedText();

        boolean sameAsLast = lastRawMessage != null && lastRawMessage.equals(raw);

        if (sameAsLast)
        {
            lastCount++;
            IChatComponent merged = chatComponent.createCopy();
            merged.appendSibling(new ChatComponentText(" §7(" + lastCount + "x)"));
            setChatLine(merged, lastLineId, this.mc.ingameGUI.getUpdateCounter(), false);
            logger.info("[CHAT] " + merged.getUnformattedText());
        }
        else
        {
            lastRawMessage = raw;
            lastCount = 1;
            lastLineId = newUniqueLineId();
            setChatLine(chatComponent, lastLineId, this.mc.ingameGUI.getUpdateCounter(), false);
            logger.info("[CHAT] " + chatComponent.getUnformattedText());
        }
    }

    private int newUniqueLineId()
    {
        return (int)(System.nanoTime() & 0x7FFFFFFF);
    }

    private void setChatLine(IChatComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly)
    {
        if (chatLineId != 0)
        {
            this.deleteChatLine(chatLineId);
        }

        int i = MathHelper.floor_float((float)this.getChatWidth() / this.getChatScale());
        List<IChatComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i, this.mc.fontRendererObj, false, false);
        boolean flag = this.getChatOpen();

        for (IChatComponent ichatcomponent : list)
        {
            if (flag && this.scrollPos > 0)
            {
                this.isScrolled = true;
                this.scroll(1);
            }

            this.drawnChatLines.addFirst(new ChatLine(updateCounter, ichatcomponent, chatLineId));
        }

        while (this.drawnChatLines.size() > 100)
        {
            this.drawnChatLines.removeLast();
        }

        if (!displayOnly)
        {
            this.chatLines.addFirst(new ChatLine(updateCounter, chatComponent, chatLineId));

            while (this.chatLines.size() > 100)
            {
                this.chatLines.removeLast();
            }
        }
    }

    public void refreshChat()
    {
        this.drawnChatLines.clear();
        this.resetScroll();

        for (int i = this.chatLines.size() - 1; i >= 0; --i)
        {
            ChatLine chatline = (ChatLine)this.chatLines.get(i);
            this.setChatLine(chatline.getChatComponent(), chatline.getChatLineID(), chatline.getUpdatedCounter(), true);
        }
    }

    public List<String> getSentMessages()
    {
        return this.sentMessages;
    }

    public void addToSentMessages(String message)
    {
        if (this.sentMessages.isEmpty() || !((String)this.sentMessages.get(this.sentMessages.size() - 1)).equals(message))
        {
            this.sentMessages.add(message);
        }
    }

    public void resetScroll()
    {
        this.scrollPos = 0;
        this.isScrolled = false;
    }

    public void scroll(int amount)
    {
        this.scrollPos += amount;
        int i = this.drawnChatLines.size();

        if (this.scrollPos > i - this.getLineCount())
        {
            this.scrollPos = i - this.getLineCount();
        }

        if (this.scrollPos <= 0)
        {
            this.scrollPos = 0;
            this.isScrolled = false;
        }
    }

    public IChatComponent getChatComponent(int mouseX, int mouseY)
    {
        if (!this.getChatOpen())
        {
            return null;
        }
        else
        {
            ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int i = scaledresolution.getScaleFactor();
            float f = this.getChatScale();
            int j = mouseX / i - 3;
            int k = mouseY / i - 27;
            j = MathHelper.floor_float((float)j / f);
            k = MathHelper.floor_float((float)k / f);

            if (j >= 0 && k >= 0)
            {
                int l = Math.min(this.getLineCount(), this.drawnChatLines.size());

                if (j <= MathHelper.floor_float((float)this.getChatWidth() / this.getChatScale()) && k < this.mc.fontRendererObj.FONT_HEIGHT * l + l)
                {
                    int i1 = k / this.mc.fontRendererObj.FONT_HEIGHT + this.scrollPos;

                    if (i1 >= 0 && i1 < this.drawnChatLines.size())
                    {
                        ChatLine chatline = (ChatLine)this.drawnChatLines.get(i1);
                        int j1 = 0;

                        for (IChatComponent ichatcomponent : chatline.getChatComponent())
                        {
                            if (ichatcomponent instanceof ChatComponentText)
                            {
                                j1 += this.mc.fontRendererObj.getStringWidth(GuiUtilRenderComponents.func_178909_a(((ChatComponentText)ichatcomponent).getChatComponentText_TextValue(), false));

                                if (j1 > j)
                                {
                                    return ichatcomponent;
                                }
                            }
                        }
                    }

                    return null;
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
    }

    public boolean getChatOpen()
    {
        return this.mc.currentScreen instanceof GuiChat;
    }

    public void deleteChatLine(int id) {

        try {
        synchronized (this.drawnChatLines) {
            this.drawnChatLines.removeIf(chatline -> chatline.getChatLineID() == id);
        }

        synchronized (this.chatLines) {
            Iterator<ChatLine> iterator = this.chatLines.iterator();
            while (iterator.hasNext()) {
                ChatLine chatline1 = iterator.next();
                if (chatline1.getChatLineID() == id) {
                    iterator.remove();
                    break;
                }
            }
        }
        } catch (ConcurrentModificationException e) {
        }
    }


        public int getChatWidth()
    {
        return calculateChatboxWidth(this.mc.gameSettings.chatWidth);
    }

    public int getChatHeight()
    {
        return calculateChatboxHeight(this.getChatOpen() ? this.mc.gameSettings.chatHeightFocused : this.mc.gameSettings.chatHeightUnfocused);
    }

    public float getChatScale()
    {
        return this.mc.gameSettings.chatScale;
    }

    public static int calculateChatboxWidth(float scale)
    {
        int i = 320;
        int j = 40;
        return MathHelper.floor_float(scale * (float)(i - j) + (float)j);
    }

    public static int calculateChatboxHeight(float scale)
    {
        int i = 180;
        int j = 20;
        return MathHelper.floor_float(scale * (float)(i - j) + (float)j);
    }

    public int getLineCount()
    {
        return this.getChatHeight() / 9;
    }
}
