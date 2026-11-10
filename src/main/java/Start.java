import net.minecraft.client.main.Main;

import java.io.File;

@SuppressWarnings("ALL")
public class Start {
    public static void main(String[] args) {
        final String appdataDirectory = System.getenv("APPDATA");

        File runDir = new File(appdataDirectory != null ? appdataDirectory : System.getProperty("user.home", "."), ".minecraft/");

        File gameDir = new File(runDir, ".");
        File assetsDir = new File(runDir, "assets/");

        Main.main(new String[]{
                "--version", "1.8.9",
                "--accessToken", "0",
                "--assetIndex", "1.8",
                "--userProperties", "{}",
                "--gameDir", gameDir.getAbsolutePath(),
                "--assetsDir", assetsDir.getAbsolutePath()
        });
    }
}