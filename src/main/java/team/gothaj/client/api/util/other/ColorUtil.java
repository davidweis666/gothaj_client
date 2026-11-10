package team.gothaj.client.api.util.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;

import java.awt.*;

public class ColorUtil {

    public static Color fade(int speed, int index, Color color, float alpha) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;

        Color colorHSB = new Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f));

        return new Color(colorHSB.getRed(), colorHSB.getGreen(), colorHSB.getBlue(), Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    public static Color interpolateColor(Color color1, Color color2, float ratio) {
        int r = (int) (color2.getRed() * ratio + color1.getRed() * (1 - ratio));
        int g = (int) (color2.getGreen() * ratio + color1.getGreen() * (1 - ratio));
        int b = (int) (color2.getBlue() * ratio + color1.getBlue() * (1 - ratio));
        int a = (int) (color2.getAlpha() * ratio + color1.getAlpha() * (1 - ratio));
        return new Color(r, g, b, a);
    }



    public static int mix(int c1, int c2, double size, double max) {
        int f3 = (c1 >> 24 & 255);
        int f4 = (c1 >> 24 & 255);

        Color col1 = new Color(c1);
        Color col2 = new Color(c2);

        int diffR = (int) (col1.getRed()-(col1.getRed()-col2.getRed())/max*size);
        int diffG = (int) (col1.getGreen()-(col1.getGreen()-col2.getGreen())/max*size);
        int diffB = (int) (col1.getBlue()-(col1.getBlue()-col2.getBlue())/max*size);
        if(diffR > 255) diffR = 255;
        if(diffR < 0)diffR = 0;
        if(diffG > 255) diffG = 255;
        if(diffG < 0)diffG = 0;
        if(diffB > 255) diffB = 255;
        if(diffB < 0)diffB = 0;

        return new Color(diffR, diffG, diffB).getRGB();
    }

    public static int setAlpha(int color, int alpha) {
        Color col = new Color(color);
        return new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha).getRGB();
    }


    public static int fadeBetween(final int color1, final int color2, float offset) {
        if (offset > 1.0f) {
            offset = 1.0f - offset % 1.0f;
        }
        final double invert = 1.0f - offset;
        final int r = (int)((color1 >> 16 & 0xFF) * invert + (color2 >> 16 & 0xFF) * offset);
        final int g = (int)((color1 >> 8 & 0xFF) * invert + (color2 >> 8 & 0xFF) * offset);
        final int b = (int)((color1 & 0xFF) * invert + (color2 & 0xFF) * offset);
        final int a = (int)((color1 >> 24 & 0xFF) * invert + (color2 >> 24 & 0xFF) * offset);
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }



    public static int astolfoColors(int yOffset, int yTotal) {
        float speed = 2900F;
        float hue = (float) (System.currentTimeMillis() % (int)speed) + ((yTotal - yOffset) * 9);
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5) {
            hue = 0.5F - (hue - 0.5f);
        }
        hue += 0.5F;
        return Color.HSBtoRGB(hue, 0.9f, 1F);
    }

    public static float[] get(int rgba) {
        float red = (rgba >> 16 & 255) / 255F;
        float green = (rgba >> 8 & 255) / 255F;
        float blue = (rgba & 255) / 255F;
        float alpha = (rgba >> 24 & 255) / 255F;

        return new float[]{red, green, blue, alpha};
    }
    public static Color rainbow(float index, float multi) {
        return rainbow(index, multi, 0.8F, 1F);
    }

    public static Color rainbow(float index, float multi, float saturation, float brightness) {
        double delay = (Math.abs(System.currentTimeMillis() / 20L) / 100.0 + 6.0F * ((index * multi) + 2.55) / 60) % 1;
        final double n3 = 1.0 - delay;
        return Color.getHSBColor((float) n3, saturation, brightness);
    }

    public static int fadeBetween(int startColor, int endColor) {
        return fadeBetween(startColor, endColor, (System.currentTimeMillis() % 2000) / 1000.0F);
    }

    public static int getColorFromPercentage(float percentage) {
        return Color.HSBtoRGB(Math.min(1.0F, Math.max(0.0F, percentage)) / 3, 0.9F, 0.9F);
    }

    public static Color darker(Color color, float factor) {
        factor = MathHelper.clamp(factor, 0.001f, 0.999f);

        return new Color(Math.max((int) (color.getRed() * factor), 0), Math.max((int) (color.getGreen() * factor), 0),
                Math.max((int) (color.getBlue() * factor), 0), color.getAlpha());
    }


    public static int getTeamColor(EntityLivingBase player) {

        int color = 0xFFFFFF;

        if (player == null) {
            return color;
        }

        ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();

        if (team != null) {
            String prefix = team.getColorPrefix();

            if (prefix.contains("⚑")) {
                String colorSplit = prefix.split("⚑")[0];
                String colorVal = colorSplit.replace("§7", "");
                if (colorVal.length() >= 2) {
                    color = Minecraft.getMinecraft().fontRendererObj.getColorCode(colorVal.charAt(1)) & 0xFFFFFF;
                }
            } else {
                String s = FontRenderer.getFormatFromString(prefix);
                if (s.length() >= 2) {
                    color = Minecraft.getMinecraft().fontRendererObj.getColorCode(s.charAt(1)) & 0xFFFFFF;
                }
            }
        }

        return color | 0xFF000000;
    }


}
