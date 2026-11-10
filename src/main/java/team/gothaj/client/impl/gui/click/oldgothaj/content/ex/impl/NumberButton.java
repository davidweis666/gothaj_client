package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import net.minecraft.client.gui.Gui;

public class NumberButton extends SettingsButton {
    private NumberProperty setting;
    private boolean dragging;
    private PositionUtils slider;

    public NumberButton(NumberProperty setting, PositionUtils modesPosition) {
        this.setting = setting;
        this.position = modesPosition;
        this.settingMain = setting;
        this.slider = new PositionUtils(0.0, 0.0, 120.0, 10.0, 1.0F);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        Fonts.nunito14.drawString(setting.getName(),
                (float) (position.getX() + 8.0), (float) (position.getY() + 1.0), -1);
        slider.setX(position.getX2() - 10.0 - slider.getWidth());
        slider.setY(position.getY() + 2.5);
        double diff = Math.min(slider.getWidth(), Math.max(0.0, (double) mouseX - slider.getX()));
        if (dragging) {
            double newValue = roundToPlace(
                    diff / slider.getWidth() * (setting.getMax() - setting.getMin()) + setting.getMin(),
                    (int) slider.getWidth()
            );
            setting.setProperty((float) newValue);
        }

        Gui.drawRect((int) slider.getX(), (int) slider.getY(),
                (int) slider.getX2(), (int) slider.getY2(), 0x30000000);
        Gui.drawRect((int) slider.getX(), (int) slider.getY(),
                (int) slider.getX2(), (int) slider.getY() + 1, 0x90FFFFFF);
        Gui.drawRect((int) slider.getX(), (int) slider.getY(),
                (int) slider.getX() + 1, (int) slider.getY2(), 0x90FFFFFF);
        Gui.drawRect((int) slider.getX(), (int) slider.getY2() - 1,
                (int) slider.getX2(), (int) slider.getY2(), 0x90FFFFFF);
        Gui.drawRect((int) slider.getX2() - 1, (int) slider.getY(),
                (int) slider.getX2(), (int) slider.getY2(), 0x90FFFFFF);

        double x = slider.getX() + 2.0
                + (slider.getWidth() - 4.0) * (setting.getProperty() - setting.getMin()) / (setting.getMax() - setting.getMin());
        Gui.drawRect((int) (x - 2.0), (int) slider.getY(),
                (int) (x + 2.0), (int) slider.getY2(), 0x40505050);
        Fonts.nunito12.drawString(
                String.valueOf(setting.getProperty()),
                (float) (slider.getX() + slider.getWidth() / 2.0 - Fonts.nunito12.getStringWidth(String.valueOf(setting.getProperty())) / 2.0),
                (float) slider.getY() + 1.0F,
                -12424715
        );
    }

    @Override
    public void click(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY) && slider.isInside(mouseX, mouseY) && button == 0) {
            dragging = true;
        }
    }

    @Override
    public void release(int mouseX, int mouseY, int button) {
        dragging = false;
    }

    private double roundToPlace(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        } else {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }
}
