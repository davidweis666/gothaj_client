package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import systems.vajzii.irc.players.IrcPlayer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.CustomColors;
import net.optifine.render.GlBlendState;
import net.optifine.util.FontUtils;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.impl.module.misc.AntiCrash;
import team.gothaj.client.impl.module.visual.NameProtect;
import team.gothaj.client.main.GothajClient;

public class FontRenderer implements IResourceManagerReloadListener {
    private static final ResourceLocation[] unicodePageLocations = new ResourceLocation[256];
    private static final String LEGACY_CHARS =
            "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 " +
                    "!\"#$%&'()*+,-./0123456789:;<=>?@" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
                    "abcdefghijklmnopqrstuvwxyz{|}~\u0000" +
                    "\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192" +
                    "\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb" +
                    "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c" +
                    "\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580" +
                    "\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

    private final int[] charWidth = new int[256];
    public int FONT_HEIGHT = 9;
    public Random fontRandom = new Random();
    private byte[] glyphWidth = new byte[65536];
    private int[] colorCode = new int[32];
    private ResourceLocation locationFontTexture;
    private final TextureManager renderEngine;
    private float posX;
    private float posY;
    private boolean unicodeFlag;
    private boolean bidiFlag;
    private float red;
    private float blue;
    private float green;
    private float alpha;
    private int textColor;
    private boolean randomStyle;
    private boolean boldStyle;
    private boolean italicStyle;
    private boolean underlineStyle;
    private boolean strikethroughStyle;
    public GameSettings gameSettings;
    public ResourceLocation locationFontTextureBase;
    public float offsetBold = 1.0F;
    private final float[] charWidthFloat = new float[256];
    private boolean blend = false;
    private final GlBlendState oldBlendState = new GlBlendState();

    @Getter
    @Setter
    protected String name;

    private NameProtect nameProtectModule;
    private AntiCrash antiCrashModule;

    public FontRenderer(GameSettings gameSettingsIn, ResourceLocation location, TextureManager textureManagerIn, boolean unicode) {
        this.name = "Minecraft";
        this.gameSettings = gameSettingsIn;
        this.locationFontTextureBase = location;
        this.locationFontTexture = location;
        this.renderEngine = textureManagerIn;
        this.unicodeFlag = unicode;
        this.locationFontTexture = FontUtils.getHdFontLocation(this.locationFontTextureBase);
        this.bindTexture(this.locationFontTexture);

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;

            if (i == 6) k += 85;

            if (gameSettingsIn.anaglyph) {
                int j1 = (k * 30 + l * 59 + i1 * 11) / 100;
                int k1 = (k * 30 + l * 70) / 100;
                int l1 = (k * 30 + i1 * 70) / 100;
                k = j1;
                l = k1;
                i1 = l1;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | (i1 & 255);
        }

        this.readGlyphSizes();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.locationFontTexture = FontUtils.getHdFontLocation(this.locationFontTextureBase);
        for (int i = 0; i < unicodePageLocations.length; ++i) {
            unicodePageLocations[i] = null;
        }
        this.readFontTexture();
        this.readGlyphSizes();
    }

    private void readFontTexture() {
        BufferedImage bufferedimage;
        try {
            bufferedimage = TextureUtil.readBufferedImage(this.getResourceInputStream(this.locationFontTexture));
        } catch (IOException ioexception1) {
            throw new RuntimeException(ioexception1);
        }

        Properties properties = FontUtils.readFontProperties(this.locationFontTexture);
        this.blend = FontUtils.readBoolean(properties, "blend", false);
        int i = bufferedimage.getWidth();
        int j = bufferedimage.getHeight();
        int k = i / 16;
        int l = j / 16;
        float f = (float) i / 128.0F;
        float f1 = Config.limit(f, 1.0F, 2.0F);
        this.offsetBold = 1.0F / f1;
        float f2 = FontUtils.readFloat(properties, "offsetBold", -1.0F);
        if (f2 >= 0.0F) this.offsetBold = f2;

        int[] aint = new int[i * j];
        bufferedimage.getRGB(0, 0, i, j, aint, 0, i);

        for (int i1 = 0; i1 < 256; ++i1) {
            int j1 = i1 % 16;
            int k1 = i1 / 16;
            int l1;
            for (l1 = k - 1; l1 >= 0; --l1) {
                int i2 = j1 * k + l1;
                boolean empty = true;
                for (int j2 = 0; j2 < l && empty; ++j2) {
                    int k2 = (k1 * l + j2) * i;
                    int pixel = aint[i2 + k2];
                    int alpha = pixel >> 24 & 255;
                    if (alpha > 16) empty = false;
                }
                if (!empty) break;
            }

            if (i1 == 32) {
                if (k <= 8) {
                    l1 = (int) (2.0F * f);
                } else {
                    l1 = (int) (1.5F * f);
                }
            }

            this.charWidthFloat[i1] = (float) (l1 + 1) / f + 1.0F;
        }

        FontUtils.readCustomCharWidths(properties, this.charWidthFloat);
        for (int j3 = 0; j3 < this.charWidth.length; ++j3) {
            this.charWidth[j3] = Math.round(this.charWidthFloat[j3]);
        }
    }

