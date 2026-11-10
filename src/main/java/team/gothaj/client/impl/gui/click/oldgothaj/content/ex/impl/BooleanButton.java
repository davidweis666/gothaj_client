package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;

public class BooleanButton extends SettingsButton {
    private BooleanProperty setting;
    private PositionUtils checkBox;
    private double animation;

    public BooleanButton(BooleanProperty setting, PositionUtils checkBox) {
        this.setting = setting;
        this.settingMain = setting;
        this.position = checkBox;
        this.checkBox = new PositionUtils(0.0, 0.0, 10.0, 10.0, 1.0F);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (setting.getProperty()) {
            animation = (animation * 15.0 + 5.0) / 16.0;
        } else {
            animation = animation * 15.0 / 16.0;
        }

        Fonts.nunito14.drawString(setting.getName(), (float) (position.getX() + 8.0), (float) (position.getY() + 1.0), -1);
        checkBox.setX(position.getX2() - 55.0);
        checkBox.setY(position.getY() + 2.5);
        RoundedUtil.drawRound((float) checkBox.getX(), (float) checkBox.getY(),
                (float) checkBox.getWidth(), (float) checkBox.getHeight(), 2, new java.awt.Color(0x131313, false));
        RoundedUtil.drawRound(
                (float) (checkBox.getX() + checkBox.getWidth() / 2.0 - animation),
                (float) (checkBox.getY() + checkBox.getHeight() / 2.0 - animation),
                (float) (animation * 2.0),
                (float) (animation * 2.0),
                2,
                new java.awt.Color(0x434343)
        );
        if (setting.getProperty()) {
            Fonts.rubik18Bold.drawString("v", (float) (checkBox.getX() + checkBox.getWidth() / 2.0 - 3.0),
                    (float) (checkBox.getY() - 1.0), -1);
        }
    }

    @Override
    public void click(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY) && checkBox.isInside(mouseX, mouseY) && button == 0) {
            setting.setProperty(!setting.getProperty());
        }
    }
}
