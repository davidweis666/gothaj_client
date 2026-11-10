package net.minecraft.client.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;
import java.util.UUID;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import org.lwjgl.Sys;
import team.gothaj.client.api.util.Logger;

public class Main
{
    public static void main(String[] p_main_0_)
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        OptionParser optionparser = new OptionParser();
        optionparser.allowsUnrecognizedOptions();
        optionparser.accepts("demo");
        optionparser.accepts("fullscreen");
        optionparser.accepts("checkGlErrors");
        OptionSpec<String> optionspec = optionparser.accepts("server").withRequiredArg();
        OptionSpec<Integer> optionspec1 = optionparser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(25565);
        OptionSpec<File> optionspec2 = optionparser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."));
        OptionSpec<File> optionspec3 = optionparser.accepts("assetsDir").withRequiredArg().<File>ofType(File.class);
        OptionSpec<File> optionspec4 = optionparser.accepts("resourcePackDir").withRequiredArg().<File>ofType(File.class);
        OptionSpec<String> optionspec5 = optionparser.accepts("proxyHost").withRequiredArg();
        OptionSpec<Integer> optionspec6 = optionparser.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).<Integer>ofType(Integer.class);
        OptionSpec<String> optionspec7 = optionparser.accepts("proxyUser").withRequiredArg();
        OptionSpec<String> optionspec8 = optionparser.accepts("proxyPass").withRequiredArg();
        OptionSpec<String> optionspec9 = optionparser.accepts("username").withRequiredArg().defaultsTo("Gothaj" + System.currentTimeMillis() % 1000L);
        OptionSpec<String> optionspec10 = optionparser.accepts("uuid").withRequiredArg();
        OptionSpec<String> optionspec11 = optionparser.accepts("accessToken").withRequiredArg().required();
        OptionSpec<String> optionspec12 = optionparser.accepts("version").withRequiredArg().required();
        OptionSpec<Integer> optionspec13 = optionparser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(854);
        OptionSpec<Integer> optionspec14 = optionparser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(480);
        OptionSpec<String> optionspec15 = optionparser.accepts("userProperties").withRequiredArg().defaultsTo("{}");
        OptionSpec<String> optionspec16 = optionparser.accepts("profileProperties").withRequiredArg().defaultsTo("{}");
        OptionSpec<String> optionspec17 = optionparser.accepts("assetIndex").withRequiredArg();
        // Fix: default to "mojang" instead of "legacy" so premium auth works when launcher doesn't specify
        OptionSpec<String> optionspec18 = optionparser.accepts("userType").withRequiredArg().defaultsTo("mojang");
        OptionSpec<String> optionspec19 = optionparser.nonOptions();
        OptionSet optionset = optionparser.parse(p_main_0_);
        List<String> list = optionset.valuesOf(optionspec19);

        if (!list.isEmpty())
        {
            System.out.println("Completely ignored arguments: " + list);
        }

        String s = (String)optionspec5.value(optionset);
        Proxy proxy = Proxy.NO_PROXY;

        if (s != null)
        {
            try
            {
                proxy = new Proxy(Type.SOCKS, new InetSocketAddress(s, optionset.valueOf(optionspec6)));
            }
            catch (Exception var46)
            {
            }
        }

        final String s1 = (String)optionspec7.value(optionset);
        final String s2 = (String)optionspec8.value(optionset);

        // Fix: corrected isNullOrEmpty logic (was inverted before)
        if (!proxy.equals(Proxy.NO_PROXY) && !isNullOrEmpty(s1) && !isNullOrEmpty(s2))
        {
            Authenticator.setDefault(new Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication(s1, s2.toCharArray());
                }
            });
        }

        int i = (Integer) optionspec13.value(optionset);
        int j = (Integer) optionspec14.value(optionset);
        boolean flag = optionset.has("fullscreen");
        boolean flag1 = optionset.has("checkGlErrors");
        boolean flag2 = optionset.has("demo");
        String s3 = (String)optionspec12.value(optionset);
        Gson gson = (new GsonBuilder()).registerTypeAdapter(PropertyMap.class, new Serializer()).create();
        PropertyMap propertymap = (PropertyMap)gson.fromJson((String)optionspec15.value(optionset), PropertyMap.class);
        PropertyMap propertymap1 = (PropertyMap)gson.fromJson((String)optionspec16.value(optionset), PropertyMap.class);
        File file1 = (File)optionspec2.value(optionset);
        File file2 = new File(System.getProperty("user.home"), "AppData/Roaming/.minecraft/assets");
        File file3 = optionset.has(optionspec4) ? (File)optionspec4.value(optionset) : new File(file1, "resourcepacks/");
        String username = (String)optionspec9.value(optionset);
        String passedUuid = optionset.has(optionspec10) ? (String)optionspec10.value(optionset) : null;
        String uuidString;
        if (passedUuid != null && !passedUuid.isEmpty()) {
            uuidString = passedUuid;
        } else {
            try {
                uuidString = UUID.nameUUIDFromBytes(("GothajUser:" + username).getBytes("UTF-8")).toString();
            } catch (Exception e) {
                uuidString = "00000000-0000-0000-0000-000000000000";
            }
        }
        String s5 = optionset.has(optionspec17) ? (String)optionspec17.value(optionset) : null;
        String s6 = (String)optionspec.value(optionset);
        Integer integer = (Integer)optionspec1.value(optionset);
        Session session = new Session(username, uuidString, (String)optionspec11.value(optionset), (String)optionspec18.value(optionset));
        GameConfiguration gameconfiguration = new GameConfiguration(
                new GameConfiguration.UserInformation(session, propertymap, propertymap1, proxy),
                new GameConfiguration.DisplayInformation(i, j, flag, flag1),
                new GameConfiguration.FolderInformation(file1, file3, file2, s5),
                new GameConfiguration.GameInformation(flag2, s3),
                new GameConfiguration.ServerInformation(s6, integer.intValue())
        );
        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread")
        {
            public void run()
            {
                Minecraft.stopIntegratedServer();
            }
        });
        Thread.currentThread().setName("Client thread");
        (new Minecraft(gameconfiguration)).run();
    }

    private static boolean isNullOrEmpty(String str)
    {
        // Fix: was "str != null && !str.isEmpty()" which is actually isNotEmpty
        return str == null || str.isEmpty();
    }
}