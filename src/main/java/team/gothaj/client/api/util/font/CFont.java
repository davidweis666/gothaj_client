package team.gothaj.client.api.util.font;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.apache.commons.lang3.tuple.MutablePair;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.render.GLUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.module.misc.AntiCrash;
import team.gothaj.client.impl.module.visual.NameProtect;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class CFont implements IFontRenderer {
    private static int[] colorCode;
    private static final String colorcodeIdentifiers = "0123456789abcdefklmnor";
    private final Font font;
    @Setter
    @Getter
    private CFont boldFont;
    private final FontData regular = new FontData(Font.PLAIN), italic = new FontData(Font.ITALIC);

    private int fontHeight;
    private static final float KERNING = 8.2f;

    private boolean isDrawingGradient = false;

    @Getter
    @Setter
    private boolean hudFont = false;

    private int currentTexture = -1;

    private NameProtect nameProtectModule;
    private AntiCrash antiCrashModule;

    public CFont(Font font) {
        generateColorCodes();
        this.font = font;
        setupTexture(regular);
        setupTexture(italic);
    }

    private void setupTexture(FontData fontData) {
        BufferedImage fakeImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) fakeImage.getGraphics();

        Font currentFont = fontData.textType == 0 ? font : font.deriveFont(fontData.textType);
        graphics.setFont(currentFont);
        handleSprites(fontData, currentFont, graphics);
    }

    private NameProtect getNameProtect() {
        if (nameProtectModule == null) {
            nameProtectModule = GothajClient.getInstance().getModuleManager().getAndCast(NameProtect.class);
        }
        return nameProtectModule;
    }

    private AntiCrash getAntiCrash() {
        if (antiCrashModule == null) {
            antiCrashModule = GothajClient.getInstance().getModuleManager().getAndCast(AntiCrash.class);
        }
        return antiCrashModule;
    }

    public String processString(String text) {
        if (text == null || text.isEmpty()) return text;

        if (Utils.mc.thePlayer == null || Utils.mc.theWorld == null) return text;

        AntiCrash ac = getAntiCrash();
        if (ac != null && ac.isToggled()) {
            if (text.contains("Your PC is DEAD") || text.contains("CHEATER!") || text.contains("§r§a§k+")) {
                text = text.replace("CHEATER!", "")
                        .replace("Your PC is DEAD", "")
                        .replace("§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r", "");
            }
        }

        NameProtect np = getNameProtect();
        if (np != null && np.isToggled()) {
            String localName = Utils.mc.thePlayer.getName();

            if (np.targets.getOptionState("Yourself") && text.contains(localName) && !isModuleName(text)) {
                switch (np.ownMode.getProperty()) {
                    case "IRC" -> text = text.replace(localName,
                            GothajClient.getInstance().getCurrentPlayer().getRank().getChatColor()
                                    + GothajClient.getInstance().getUser().getUsername());
                    case "Custom" -> {
                        String raw = np.customSelf.getProperty();
                        String customName = raw.isEmpty() ? "You" : "§r" + translateAlternateColorCodes(raw);
                        text = text.replace(localName, customName);
                    }
                }
            }


            if (np.targets.getOptionState("Everyone") && !Utils.mc.thePlayer.sendQueue.getPlayerInfoMap().isEmpty()) {
                for (NetworkPlayerInfo player : Utils.mc.thePlayer.sendQueue.getPlayerInfoMap()) {
                    String pName = player.getGameProfile().getName();
                    if (!pName.equalsIgnoreCase(localName) && text.contains(pName) && !isModuleName(text)) {
                        String raw = np.customEveryone.getProperty();
                        String replacement = raw.isEmpty() ? "Protected" : "§r" + translateAlternateColorCodes(raw);
                        text = text.replace(pName, replacement);
                    }
                }
            }
        }
        return text;
    }

    private String translateAlternateColorCodes(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&' && i + 1 < text.length()) {
                char c = text.charAt(i + 1);
                if ("0123456789abcdefklmnor".indexOf(Character.toLowerCase(c)) != -1) {
                    sb.append('§').append(c);
                    i++;
                    continue;
                }
            }
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private boolean isModuleName(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            for (Module module : GothajClient.getInstance().getModuleManager().getItems()) {
                if (module.getName().equalsIgnoreCase(text)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    public void drawSmoothString(String text, double x2, float y2, int color) {
        this.drawString(text, x2, y2, color, false, KERNING, true);
    }

    public void drawSmoothStringWithShadow(String text, double x2, float y2, int color) {
        this.drawString(text, x2 + 0.5f, y2 + 0.5f, color, true, KERNING, true);
        this.drawString(text, x2, y2, color, false, KERNING, true);
    }

    @Override
    public int drawCenteredString(String name, float x, float y, int color) {
        String finalName = processString(name);
        return (int) drawStringInternal(finalName, x - (getStringWidthRaw(finalName, KERNING) / 2), y, color, false, KERNING, false);
    }

    @Override
    public void drawCenteredString(String name, float x, float y, Color color) {
        drawCenteredString(name, x, y, color.getRGB());
    }

    @Override
    public void drawCenteredStringWithShadow(String text, float x, float y, int color) {
        this.drawStringWithShadow(text, x - (this.getStringWidth(text) / 2), y, color);
    }

    @Override
    public int drawStringWithShadow(String name, float x, float y, int color) {
        String processed = processString(name);
        drawStringInternal(processed, x + .5f, y + .5f, color, true, KERNING, false);
        return (int) drawStringInternal(processed, x, y, color, false, KERNING, false);
    }

    @Override
    public void drawStringWithShadow(String name, float x, float y, Color color) {
        drawStringWithShadow(name, x, y, color.getRGB());
    }

    @Override
    public int drawString(String text, float x, float y, int color, boolean shadow) {
        if (shadow) return this.drawStringWithShadow(text, x, y, color);
        return (int) this.drawString(text, x, y, color, false, KERNING, false);
    }

    @Override
    public int drawString(String name, float x, float y, int color) {
        return drawString(name, x, y, color, false);
    }

    @Override
    public void drawString(String name, float x, float y, Color color) {
        drawString(name, x, y, color.getRGB(), false);
    }

    public float drawString(String text, double x, double y, int color, boolean shadow, float kerning, boolean smooth) {
        return drawStringInternal(processString(text), x, y, color, shadow, kerning, smooth);
    }

    private float drawStringInternal(String text, double x, double y, int color, boolean shadow, float kerning, boolean smooth) {
        if (text == null) {
            return 0;
        }

        boolean useGradient = false;
        String customName = null;
        NameProtect np = getNameProtect();

        if (!isDrawingGradient && np != null && np.isToggled() && np.targets.getOptionState("Yourself") && np.ownMode.is("Custom")) {
            customName = "§r" + translateAlternateColorCodes(np.customSelf.getProperty().isEmpty() ? "You" : np.customSelf.getProperty());
            if (text.contains(customName) && !isModuleName(text)) {
                useGradient = true;
            }
        }

        if (useGradient) {
            int nameIndex = text.indexOf(customName);
            if (nameIndex >= 0) {
                String before = text.substring(0, nameIndex);
                String after = text.substring(nameIndex + customName.length());

                float currentX = (float) x;

                isDrawingGradient = true;

                if (!before.isEmpty()) {
                    currentX = drawStringInternal(before, currentX, y, color, shadow, kerning, smooth);
                }

                FontUtil.drawHorizontalGradient(this, customName, currentX, (float) y, 5);
                currentX += (float) getStringWidthRaw(customName, kerning);

                if (!after.isEmpty()) {
                    drawStringInternal(after, currentX, y, color, shadow, kerning, smooth);
                }

                isDrawingGradient = false;

                return (int) (x + getStringWidthRaw(text, kerning));
            }
        }

        if (shadow) {
            color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(.5, .5, .5);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.color(color);
        GlStateManager.enableTexture2D();

        this.currentTexture = this.regular.texture.getGlTextureId();
        GlStateManager.bindTexture(this.currentTexture);

        if (smooth) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        float returnVal = drawCustomChars(text, x, y, kerning, color, shadow);

        this.currentTexture = -1;

        glHint(GL_POLYGON_SMOOTH_HINT, GL_DONT_CARE);
        glPopMatrix();
        glColor4f(1, 1, 1, 1);
        GlStateManager.resetColor();
        GlStateManager.bindTexture(0);
        return returnVal;
    }

    private float drawCustomChars(String text, double x, double y, float kerning, int color, boolean shadow) {
        x = (x - 1) * 2;
        y = (y - 3) * 2;
        FontData currentData = this.regular;
        float alpha = (float) (color >> 24 & 255) / 255f;

        boolean bold = false, italic = false, strikethrough = false, underline = false;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (character == '§') {
                int colorIndex = 21;

                try {
                    colorIndex = colorcodeIdentifiers.indexOf(text.charAt(index + 1));
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }

                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;

                    int newTexture = this.regular.texture.getGlTextureId();
                    if (this.currentTexture != newTexture) {
                        GlStateManager.bindTexture(newTexture);
                        this.currentTexture = newTexture;
                    }

                    currentData = this.regular;

                    if (colorIndex < 0) {
                        colorIndex = 15;
                    }

                    if (shadow) {
                        colorIndex += 16;
                    }

                    RenderUtil.color(colorCode[colorIndex], alpha);
                } else {
                    switch (colorIndex) {
                        case 17:
                            if (hasBoldFont()) {
                                bold = true;
                                int newTexture;
                                if (italic) {
                                    newTexture = this.boldFont.italic.texture.getGlTextureId();
                                    currentData = this.boldFont.italic;
                                } else {
                                    newTexture = this.boldFont.regular.texture.getGlTextureId();
                                    currentData = this.boldFont.regular;
                                }

                                if (this.currentTexture != newTexture) {
                                    GlStateManager.bindTexture(newTexture);
                                    this.currentTexture = newTexture;
                                }
                            }
                            break;
                        case 18:
                            strikethrough = true;
                            break;
                        case 19:
                            underline = true;
                            break;
                        case 20:
                            italic = true;
                            int newTexture;
                            if (bold && hasBoldFont()) {
                                newTexture = this.boldFont.italic.texture.getGlTextureId();
                                currentData = this.boldFont.italic;
                            } else {
                                newTexture = this.italic.texture.getGlTextureId();
                                currentData = this.italic;
                            }

                            if (this.currentTexture != newTexture) {
                                GlStateManager.bindTexture(newTexture);
                                this.currentTexture = newTexture;
                            }
                            break;
                        default:
                            bold = false;
                            italic = false;
                            underline = false;
                            strikethrough = false;
                            RenderUtil.color(color);

                            newTexture = this.regular.texture.getGlTextureId();
                            if (this.currentTexture != newTexture) {
                                GlStateManager.bindTexture(newTexture);
                                this.currentTexture = newTexture;
                            }
                            currentData = this.regular;
                            break;
                    }
                }

                ++index;
            } else if (character < currentData.chars.length) {

                drawLetter(x, y, currentData, strikethrough, underline, character);
                x += MathUtil.roundToHalf(currentData.chars[character].width - KERNING);
            }
        }
        return (float) (x / 2);
    }

    public void drawLetter(double x, double y, FontData currentData, boolean strikethrough, boolean underline, char character) {
        CharData charData = currentData.chars[character];

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        drawQuadBatched(worldrenderer, (float) x, (float) y, charData.width, charData.height, charData.storedX, charData.storedY, currentData.imageSize.getLeft(), currentData.imageSize.getRight());
        tessellator.draw();

        if (strikethrough) {
            this.drawLine(x, y + (double) (charData.height / 2), x + (double) charData.width - 8, y + (double) (charData.height / 2));
        }
        if (underline) {
            this.drawLine(x + 2.5f, y + (double) charData.height - 1, x + charData.width - 6, y + (double) charData.height - 1);
        }
    }

    protected void drawQuadBatched(WorldRenderer worldrenderer, float x2, float y2, float width, float height, float srcX, float srcY, float imgWidth, float imgHeight) {
        float renderSRCX = srcX / imgWidth;
        float renderSRCY = srcY / imgHeight;
        float renderSRCWidth = width / imgWidth;
        float renderSRCHeight = height / imgHeight;

        worldrenderer.pos(x2, y2, 0.0D).tex(renderSRCX, renderSRCY).endVertex();
        worldrenderer.pos(x2, y2 + height, 0.0D).tex(renderSRCX, renderSRCY + renderSRCHeight).endVertex();
        worldrenderer.pos(x2 + width, y2 + height, 0.0D).tex(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight).endVertex();
        worldrenderer.pos(x2 + width, y2, 0.0D).tex(renderSRCX + renderSRCWidth, renderSRCY).endVertex();
    }

    @Override
    public float getMiddleOfBox(float height) {
        return height / 2f - getHeight() / 2f;
    }

    @Override
    public String trimStringToWidth(String text, int width) {
        return this.trimStringToWidth(text, width, false);
    }

    @Override
    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (text == null) return "";
        StringBuilder buffer = new StringBuilder();
        float lineWidth = 0.0F;
        int offset = reverse ? text.length() - 1 : 0;
        int increment = reverse ? -1 : 1;
        boolean var8 = false;
        boolean var9 = false;

        for (int index = offset; index >= 0 && index < text.length() && lineWidth < (float) width; index += increment) {
            char character = text.charAt(index);
            float charWidth = this.getCharWidthFloat(character);

            if (var8) {
                var8 = false;

                if (character != 108 && character != 76) {
                    if (character == 114 || character == 82) {
                        var9 = false;
                    }
                } else {
                    var9 = true;
                }
            } else if (charWidth < 0) {
                var8 = true;
            } else {
                lineWidth += charWidth;

                if (var9) {
                    ++lineWidth;
                }
            }

            if (lineWidth > (float) width) {
                break;
            }

            if (reverse) {
                buffer.insert(0, character);
            } else {
                buffer.append(character);
            }
        }

        return buffer.toString();
    }

    @Override
    public float getCharWidthFloat(char c) {
        if (c == 167) {
            return -1;
        } else if (c == 32) {
            return 2;
        } else {
            int var2 = ("\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000").indexOf(c);

            if (var2 != -1 && c > 0) {
                return (regular.chars[var2].width - KERNING) / 2.f;
            } else if (c < regular.chars.length && regular.chars[c].width > 0) {
                return (regular.chars[c].width - KERNING) / 2.f;
            } else {
                return 0;
            }
        }
    }

    @Override
    public int getHeight() {
        return (this.fontHeight - 8) / 2;
    }

    @Override
    public float getStringWidth(String text) {
        return (float) getStringWidthRaw(processString(text), KERNING);
    }

    public double getStringWidth(String text, float kerning) {
        return getStringWidthRaw(processString(text), kerning);
    }

    private double getStringWidthRaw(String text, float kerning) {
        if (text == null) {
            return 0;
        }

        float width = 0;
        CharData[] currentData = regular.chars;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (character == '§') {
                int colorIndex = colorcodeIdentifiers.indexOf(text.charAt(index + 1));
                switch (colorIndex) {
                    case 17:
                        if (hasBoldFont()) {
                            currentData = this.boldFont.regular.chars;
                        }
                        break;
                    case 20:
                        currentData = this.regular.chars;
                        break;
                    default:
                        currentData = regular.chars;
                        break;
                }
                ++index;
            } else if (character < currentData.length) {
                width += currentData[character].width - kerning;
            }
        }

        return width / 2;
    }

    public boolean hasBoldFont() {
        return this.boldFont != null;
    }

    private final List<String> lines = new ArrayList<>();

    public List<String> getWrappedLines(String text, float x, float width, float heightIncrement) {
        wrapTextToLines(text, x, width);
        return lines;
    }

    public float drawWrappedText(String text, float x, float y, int color, float width, float heightIncrement) {
        wrapTextToLines(text, x, width);

        float newY = y;
        for (String s : lines) {
            RenderUtil.resetColor();
            drawString(s, x, newY, color);
            newY += getHeight() + heightIncrement;
        }
        return newY - y;
    }

    public MutablePair<Float, Float> drawNewLineText(String text, float x, float y, int color, float heightIncrement) {
        wrapTextToNewLine(text, x);

        String longest = "";
        float newY = y;
        for (String s : lines) {
            if (getStringWidth(s) > getStringWidth(longest)) {
                longest = s;
            }
            RenderUtil.resetColor();
            drawString(s, x, newY, color);
            newY += getHeight() + heightIncrement;
        }

        return MutablePair.of(getStringWidth(longest), newY - y);
    }

    private void wrapTextToNewLine(String text, float x) {
        lines.clear();
        lines.addAll(Arrays.asList(text.trim().split("\n")));
    }

    private void wrapTextToLines(String text, float x, float width) {
        lines.clear();
        String[] words = text.trim().split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            float totalWidth = getStringWidth(line + " " + word);

            if (x + totalWidth >= x + width) {
                lines.add(line.toString());
                line = new StringBuilder(word).append(" ");
                continue;
            }

            line.append(word).append(" ");
        }
        lines.add(line.toString());
    }

    private void drawLine(double x2, double y2, double x1, double y1) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth((float) 1);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x2, y2);
        GL11.glVertex2d(x1, y1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }


    private static class FontData {
        private final CharData[] chars = new CharData[256];
        private final int textType;
        private DynamicTexture texture;
        private final MutablePair<Integer, Integer> imageSize = MutablePair.of(512, 0);

        public FontData(int textType) {
            this.textType = textType;
        }
    }

    private static class CharData {
        private float width;
        private int height, storedX, storedY;
    }

    private void generateColorCodes() {
        if (colorCode == null) {
            colorCode = new int[32];
            for (int i = 0; i < 32; ++i) {
                final int noClue = (i >> 3 & 0x1) * 85;
                int red = (i >> 2 & 0x1) * 170 + noClue;
                int green = (i >> 1 & 0x1) * 170 + noClue;
                int blue = (i & 0x1) * 170 + noClue;
                if (i == 6) {
                    red += 85;
                }
                if (i >= 16) {
                    red /= 4;
                    green /= 4;
                    blue /= 4;
                }
                colorCode[i] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF));
            }
        }
    }


    private void handleSprites(FontData fontData, Font currentFont, Graphics2D graphics2D) {
        handleSprites(fontData, currentFont, graphics2D, false);
    }

    private void handleSprites(FontData fontData, Font currentFont, Graphics2D graphics2D, boolean drawString) {
        int charHeight = 0;
        int positionX = 0;
        int positionY = 1;
        int index = 0;
        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        if (drawString) {
            BufferedImage image = new BufferedImage(fontData.imageSize.getLeft(), fontData.imageSize.getRight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D) image.getGraphics();

            graphics.setFont(currentFont);
            graphics.setColor(new Color(255, 255, 255, 0));
            graphics.fillRect(0, 0, fontData.imageSize.getLeft(), fontData.imageSize.getRight());
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (CharData data : fontData.chars) {
                char c = (char) index;
                graphics.drawString(String.valueOf(c), data.storedX + 2, data.storedY + fontMetrics.getAscent());
                index++;
            }

            fontData.texture = new DynamicTexture(image);
        } else {
            while (index < fontData.chars.length) {
                char c = (char) index;
                CharData charData = new CharData();
                Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(c), graphics2D);
                charData.width = dimensions.getBounds().width + KERNING;
                charData.height = dimensions.getBounds().height;

                if (positionX + charData.width >= fontData.imageSize.getLeft()) {
                    positionX = 0;
                    positionY += charHeight;
                    charHeight = 0;
                }

                if (charData.height > charHeight) {
                    charHeight = charData.height;
                }

                charData.storedX = positionX;
                charData.storedY = positionY;

                if (charData.height > this.fontHeight) {
                    this.fontHeight = charData.height;
                }

                fontData.chars[index] = charData;
                positionX += charData.width;
                fontData.imageSize.setRight(positionY + fontMetrics.getAscent());
                ++index;
            }

            handleSprites(fontData, currentFont, graphics2D, true);
        }

    }


}
