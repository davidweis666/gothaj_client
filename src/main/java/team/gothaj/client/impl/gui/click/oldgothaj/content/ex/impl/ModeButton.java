package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import java.util.ArrayList;
import java.util.List;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import net.minecraft.client.gui.Gui;

public class ModeButton extends SettingsButton {
    private ModeProperty setting;
    private PositionUtils modesPosition;
    private double defaultHeight;
    private List<ModeEntry> modes;
    private boolean open;
    private double rotateAnimation;
    private double rollAnimation;

    public ModeButton(ModeProperty setting, PositionUtils modesPosition) {
        this.settingMain = setting;
        this.setting = setting;
        this.position = modesPosition;
        this.defaultHeight = this.position.getHeight();
        this.modes = new ArrayList<>();

        for (String s : setting.getModes()) {
            this.modes.add(new ModeEntry(s, new PositionUtils(0.0, 0.0, 80.0, 12.0, 1.0F)));
        }

        this.modesPosition = new PositionUtils(0.0, 0.0, 80.0, 12.0, 1.0F);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        rotateAnimation = rotateAnimation * 12.0 / 13.0;
        Fonts.nunito14.drawString(setting.getName(), (float) (position.getX() + 8.0), (float) (position.getY() + 2.0), -1);
        modesPosition.setX(position.getX2() - modesPosition.getWidth() - 10.0);
        modesPosition.setY(position.getY() + 1.0);
        Gui.drawRect((int) modesPosition.getX(), (int) modesPosition.getY(),
                (int) modesPosition.getX2(), (int) modesPosition.getY2(), 0x30000000);
        Fonts.nunito12.drawString(
                setting.getProperty(),
                (float) (modesPosition.getX() + modesPosition.getWidth() / 2.0 - Fonts.nunito12.getStringWidth(setting.getProperty()) / 2.0),
                (float) (modesPosition.getY() + modesPosition.getHeight() / 2.0 - Fonts.nunito12.getHeight() / 2.0F),
                -12424715
        );
        if (open) {
            rollAnimation = getModesHeight();
            double p = 0.0;

            for (ModeEntry m : modes) {
                m.pos.setX(modesPosition.getX());
                m.pos.setY(modesPosition.getY2() + p);
                m.draw(mouseX, mouseY);
                p += m.pos.getHeight();
            }

            position.setHeight(defaultHeight + rollAnimation);
        } else {
            rollAnimation = (rollAnimation * 11.0 + defaultHeight) / 12.0;
            rotateAnimation = (rotateAnimation * 12.0 + 180.0) / 13.0;
            position.setHeight(rollAnimation);
        }

        Gui.drawRect((int) modesPosition.getX(), (int) modesPosition.getY(),
                (int) modesPosition.getX2(), (int) modesPosition.getY() + 1, -12424715);
        Gui.drawRect((int) modesPosition.getX(), (int) modesPosition.getY(),
                (int) modesPosition.getX() + 1, (int) modesPosition.getY2(), -12424715);
        Gui.drawRect((int) modesPosition.getX(), (int) modesPosition.getY2() - 1,
                (int) modesPosition.getX2(), (int) modesPosition.getY2(), -12424715);
        Gui.drawRect((int) modesPosition.getX2() - 1, (int) modesPosition.getY(),
                (int) modesPosition.getX2(), (int) modesPosition.getY2(), -12424715);
    }

    public double getModesHeight() {
        double p = 0.0;
        for (ModeEntry m : modes) {
            if (!m.mode.equalsIgnoreCase(setting.getProperty())) {
                p += m.pos.getHeight();
            }
        }
        return p;
    }

    @Override
    public void click(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY)) {
            if (button == 0) {
                if (open) {
                    for (ModeEntry m : modes) {
                        m.click(mouseX, mouseY, setting, button);
                    }
                }
            } else if (button == 1 && modesPosition.isInside(mouseX, mouseY)) {
                open = !open;
            }
        }
    }

    public class ModeEntry {
        public String mode;
        public PositionUtils pos;

        public ModeEntry(String mode, PositionUtils pos) {
            this.mode = mode;
            this.pos = pos;
        }

        public void draw(int mouseX, int mouseY) {
            Gui.drawRect((int) pos.getX(), (int) pos.getY(),
                    (int) pos.getX2(), (int) pos.getY2(), 0x30000000);
            Fonts.nunito12.drawString(
                    mode,
                    (float) (pos.getX() + pos.getWidth() / 2.0 - Fonts.nunito12.getStringWidth(mode) / 2.0),
                    (float) (pos.getY() + pos.getHeight() / 2.0 - Fonts.nunito12.getHeight() / 2.0F),
                    -1
            );
        }

        public void click(int mouseX, int mouseY, ModeProperty setting, int button) {
            if (pos.isInside(mouseX, mouseY) && button == 0) {
                setting.setProperty(mode);
            }
        }
    }
}
