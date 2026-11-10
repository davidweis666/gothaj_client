package team.gothaj.client.api.property.properties;

import lombok.Getter;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;

import java.util.HashMap;
import java.util.Map;

public class FontProperty extends Property<IFontRenderer> {

    @Getter
    public final Map<IFontRenderer, String> modes;

    public FontProperty(String name, IFontRenderer property) {
        super(name, property);
        modes = new HashMap<>();
        this.modes.put(Fonts.museo20, "Museo");
        this.modes.put(Fonts.nunito20, "Nunito");
        this.modes.put(Fonts.vk19Medium, "VK-Medium");
        this.modes.put(Fonts.vk17Medium, "VK");
        this.modes.put(Fonts.tahoma20, "Tahoma");
        this.modes.put(Fonts.rubik20, "Rubik");
    }

    public String getFontName() {
        return this.modes.get(this.getProperty());
    }

    public IFontRenderer getFontByName(String fontName) {
        for (Map.Entry<IFontRenderer, String> entry : modes.entrySet()) {
            if (entry.getValue().equals(fontName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void setPropertyString(String fontName) {
        IFontRenderer font = getFontByName(fontName);
        setProperty(font);
    }

}
