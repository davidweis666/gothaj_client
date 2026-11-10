package team.gothaj.client.impl.gui.click.oldgothaj.content.ex;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl.BooleanButton;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl.ColorButton;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl.ModeButton;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl.ModuleToggleButton;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl.NumberButton;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.impl.SettingsButton;
import net.minecraft.client.gui.Gui;

public class ModuleButton {
    private Module module;
    private PositionUtils position;
    private List<SettingsButton> settings = new ArrayList<>();
    private boolean open = false;
    private double rollAnimation;

    public ModuleButton(Module module, PositionUtils position) {
        this.module = module;
        this.position = position;
        this.rollAnimation = getSettingsHeight();
        this.settings.add(new ModuleToggleButton(module, new PositionUtils(0.0, 0.0, position.getWidth(), 15.0, 1.0F)));

        for (Property<?> setting : module.getProperties()) {
            if (setting instanceof ModeProperty) {
                this.settings.add(new ModeButton((ModeProperty) setting, new PositionUtils(0.0, 0.0, position.getWidth(), 15.0, 1.0F)));
            }
            if (setting instanceof BooleanProperty) {
                this.settings.add(new BooleanButton((BooleanProperty) setting, new PositionUtils(0.0, 0.0, position.getWidth(), 15.0, 1.0F)));
            }
            if (setting instanceof NumberProperty) {
                this.settings.add(new NumberButton((NumberProperty) setting, new PositionUtils(0.0, 0.0, position.getWidth(), 15.0, 1.0F)));
            }
            if (setting instanceof ColorProperty) {
                this.settings.add(new ColorButton((ColorProperty) setting, new PositionUtils(0.0, 0.0, position.getWidth(), 15.0, 1.0F)));
            }
        }
    }

    public void draw(int mouseX, int mouseY) {
        rollAnimation = (rollAnimation * 12.0 + getSettingsHeight()) / 13.0;
        position.setHeight(rollAnimation);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) position.getX() * sr.getScaleFactor(),
                Minecraft.getMinecraft().displayHeight - (int) position.getY2() * sr.getScaleFactor(),
                (int) position.getWidth() * sr.getScaleFactor(),
                (int) position.getHeight() * sr.getScaleFactor());

        Gui.drawRect((int) position.getX(), (int) position.getY(),
                (int) position.getX2(), (int) position.getY2(), 0x901A1A1A);

        double h = 0.0;

        for (SettingsButton s : getVisibleSettings()) {
            if (s.getSettingMain() == null || s.getSettingMain().isAvailable()) {
                s.getPosition().setX(position.getX());
                s.getPosition().setY(position.getY() + h);
                s.draw(mouseX, mouseY);
                h += s.getPosition().getHeight();
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public double getSettingsHeight() {
        double h = 0.0;

        for (SettingsButton s : settings) {
            if ((open || s instanceof ModuleToggleButton) && (s.getSettingMain() == null || s.getSettingMain().isAvailable())) {
                h += s.getPosition().getHeight();
            }
        }

        return h;
    }

    public List<SettingsButton> getVisibleSettings() {
        List<SettingsButton> set = new ArrayList<>();

        for (SettingsButton s : settings) {
            if ((open || s instanceof ModuleToggleButton)
                    && (s.getSettingMain() == null || s.getSettingMain().isAvailable())) {
                set.add(s);
            }
        }

        return set;
    }

    public void clicked(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY)) {
            for (SettingsButton s : getVisibleSettings()) {
                if (open || s instanceof ModuleToggleButton) {
                    if (s instanceof ModuleToggleButton && s.getPosition().isInside(mouseX, mouseY) && button == 1) {
                        open = !open;
                    }

                    if (s.getSettingMain() == null || s.getSettingMain().isAvailable()) {
                        s.click(mouseX, mouseY, button);
                    }
                }
            }
        }
    }

    public void released(int mouseX, int mouseY, int button) {
        for (SettingsButton s : getVisibleSettings()) {
            if ((open || s instanceof ModuleToggleButton) && (s.getSettingMain() == null || s.getSettingMain().isAvailable())) {
                s.release(mouseX, mouseY, button);
            }
        }
    }

    public void key(char typedChar, int keyCode) {
        for (SettingsButton s : getVisibleSettings()) {
            if ((open || s instanceof ModuleToggleButton) && (s.getSettingMain() == null || s.getSettingMain().isAvailable())) {
                s.key(typedChar, keyCode);
            }
        }
    }

    public PositionUtils getPosition() { return position; }
    public void setPosition(PositionUtils position) { this.position = position; }
}
