package team.gothaj.client.api.util.font;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.impl.module.visual.Hud;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontUtil implements Utils {

    public static final Map<FontType, Map<Integer, CFont>> customFontMap = new HashMap<>();

    public static void setupFonts() {
        for (FontType type : FontType.values()) {
            type.setup();
            Map<Integer, CFont> fontSizes = new HashMap<>();
            for (int size : type.getSizes()) {
                CFont font = new CFont(type.fromSize(size));
                if (type.hasBold()) font.setBoldFont(new CFont(type.fromBoldSize(size)));
                fontSizes.put(size, font);
            }
            customFontMap.put(type, fontSizes);
        }
    }

    @Getter
    public enum FontType {
        TAHOMA("tahoma", "tahoma-bold", 12, 18, 20),
        RUBIK("rubik", "rubik-bold", 11, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24),
        VK_MEDIUM("vk-medium", 17, 19),
        VK("vk-demibold", 19, 95),
        NOTIFS("notifs", 28),
        LOGIN("login", 36, 40),
        MUSEO("museo", 20),
        ICON("icon",11, 13),
        SURF("surfume", 14, 16, 20, 24, 30, 105),
        NUNITO("nunito-semibold", 12, 14, 16, 17, 18, 20, 21, 22, 23, 24, 26, 28),
        NUNITO_BOLD("nunito-bold", 12, 14, 16, 17, 18, 20, 21, 22, 23, 24, 26, 28),
        THEME("theme", 14, 16, 18, 20, 22, 24, 26),
        CATFONT("catfont", 12, 14, 16, 18, 20),
        UBUNTU("ubuntu", 14,16,18,19);

        private final ResourceLocation location;
        private final ResourceLocation boldLocation;

        private Font font;
        private Font boldFont;
        private final int[] sizes;

        FontType(String fontName, String boldName, int... sizes) {
            this.location = new ResourceLocation("gothaj", "render/font/" + fontName + ".ttf");
            this.boldLocation = new ResourceLocation("gothaj", "render/font/" + boldName + ".ttf");
            this.sizes = sizes;
        }

        FontType(String fontName, int... sizes) {
            this.location = new ResourceLocation("gothaj", "render/font/" + fontName + ".ttf");
            this.boldLocation = null;
            this.sizes = sizes;
        }

        public boolean hasBold() {
            return boldLocation != null;
        }

        public void setup() {
            font = getFontData(location);
            if (hasBold()) boldFont = getFontData(boldLocation);
        }

        public Font fromSize(int size) {
            return font.deriveFont(Font.PLAIN, size);
        }

        public Font fromBoldSize(int size) {
            return boldFont.deriveFont(Font.PLAIN, size);
        }

        public CFont size(int size) {
            return customFontMap.get(this).get(size);
        }

        public CFont boldSize(int size) {
            CFont f = customFontMap.get(this).get(size);
            return f != null ? f.getBoldFont() : null;
        }
    }

    private static Font getFontData(ResourceLocation location) {
        try (InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println("Error loading font: " + location.getResourcePath());
            return new Font("default", Font.PLAIN, 10);
        }
    }

    public static void drawHorizontalGradient(FontRenderer fontRenderer, String text, float x, float y, int moduleIndex) {
        drawHorizontalGradientOptimized(fontRenderer, text, x, y, moduleIndex, false);
    }

    public static void drawHorizontalGradient(IFontRenderer fontRenderer, String text, float x, float y, int moduleIndex) {
        drawHorizontalGradientOptimized(fontRenderer, text, x, y, moduleIndex, false);
    }


    public static void drawGradientWatermark(Object fontRenderer, String text, float x, float y, int moduleIndex, boolean centered) {
        if (text == null || text.isEmpty()) return;

        final boolean isVanilla = fontRenderer instanceof FontRenderer;
        final boolean isCustom = fontRenderer instanceof IFontRenderer;

        boolean coloredSuffix = Hud.arraylistOpt.getOptionState("Colored Suffix");
        int grayColor = new Color(170,170, 170).getRGB();

        char[] chars = text.toCharArray();
        int length = chars.length;

        float[] widths = new float[length];
        boolean[] isFormatCode = new boolean[length];
        float totalWidth = 0;

        for (int i = 0; i < length; i++) {
            char ch = chars[i];
            if (ch == '§' && i + 1 < length) {
                isFormatCode[i] = true;
                isFormatCode[i + 1] = true;
                i++;
                continue;
            }

            if (!isFormatCode[i]) {
                if (isVanilla) {
                    widths[i] = ((FontRenderer) fontRenderer).getCharWidth(ch);
                } else if (isCustom) {
                    widths[i] = ((IFontRenderer) fontRenderer).getCharWidthFloat(ch);
                }
                totalWidth += widths[i];
            }
        }

        float currentX = centered ? x - totalWidth / 2f : x;
        int charIndex = 0;
        boolean useGradient = true;

        char[] singleChar = new char[1];

        for (int i = 0; i < length; i++) {
            char ch = chars[i];

            if (ch == '§' && i + 1 < length) {
                useGradient = coloredSuffix;
                i++;
                continue;
            }

            if (isFormatCode[i]) continue;

            singleChar[0] = ch;
            int color = useGradient ? Hud.wmOptText.getOptionState("Sync Color") ? Hud.getColor(moduleIndex + charIndex) : Hud.getColorWatermark(moduleIndex + charIndex) : grayColor;

            if (isVanilla) {
                FontRenderer fr = (FontRenderer) fontRenderer;
                fr.drawString(String.valueOf(singleChar), currentX, y, color, true);
            } else if (isCustom) {
                IFontRenderer fr = (IFontRenderer) fontRenderer;
                fr.drawString(String.valueOf(singleChar), currentX, y, color, true);
            }

            currentX += widths[i];
            if (useGradient) charIndex++;
        }
    }

    public static void drawHorizontalGradientOptimized(Object fontRenderer, String text, float x, float y, int moduleIndex, boolean centered) {
        try {
            if (text == null || text.isEmpty()) return;

            final boolean isVanilla = fontRenderer instanceof FontRenderer;
            final boolean isCustom = fontRenderer instanceof IFontRenderer;

            boolean coloredSuffix = Hud.arraylistOpt.getOptionState("Colored Suffix");
            int grayColor = new Color(170,170, 170).getRGB();

            char[] chars = text.toCharArray();
            int length = chars.length;

            float[] widths = new float[length];
            boolean[] isFormatCode = new boolean[length];
            float totalWidth = 0;

            for (int i = 0; i < length; i++) {
                char ch = chars[i];
                if (ch == '§' && i + 1 < length) {
                    isFormatCode[i] = true;
                    isFormatCode[i + 1] = true;
                    i++;
                    continue;
                }

                if (!isFormatCode[i]) {
                    if (isVanilla) {
                        widths[i] = ((FontRenderer) fontRenderer).getCharWidth(ch);
                    } else if (isCustom) {
                        widths[i] = ((IFontRenderer) fontRenderer).getCharWidthFloat(ch);
                    }
                    totalWidth += widths[i];
                }
            }

            float currentX = centered ? x - totalWidth / 2f : x;
            int charIndex = 0;
            boolean useGradient = true;

            char[] singleChar = new char[1];

            for (int i = 0; i < length; i++) {
                char ch = chars[i];

                if (ch == '§' && i + 1 < length) {
                    useGradient = coloredSuffix;
                    i++;
                    continue;
                }

                if (isFormatCode[i]) continue;

                singleChar[0] = ch;
                int color = useGradient ? Hud.getColor(moduleIndex + charIndex) : grayColor;

                if (isVanilla) {
                    FontRenderer fr = (FontRenderer) fontRenderer;
                    fr.drawString(String.valueOf(singleChar), currentX, y, color, true);
                } else if (isCustom) {
                    IFontRenderer fr = (IFontRenderer) fontRenderer;
                    fr.drawString(String.valueOf(singleChar), currentX, y, color, true);
                }

                currentX += widths[i];
                if (useGradient) charIndex++;
            }
        } catch (StackOverflowError e) {

        }

    }

    public static void drawSyncedGradient(FontRenderer fontRenderer, String text, float x, float y, int moduleIndex) {
        drawSyncedGradientOptimized(fontRenderer, text, x, y, false, moduleIndex);
    }

    public static void drawSyncedGradient(IFontRenderer fontRenderer, String text, float x, float y, int moduleIndex) {
        drawSyncedGradientOptimized(fontRenderer, text, x, y, false, moduleIndex);
    }

    private static void drawSyncedGradientOptimized(Object fontRenderer, String text, float x, float y, boolean centered, int moduleIndex) {
        if (text == null || text.isEmpty()) return;

        final boolean isVanilla = fontRenderer instanceof FontRenderer;
        final boolean isCustom = fontRenderer instanceof IFontRenderer;

        boolean coloredSuffix = Hud.arraylistOpt.getOptionState("Colored Suffix");

        int grayColor = new Color(170,170, 170).getRGB();
        char[] chars = text.toCharArray();
        int length = chars.length;

        float[] widths = new float[length];
        boolean[] isFormatCode = new boolean[length];
        float totalWidth = 0;

        for (int i = 0; i < length; i++) {
            char ch = chars[i];
            if (ch == '§' && i + 1 < length) {
                isFormatCode[i] = true;
                isFormatCode[i + 1] = true;
                i++;
                continue;
            }

            if (!isFormatCode[i]) {
                if (isVanilla) {
                    widths[i] = ((FontRenderer) fontRenderer).getCharWidth(ch);
                } else if (isCustom) {
                    widths[i] = ((IFontRenderer) fontRenderer).getCharWidthFloat(ch);
                }
                totalWidth += widths[i];
            }
        }

        float currentX = centered ? x - totalWidth / 2f : x;
        int ordinal = 0;
        boolean useGradient = true;
        char[] singleChar = new char[1];

        for (int i = 0; i < length; i++) {
            char ch = chars[i];

            if (ch == '§' && i + 1 < length) {
                useGradient = coloredSuffix;
                i++;
                continue;
            }

            if (isFormatCode[i]) continue;

            singleChar[0] = ch;
            int color = useGradient ? Hud.getColor(ordinal) : grayColor;

            if (isVanilla) {
                ((FontRenderer) fontRenderer).drawString(String.valueOf(singleChar), currentX, y, color, true);
            } else if (isCustom) {
                ((IFontRenderer) fontRenderer).drawString(String.valueOf(singleChar), currentX, y, color, true);
            }

            currentX += widths[i];
            ordinal++;
        }
    }

    public static String upperCaseFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() == 1) return s.toUpperCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
