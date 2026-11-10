package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import java.awt.Color;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import net.minecraft.client.gui.Gui;

public class ColorButton extends SettingsButton {
    private ColorProperty setting;
    private PositionUtils colorPreview;
    private boolean open;
    private double height;
    private double rollAnimation;

    public ColorButton(ColorProperty setting, PositionUtils position) {
        this.settingMain = setting;
        this.position = position;
        this.setting = setting;
        this.colorPreview = new PositionUtils(0.0, 0.0, 30.0, 10.0, 1.0F);
        this.height = position.getHeight();
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        Fonts.nunito14.drawString(setting.getName(), (float) (position.getX() + 8.0), (float) (position.getY() + 1.0), -1);

        colorPreview.setX(position.getX() + position.getWidth() / 2.0 - 15.0);
        colorPreview.setY(position.getY() + 2.0);
        RoundedUtil.drawRound((float) colorPreview.getX(), (float) colorPreview.getY(),
                (float) colorPreview.getWidth(), (float) colorPreview.getHeight(), 2, setting.getProperty());

        Fonts.nunito12.drawString(
                "Alpha: " + setting.getAlpha() + "%",
                (float) (position.getX2() - Fonts.nunito12.getStringWidth("Alpha: " + setting.getAlpha() + "%") - 5.0),
                (float) (position.getY() + 1.0),
                -12424715
        );

        if (open) {
            rollAnimation = (rollAnimation * 11.0 + 50.0) / 12.0;
            position.setHeight(height + rollAnimation);

            double pickerX = position.getX() + 15.0;
            double pickerY = position.getY() + height + 2.5;
            double pickerW = 65.0;
            double pickerH = 40.0;

            Gui.drawRect((int) pickerX, (int) pickerY, (int) (pickerX + pickerW), (int) (pickerY + pickerH), 0x30000000);

            float[] hsb = Color.RGBtoHSB(
                    setting.getProperty().getRed(),
                    setting.getProperty().getGreen(),
                    setting.getProperty().getBlue(), null);
            Color hueColor = Color.getHSBColor(hsb[0], 1f, 1f);
            Gui.drawRect((int) pickerX, (int) (pickerY + pickerH + 3), (int) (pickerX + pickerW), (int) (pickerY + pickerH + 7), hueColor.getRGB());
        } else {
            rollAnimation = (rollAnimation * 11.0 + height) / 12.0;
            position.setHeight(rollAnimation);
        }
    }

    @Override
    public void click(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY) && button == 1) {
            open = !open;
        }
    }
}
