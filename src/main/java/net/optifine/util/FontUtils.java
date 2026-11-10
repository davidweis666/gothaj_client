package net.optifine.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;

public class FontUtils
{
    private static final String PNG_EXTENSION = ".png";
    private static final String PROPERTIES_EXTENSION = ".properties";
    private static final String TEXTURES_PREFIX = "textures/";
    private static final String MCPATCHER_PREFIX = "mcpatcher/";
    private static final String WIDTH_PREFIX = "width.";
    private static final int WIDTH_PREFIX_LENGTH = WIDTH_PREFIX.length();

    public static Properties readFontProperties(ResourceLocation locationFontTexture)
    {
        String s = locationFontTexture.getResourcePath();

        if (!s.endsWith(PNG_EXTENSION))
        {
            return new PropertiesOrdered();
        }

        String s2 = s.substring(0, s.length() - PNG_EXTENSION.length()) + PROPERTIES_EXTENSION;

        Properties properties = new PropertiesOrdered();

        try
        {
            ResourceLocation resourcelocation = new ResourceLocation(
                    locationFontTexture.getResourceDomain(), s2);
            InputStream inputstream = Config.getResourceStream(
                    Config.getResourceManager(), resourcelocation);

            if (inputstream == null)
            {
                return properties;
            }

            Config.log("Loading " + s2);

            try (inputstream)
            {
                properties.load(inputstream);
            }
        }
        catch (FileNotFoundException var7)
        {

        }
        catch (IOException ioexception)
        {
            ioexception.printStackTrace(System.err);
        }

        return properties;
    }

    public static void readCustomCharWidths(Properties props, float[] charWidth)
    {
        for (String key : props.stringPropertyNames())
        {
            if (key.startsWith(WIDTH_PREFIX))
            {
                String indexStr = key.substring(WIDTH_PREFIX_LENGTH);
                int i = Config.parseInt(indexStr, -1);

                if (i >= 0 && i < charWidth.length)
                {
                    String value = props.getProperty(key);
                    float f = Config.parseFloat(value, -1.0F);

                    if (f >= 0.0F)
                    {
                        charWidth[i] = f;
                    }
                }
            }
        }
    }

    public static float readFloat(Properties props, String key, float defOffset)
    {
        String s = props.getProperty(key);

        if (s == null)
        {
            return defOffset;
        }
        else
        {
            float f = Config.parseFloat(s, Float.MIN_VALUE);

            if (f == Float.MIN_VALUE)
            {
                Config.warn("Invalid value for " + key + ": " + s);
                return defOffset;
            }
            else
            {
                return f;
            }
        }
    }

    public static boolean readBoolean(Properties props, String key, boolean defVal)
    {
        String s = props.getProperty(key);

        if (s == null)
        {
            return defVal;
        }
        else
        {
            String s1 = s.toLowerCase().trim();

            if (!s1.equals("true") && !s1.equals("on"))
            {
                if (!s1.equals("false") && !s1.equals("off"))
                {
                    Config.warn("Invalid value for " + key + ": " + s);
                    return defVal;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return true;
            }
        }
    }

    public static ResourceLocation getHdFontLocation(ResourceLocation fontLoc)
    {

        if (!Config.isCustomFonts() || fontLoc == null || !Config.isMinecraftThread())
        {
            return fontLoc;
        }

        String s = fontLoc.getResourcePath();

        if (!s.startsWith(TEXTURES_PREFIX))
        {
            return fontLoc;
        }

        String pathBuilder = MCPATCHER_PREFIX + s.substring(TEXTURES_PREFIX.length());

        ResourceLocation resourcelocation = new ResourceLocation(
                fontLoc.getResourceDomain(), pathBuilder);

        return Config.hasResource(Config.getResourceManager(), resourcelocation)
                ? resourcelocation
                : fontLoc;
    }
}
