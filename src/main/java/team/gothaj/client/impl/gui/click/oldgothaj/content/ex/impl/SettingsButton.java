package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import team.gothaj.client.api.property.Property;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;

public class SettingsButton {
    PositionUtils position;
    Property<?> settingMain;
    boolean isVisible = true;

    public void draw(int mouseX, int mouseY) {}

    public void click(int mouseX, int mouseY, int button) {}

    public void release(int mouseX, int mouseY, int button) {}

    public void key(char character, int key) {}

    public PositionUtils getPosition() { return position; }
    public void setPosition(PositionUtils position) { this.position = position; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean isVisible) { this.isVisible = isVisible; }

    public Property<?> getSettingMain() { return settingMain; }
    public void setSettingMain(Property<?> settingMain) { this.settingMain = settingMain; }
}
