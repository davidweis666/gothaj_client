package team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl;

import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;

public class ModuleToggleButton extends SettingsButton {
    private Module module;
    private PositionUtils toggleButton;
    private double toggleAnimation;

    public ModuleToggleButton(Module mod, PositionUtils position) {
        this.module = mod;
        this.position = position;
        this.toggleButton = new PositionUtils(0.0, 0.0, 20.0, 10.0, 1.0F);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int color = -1;
        if (module.isToggled()) {
            toggleAnimation = (toggleAnimation * 9.0 + toggleButton.getWidth() - 10.0) / 10.0;
        } else {
            toggleAnimation = toggleAnimation * 9.0 / 10.0;
        }

        color = ColorUtil.mix(-1, -12424715, (int) toggleAnimation, (int) (toggleButton.getWidth() - 10.0));
        RoundedUtil.drawRound((float) position.getX(), (float) position.getY(),
                (float) position.getWidth(), (float) position.getHeight(), 1, new java.awt.Color(0x1A1A1A));
        Fonts.nunito14.drawString(module.getName(), (float) (position.getX() + 5.0), (float) (position.getY() + 3.0), color);
        Fonts.nunito12.drawString(
                module.getDescription(),
                (float) (position.getX() + position.getWidth() / 2.0 - Fonts.nunito12.getStringWidth(module.getDescription()) / 2.0),
                (float) (position.getY() + 2.0),
                -5592406
        );
        toggleButton.setX(position.getX2() - toggleButton.getWidth() - 2.5);
        toggleButton.setY(position.getY() + 2.5);
        RoundedUtil.drawRound(
                (float) toggleButton.getX(),
                (float) toggleButton.getY(),
                (float) toggleButton.getWidth(),
                (float) toggleButton.getHeight(),
                4,
                new java.awt.Color(ColorUtil.mix(-13421773, -12424715, (int) toggleAnimation, (int) (toggleButton.getWidth() - 10.0)))
        );
        RoundedUtil.drawRound(
                (float) (toggleButton.getX() + 3.0 + toggleAnimation),
                (float) (toggleButton.getY() + toggleButton.getHeight() / 2.0 - 4.0),
                8,
                8,
                4,
                java.awt.Color.WHITE
        );
    }

    @Override
    public void click(int mouseX, int mouseY, int button) {
        if (toggleButton.isInside(mouseX, mouseY) && button == 0) {
            module.toggle();
        }
    }
}
