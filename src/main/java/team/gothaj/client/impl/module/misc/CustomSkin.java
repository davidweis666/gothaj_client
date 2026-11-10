package team.gothaj.client.impl.module.misc;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.InputProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.value.ValueChangeEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class CustomSkin extends Module {

    public final ModeProperty skinMode = new ModeProperty("Skin Mode", "vajzii", "vajzii", "Nya", "Custom");

    public final InputProperty customName = new InputProperty("Custom Name").setVisible(() -> skinMode.is("Custom"));

    public CustomSkin() {
        super("CustomSkin", "Apply your favorite skin!", ModuleCategory.MISC);
        registerProperties(skinMode, customName);
    }

    @Override
    public String getSuffix() {
        return skinMode.is("Custom") ? customName.getProperty() : skinMode.getProperty();
    }

    @Listen
    public void onValueChange(ValueChangeEvent event) {
        if (event.getProperty() != skinMode && event.getProperty() != customName) return;{
            Logger.clientChat("Please re-connect to see changes");
        }
    }

    public String getSkinURL() {
        String baseURL = "https://minotar.net/skin/";
        switch (skinMode.getProperty()) {
            case "vajzii" -> {
                return baseURL + "vajzii.png";
            }
            case "Nya" -> {
                return baseURL + "LexiSlexi.png";
            }
            case "Custom" -> {
                return baseURL + (customName.getProperty().isEmpty() ? "gothajgayshit" : customName.getProperty());
            }
        }
        return null;
    }
}
