package team.gothaj.client.impl.module.visual;

import javafx.stage.FileChooser;
import lombok.Getter;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.ActionProperty;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.other.FileUtil;
import team.gothaj.client.main.GothajClient;

import java.awt.*;
import java.io.File;


public class Capes extends Module {

    @Getter
    private static File selectedGifFile = null;
     @Getter
    private static long gifVersion = 0;

     public static final ModeProperty capeMode = new ModeProperty("Mode", "Gothaj", "Gothaj", "Polar");

    public static final BooleanProperty useGif = new BooleanProperty("GIF Cape", false)
            .setVisible(() -> GothajClient.getInstance().getCurrentPlayer() != null &&
                    GothajClient.getInstance().getCurrentPlayer().getRank().getPermissionLevel() != 0);

    public static final BooleanProperty matchHud = new BooleanProperty("Match HUD", false)
            .setVisible(() -> GothajClient.getInstance().getCurrentPlayer() != null &&
                    GothajClient.getInstance().getCurrentPlayer().getRank().getPermissionLevel() != 0 && capeMode.is("Polar") && !useGif.getProperty());



    public final ActionProperty browseCape = new ActionProperty("Browse GIF")
            .setListener(() -> {
                File searchGif = FileUtil.openFileChooser(null, new FileChooser.ExtensionFilter("GIF Image", "*.gif"));

                if (searchGif == null || !searchGif.exists()) {
                    GothajClient.getInstance().getNotificationManager().add("Capes", "Selected path does not exist!", Notification.Type.ERROR);
                    selectedGifFile = null;
                    return;
                }

                if (!searchGif.getName().toLowerCase().endsWith(".gif")) {
                    GothajClient.getInstance().getNotificationManager().add("Capes", "Selected file is not a GIF!", Notification.Type.ERROR);
                    selectedGifFile = null;
                    return;
                }

                selectedGifFile = searchGif;
                gifVersion = System.currentTimeMillis();

                GothajClient.getInstance().getNotificationManager().add("Capes", "GIF cape loaded: " + searchGif.getName(), Notification.Type.SUCCESS);
            })
            .setVisible(useGif::getProperty);


    public static final ColorProperty baseColor = new ColorProperty("Cape Base Color", Color.BLACK).setVisible(() -> capeMode.is("Polar") && !matchHud.getProperty() && !useGif.getProperty());

    public Capes() {
        super("Capes", "Shows cool cape on you", ModuleCategory.VISUAL);
        registerProperties(capeMode, matchHud, useGif, browseCape, baseColor);
    }

}
