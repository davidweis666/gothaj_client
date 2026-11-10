package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.function.Supplier;

@Getter
@Setter
public class ColorProperty extends Property<Color> {

    public int colorGUIState;
    private float hue = 0, saturation = 1, brightness = 1;
    private int alpha;


    private float tempHue, tempSat, tempBright, tempAlpha;

    public ColorProperty(String name, Color color) {
        super(name, color);
        this.setColor(color);
        this.colorGUIState = 1;
        this.alpha = color.getAlpha();
    }


    public void setColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    public void setColor(float hue, float saturation, float brightness, float alpha) {
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
    }

    @Override
    public Color getProperty() {
        Color color = Color.getHSBColor(hue, saturation, brightness);
        int alpha = getAlpha();
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }



}