    private void readGlyphSizes() {
        InputStream inputstream = null;
        try {
            inputstream = this.getResourceInputStream(new ResourceLocation("font/glyph_sizes.bin"));
            inputstream.read(this.glyphWidth);
        } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
        } finally {
            IOUtils.closeQuietly(inputstream);
        }
    }

    private NameProtect getNameProtect() {
        if (nameProtectModule == null) nameProtectModule = GothajClient.getInstance().getModuleManager().getAndCast(NameProtect.class);
        return nameProtectModule;
    }

    private AntiCrash getAntiCrash() {
        if (antiCrashModule == null) antiCrashModule = GothajClient.getInstance().getModuleManager().getAndCast(AntiCrash.class);
        return antiCrashModule;
    }

    public String processString(String text) {
        if (text == null || text.isEmpty()) return text;
        Minecraft mc = Utils.mc;
        if (mc.thePlayer == null || mc.theWorld == null) return text;

        AntiCrash ac = getAntiCrash();
        if (ac != null && ac.isToggled()) {
            if (text.contains("Your PC is DEAD") || text.contains("CHEATER!") || text.contains("§r§a§k+")) {
                text = text.replace("CHEATER!", "")
                        .replace("Your PC is DEAD", "")
                        .replace("§r§a§k+§r§a§k+§r§a§k+ §r§4Your PC is DEAD §r§a§k+§r§a§k+§r§a§k+§r", "")
                        .replace("§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r§a§k+§r", "");
            }
        }

        if (text.indexOf('@') != -1) {
            String[] parts = text.split("@", 2);
            if (parts.length > 1) {
                String nickPart = parts[1].replaceAll("[^A-Za-z0-9_\\-\\[\\]\\{\\}\\\\`|^]", "");
                if (!nickPart.isEmpty()) {
                    String nick = nickPart.split(" ")[0];
                    IrcPlayer player = IrcPlayer.getPlayerByIRCUsername(nick);
                    if (player == null) player = IrcPlayer.getByIngameName(nick);

                    if (player != null) {
                        String inGameName = player.getIngameName();
                        String ircNick = player.getIrcNick();
                        String rankColor = player.getRank().getChatColor();

                        if (inGameName != null && text.contains("@" + inGameName)) {
                            text = text.replace("@" + inGameName, rankColor + "@" + inGameName);
                        } else if (ircNick != null && text.contains("@" + ircNick)) {
                            text = text.replace("@" + ircNick, rankColor + "@" + ircNick);
                        }
                    }
                }
            }
        }

        NameProtect np = getNameProtect();
        if (np != null && np.isToggled()) {
            String localName = mc.thePlayer.getName();

            if (np.targets.getOptionState("Yourself") && text.contains(localName) && !isModuleName(text)) {
                if (np.ownMode.is("IRC")) {
                    String replacement = GothajClient.getInstance().getCurrentPlayer().getRank().getChatColor()
                            + GothajClient.getInstance().getUser().getUsername();
                    text = replacePreservingFormat(text, localName, replacement);
                } else if (np.ownMode.is("Custom")) {
                    String raw = np.customSelf.getProperty();
                    String customName = raw.isEmpty() ? "You" : "§r" + translateAlternateColorCodes(raw);
                    text = replacePreservingFormat(text, localName, customName);
                }
            }

            if (np.targets.getOptionState("Everyone") && !mc.thePlayer.sendQueue.getPlayerInfoMap().isEmpty()) {
                for (NetworkPlayerInfo player : mc.thePlayer.sendQueue.getPlayerInfoMap()) {
                    String pName = player.getGameProfile().getName();
                    if (!pName.equalsIgnoreCase(localName) && text.contains(pName) && !isModuleName(text)) {
                        String raw = np.customEveryone.getProperty();
                        String replacement = raw.isEmpty() ? "Protected" : "§r" + translateAlternateColorCodes(raw);
                        text = replacePreservingFormat(text, pName, replacement.isEmpty() ? "Protected" : replacement);
                    }
                }
            }
        }

        return text;
    }

    private float renderChar(char ch, boolean italic) {
        if (ch != 32 && ch != 160) {
            int i = LEGACY_CHARS.indexOf(ch);
            return i != -1 && !this.unicodeFlag ? this.renderDefaultChar(i, italic) : this.renderUnicodeChar(ch, italic);
        } else {
            return !this.unicodeFlag ? this.charWidthFloat[ch] : 4.0F;
        }
    }

    private float renderDefaultChar(int ch, boolean italic) {
        int i = ch % 16 * 8;
        int j = ch / 16 * 8;
        int k = italic ? 1 : 0;
        this.bindTexture(this.locationFontTexture);
        float f = this.charWidthFloat[ch];
        float f1 = 7.99F;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f((float) i / 128.0F, (float) j / 128.0F);
        GL11.glVertex3f(this.posX + (float) k, this.posY, 0.0F);
        GL11.glTexCoord2f((float) i / 128.0F, ((float) j + 7.99F) / 128.0F);
        GL11.glVertex3f(this.posX - (float) k, this.posY + 7.99F, 0.0F);
        GL11.glTexCoord2f(((float) i + f1 - 1.0F) / 128.0F, (float) j / 128.0F);
        GL11.glVertex3f(this.posX + f1 - 1.0F + (float) k, this.posY, 0.0F);
        GL11.glTexCoord2f(((float) i + f1 - 1.0F) / 128.0F, ((float) j + 7.99F) / 128.0F);
        GL11.glVertex3f(this.posX + f1 - 1.0F - (float) k, this.posY + 7.99F, 0.0F);
        GL11.glEnd();
        return f;
    }

    private ResourceLocation getUnicodePageLocation(int page) {
        if (unicodePageLocations[page] == null) {
            unicodePageLocations[page] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", Integer.valueOf(page)));
            unicodePageLocations[page] = FontUtils.getHdFontLocation(unicodePageLocations[page]);
        }
        return unicodePageLocations[page];
    }

    private void loadGlyphTexture(int page) {
        this.bindTexture(this.getUnicodePageLocation(page));
    }

    private float renderUnicodeChar(char ch, boolean italic) {
        if (this.glyphWidth[ch] == 0) {
            return 0.0F;
        } else {
            int i = ch / 256;
            this.loadGlyphTexture(i);
            int j = this.glyphWidth[ch] >>> 4;
            int k = this.glyphWidth[ch] & 15;
            float f = (float) j;
            float f1 = (float) (k + 1);
            float f2 = (float) (ch % 16 * 16) + f;
            float f3 = (float) ((ch & 255) / 16 * 16);
            float f4 = f1 - f - 0.02F;
            float f5 = italic ? 1.0F : 0.0F;
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glTexCoord2f(f2 / 256.0F, f3 / 256.0F);
            GL11.glVertex3f(this.posX + f5, this.posY, 0.0F);
            GL11.glTexCoord2f(f2 / 256.0F, (f3 + 15.98F) / 256.0F);
            GL11.glVertex3f(this.posX - f5, this.posY + 7.99F, 0.0F);
            GL11.glTexCoord2f((f2 + f4) / 256.0F, f3 / 256.0F);
            GL11.glVertex3f(this.posX + f4 / 2.0F + f5, this.posY, 0.0F);
            GL11.glTexCoord2f((f2 + f4) / 256.0F, (f3 + 15.98F) / 256.0F);
            GL11.glVertex3f(this.posX + f4 / 2.0F - f5, this.posY + 7.99F, 0.0F);
            GL11.glEnd();
            return (f1 - f) / 2.0F + 1.0F;
        }
    }


    public int drawStringWithShadow(String text, float x, float y, int color) {
        return this.drawString(text, x, y, color, true);
    }

    public int drawString(String text, int x, int y, int color) {
        return this.drawString(text, (float) x, (float) y, color, false);
    }

    public int drawCenteredString(String text, int x, int y, int color) {
        String processed = processString(text);
        return this.drawStringInternal(processed, (float) x - getStringWidthRaw(processed) / 2f, (float) y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        return drawStringInternal(processString(text), x, y, color, dropShadow);
    }

    private int drawStringInternal(String text, float x, float y, int color, boolean dropShadow) {
        NameProtect np = getNameProtect();
        boolean useGradient = false;

        if (np != null && np.isToggled() && np.targets.getOptionState("Yourself") && np.ownMode.is("Custom")) {
            String customName = np.customSelf.getProperty().isEmpty() ? "You" : np.customSelf.getProperty();
            if (text.contains(customName)) {
                useGradient = true;
            }
        }

        if (useGradient) {
            FontUtil.drawHorizontalGradient(this, text, x, y, 5);
            return (int) (x + getStringWidthRaw(text));
        }

        this.enableAlpha();
        if (this.blend) {
            GlStateManager.getBlendState(this.oldBlendState);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
        }

        this.resetStyles();
        int i;
        if (dropShadow) {
            i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
            i = Math.max(i, this.renderString(text, x, y, color, false));
        } else {
            i = this.renderString(text, x, y, color, false);
        }

        if (this.blend) {
            GlStateManager.setBlendState(this.oldBlendState);
        }
        return i;
    }

    private static String replacePreservingFormat(String text, String target, String replacement) {
        if (text == null || target == null || replacement == null) return text;

        StringBuilder result = new StringBuilder();
        String stripped = stripColorCodes(text);
        int targetLen = target.length();

        int textIdx = 0;
        int strippedIdx = 0;

        while (textIdx < text.length()) {
            if (text.charAt(textIdx) == '§' && textIdx + 1 < text.length()) {
                result.append(text.charAt(textIdx));
                result.append(text.charAt(textIdx + 1));
                textIdx += 2;
                continue;
            }

            if (strippedIdx < stripped.length() &&
                    stripped.substring(strippedIdx).startsWith(target)) {

                result.append(replacement);

                int skipChars = targetLen;
                while (skipChars > 0 && textIdx < text.length()) {
                    if (text.charAt(textIdx) == '§' && textIdx + 1 < text.length()) {
                        textIdx += 2;
                    } else {
                        textIdx++;
                        skipChars--;
                    }
                }
                strippedIdx += targetLen;
            } else {
                result.append(text.charAt(textIdx));
                textIdx++;
                strippedIdx++;
            }
        }

        return result.toString();
    }

    private static String stripColorCodes(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                i++;
            } else {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    private static String translateAlternateColorCodes(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&' && i + 1 < text.length()) {
                char c = text.charAt(i + 1);
                if ("0123456789abcdefklmnor".indexOf(Character.toLowerCase(c)) != -1) {
                    sb.append('\u00A7').append(c);
                    i++;
                    continue;
                }
            }
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }


    private String bidiReorder(String text) {
        try {
            Bidi bidi = new Bidi((new ArabicShaping(8)).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException var3) {
            return text;
        }
    }

    private void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    private void renderStringAtPos(String text, boolean shadow) {
        for (int i = 0; i < text.length(); ++i) {
            char c0 = text.charAt(i);

            if (c0 == 167 && i + 1 < text.length()) {
                int l = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));

                if (l < 16) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;

                    if (l < 0 || l > 15) l = 15;
                    if (shadow) l += 16;

                    int i1 = this.colorCode[l];
                    if (Config.isCustomColors()) i1 = CustomColors.getTextColor(l, i1);

                    this.textColor = i1;
                    this.setColor((float) (i1 >> 16) / 255.0F, (float) (i1 >> 8 & 255) / 255.0F, (float) (i1 & 255) / 255.0F, this.alpha);
                } else if (l == 16) {
                    this.randomStyle = true;
                } else if (l == 17) {
                    this.boldStyle = true;
                } else if (l == 18) {
                    this.strikethroughStyle = true;
                } else if (l == 19) {
                    this.underlineStyle = true;
                } else if (l == 20) {
                    this.italicStyle = true;
                } else if (l == 21) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    this.setColor(this.red, this.blue, this.green, this.alpha);
                }

                ++i;
            } else {
                int j = LEGACY_CHARS.indexOf(c0);

                if (this.randomStyle && j != -1) {
                    int k = this.getCharWidth(c0);
                    char c1;
                    while (true) {
                        j = this.fontRandom.nextInt(LEGACY_CHARS.length());
                        c1 = LEGACY_CHARS.charAt(j);
                        if (k == this.getCharWidth(c1)) break;
                    }
                    c0 = c1;
                }

                float f1 = j != -1 && !this.unicodeFlag ? this.offsetBold : 0.5F;
                boolean flag = (c0 == 0 || j == -1 || this.unicodeFlag) && shadow;

                if (flag) {
                    this.posX -= f1;
                    this.posY -= f1;
                }

                float f = this.renderChar(c0, this.italicStyle);

                if (flag) {
                    this.posX += f1;
                    this.posY += f1;
                }

                if (this.boldStyle) {
                    this.posX += f1;

                    if (flag) {
                        this.posX -= f1;
                        this.posY -= f1;
                    }

                    this.renderChar(c0, this.italicStyle);
                    this.posX -= f1;

                    if (flag) {
                        this.posX += f1;
                        this.posY += f1;
                    }

                    f += f1;
                }

                this.doDraw(f);
            }
        }
    }

    protected void doDraw(float w) {
        if (this.strikethroughStyle) {
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            GlStateManager.disableTexture2D();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION);
            worldrenderer.pos((double) this.posX, (double) (this.posY + (float) (this.FONT_HEIGHT / 2)), 0.0D).endVertex();
            worldrenderer.pos((double) (this.posX + w), (double) (this.posY + (float) (this.FONT_HEIGHT / 2)), 0.0D).endVertex();
            worldrenderer.pos((double) (this.posX + w), (double) (this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F), 0.0D).endVertex();
            worldrenderer.pos((double) this.posX, (double) (this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F), 0.0D).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        if (this.underlineStyle) {
            Tessellator tessellator1 = Tessellator.getInstance();
            WorldRenderer worldrenderer1 = tessellator1.getWorldRenderer();
            GlStateManager.disableTexture2D();
            worldrenderer1.begin(7, DefaultVertexFormats.POSITION);
            int i = this.underlineStyle ? -1 : 0;
            worldrenderer1.pos((double) (this.posX + (float) i), (double) (this.posY + (float) this.FONT_HEIGHT), 0.0D).endVertex();
            worldrenderer1.pos((double) (this.posX + w), (double) (this.posY + (float) this.FONT_HEIGHT), 0.0D).endVertex();
            worldrenderer1.pos((double) (this.posX + w), (double) (this.posY + (float) this.FONT_HEIGHT - 1.0F), 0.0D).endVertex();
            worldrenderer1.pos((double) (this.posX + (float) i), (double) (this.posY + (float) this.FONT_HEIGHT - 1.0F), 0.0D).endVertex();
            tessellator1.draw();
            GlStateManager.enableTexture2D();
        }

        this.posX += w;
    }

    private int renderStringAligned(String text, int x, int y, int width, int color, boolean dropShadow) {
        if (this.bidiFlag) {
            int i = this.getStringWidthRaw(this.bidiReorder(text));
            x = x + width - i;
        }
        return this.renderString(text, (float) x, (float) y, color, dropShadow);
    }

    private int renderString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) {
            return 0;
        } else {

            if (this.bidiFlag) {
                text = this.bidiReorder(text);
            }

            if ((color & -67108864) == 0) {
                color |= -16777216;
            }

            if (dropShadow) {
                color = (color & 16579836) >> 2 | color & -16777216;
            }

            this.red = (float) (color >> 16 & 255) / 255.0F;
            this.blue = (float) (color >> 8 & 255) / 255.0F;
            this.green = (float) (color & 255) / 255.0F;
            this.alpha = (float) (color >> 24 & 255) / 255.0F;
            this.setColor(this.red, this.blue, this.green, this.alpha);
            this.posX = x;
            this.posY = y;
            this.renderStringAtPos(text, dropShadow);
            return (int) this.posX;
        }
    }

    public int getStringWidth(String text) {
        if (text == null) return 0;
        return getStringWidthRaw(processString(text));
    }

    public int getStringWidthRaw(String text) {
        if (text == null) {
            return 0;
        } else {
            float f = 0.0F;
            boolean bold = false;

            for (int i = 0; i < text.length(); ++i) {
                char c0 = text.charAt(i);
                float w = this.getCharWidthFloat(c0);

                if (w < 0.0F && i < text.length() - 1) {
                    ++i;
                    char fmt = text.charAt(i);
                    if (fmt == 'l' || fmt == 'L') {
                        bold = true;
                    } else if (fmt == 'r' || fmt == 'R' || isFormatColor(fmt)) {
                        bold = false;
                    }
                    w = 0.0F;
                }

                f += w;
                if (bold && w > 0.0F) {
                    f += this.unicodeFlag ? 1.0F : this.offsetBold;
                }
            }

            return Math.round(f);
        }
    }

    public int getCharWidth(char character) {
        try {
            return Math.round(this.getCharWidthFloat(character));
        } catch (Throwable t) {
            return 0;
        }
    }

    private float getCharWidthFloat(char ch) {
        if (ch == 167) {
            return -1.0F;
        } else if (ch != 32 && ch != 160) {
            int i = LEGACY_CHARS.indexOf(ch);
            if (ch > 0 && i != -1 && !this.unicodeFlag) {
                return this.charWidthFloat[i];
            } else if (this.glyphWidth[ch] != 0) {
                int j = this.glyphWidth[ch] >>> 4;
                int k = this.glyphWidth[ch] & 15;
                if (k > 7) { k = 15; j = 0; }
                ++k;
                return (float) ((k - j) / 2 + 1);
            } else {
                return 0.0F;
            }
        } else {
            return this.charWidthFloat[32];
        }
    }

    public String trimStringToWidth(String text, int width) {
        return this.trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        StringBuilder sb = new StringBuilder();
        float acc = 0.0F;
        int i = reverse ? text.length() - 1 : 0;
        int step = reverse ? -1 : 1;
        boolean fmtPending = false;
        boolean bold = false;

        for (int k = i; k >= 0 && k < text.length() && acc < (float) width; k += step) {
            char c0 = text.charAt(k);
            float w = this.getCharWidthFloat(c0);

            if (fmtPending) {
                fmtPending = false;
                if (c0 == 'l' || c0 == 'L') {
                    bold = true;
                } else if (c0 == 'r' || c0 == 'R') {
                    bold = false;
                } else if (isFormatColor(c0)) {
                    bold = false;
                }
            } else if (w < 0.0F) {
                fmtPending = true;
            } else {
                acc += w;
                if (bold) ++acc;
            }

            if (acc > (float) width) break;

            if (reverse) {
                sb.insert(0, c0);
            } else {
                sb.append(c0);
            }
        }

        return sb.toString();
    }

    private String trimStringNewline(String text) {
        while (text != null && text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor) {
        str = processString(str);

        if (this.blend) {
            GlStateManager.getBlendState(this.oldBlendState);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
        }

        this.resetStyles();
        this.textColor = textColor;
        str = this.trimStringNewline(str);
        this.renderSplitString(str, x, y, wrapWidth, false);

        if (this.blend) {
            GlStateManager.setBlendState(this.oldBlendState);
        }
    }

    private void renderSplitString(String str, int x, int y, int wrapWidth, boolean addShadow) {
        for (String s : this.listFormattedStringToWidth(str, wrapWidth)) {
            this.renderStringAligned(s, x, y, wrapWidth, this.textColor, addShadow);
            y += this.FONT_HEIGHT;
        }
    }

    public int splitStringWidth(String str, int maxLength) {
        return this.FONT_HEIGHT * this.listFormattedStringToWidth(str, maxLength).size();
    }

    public void setUnicodeFlag(boolean unicodeFlagIn) {
        this.unicodeFlag = unicodeFlagIn;
    }

    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    public void setBidiFlag(boolean bidiFlagIn) {
        this.bidiFlag = bidiFlagIn;
    }

    public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        String wrapped = this.wrapFormattedStringToWidth(processString(str), wrapWidth);
        return Arrays.asList(wrapped.split("\n"));
    }

    String wrapFormattedStringToWidth(String str, int wrapWidth) {
        if (str == null || str.length() <= 1) {
            return str;
        }
        StringBuilder out = new StringBuilder();
        String carryFormats = "";
        int start = 0;
        int n = str.length();

        while (start < n) {
            int next = sizeStringToWidth(str.substring(start), wrapWidth);
            int end = start + next;
            if (end >= n) {
                out.append(str.substring(start));
                break;
            }

            String line = str.substring(start, end);
            out.append(line).append('\n');

            boolean skip = end < n && (str.charAt(end) == ' ' || str.charAt(end) == '\n');
            int nextStart = end + (skip ? 1 : 0);

            carryFormats = getFormatFromString(line);
            if (!carryFormats.isEmpty() && nextStart < n && str.charAt(nextStart) != '§') {
                str = str.substring(0, nextStart) + carryFormats + str.substring(nextStart);
                n = str.length();
                nextStart += carryFormats.length();
            }

            start = nextStart;
        }

        return out.toString();
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

    private int sizeStringToWidth(String str, int wrapWidth) {
        int length = str.length();
        float widthAccum = 0.0F;
        int pos = 0;
        int lastSpace = -1;
        boolean bold = false;

        while (pos < length) {
            char c = str.charAt(pos);

            if (c == '\n') {
                pos++;
                lastSpace = pos;
                break;
            } else if (c == ' ') {
                lastSpace = pos;
            } else if (c == '§') {
                if (pos + 1 < length) {
                    char fmt = str.charAt(pos + 1);
                    if (fmt == 'l' || fmt == 'L') {
                        bold = true;
                    } else if (fmt == 'r' || fmt == 'R' || isFormatColor(fmt)) {
                        bold = false;
                    }
                    pos += 2;
                    continue;
                } else {
                    pos++;
                    continue;
                }
            } else {
                widthAccum += getCharWidth(c);
                if (bold) {
                    widthAccum += this.unicodeFlag ? 1.0F : this.offsetBold;
                }
            }

            if (Math.round(widthAccum) > wrapWidth) {
                break;
            }
            pos++;
        }

        if (pos != length && lastSpace != -1 && lastSpace < pos) {
            return lastSpace;
        } else {
            return pos;
        }
    }

    private static boolean isFormatColor(char colorChar) {
        char c = Character.toLowerCase(colorChar);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static boolean isFormatSpecial(char formatChar) {
        char c = Character.toLowerCase(formatChar);
        return (c >= 'k' && c <= 'o') || c == 'r';
    }

    public static String getFormatFromString(String text) {
        String s = "";
        int i = -1;
        int j = text.length();

        while ((i = text.indexOf(167, i + 1)) != -1) {
            if (i < j - 1) {
                char c0 = text.charAt(i + 1);
                if (isFormatColor(c0)) {
                    s = "\u00a7" + c0;
                } else if (isFormatSpecial(c0)) {
                    s = s + "\u00a7" + c0;
                }
            }
        }

        return s;
    }

    public boolean getBidiFlag() {
        return this.bidiFlag;
    }

    public int getColorCode(char character) {
        int i = "0123456789abcdef".indexOf(character);
        if (i >= 0 && i < this.colorCode.length) {
            int j = this.colorCode[i];
            if (Config.isCustomColors()) {
                j = CustomColors.getTextColor(i, j);
            }
            return j;
        } else {
            return 16777215;
        }
    }

    protected void setColor(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    protected void enableAlpha() {
        GlStateManager.enableAlpha();
    }

    protected void bindTexture(ResourceLocation rl) {
        this.renderEngine.bindTexture(rl);
    }

    protected InputStream getResourceInputStream(ResourceLocation rl) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(rl).getInputStream();
    }
}
