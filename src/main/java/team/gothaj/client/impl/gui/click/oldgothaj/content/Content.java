package team.gothaj.client.impl.gui.click.oldgothaj.content;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.util.render.StencilUtil;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.ModuleButton;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.Navbar;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex.CategoryButton;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex.ClientButtons;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex.NavbarButtons;
import net.minecraft.client.gui.Gui;

public class Content {
    private PositionUtils position;
    private Navbar navbar;
    private double scrollY;
    private double temp;
    public double openAnimation;
    NavbarButtons last = null;

    public Content(PositionUtils position, Navbar navbar) {
        this.position = position;
        this.navbar = navbar;
        this.openAnimation = 0.0;
    }

    public void draw(int mouseX, int mouseY) {
        Gui.drawRect((int) position.getX(), (int) position.getY(),
                (int) position.getX2(), (int) position.getY2(), 0x70181818);

        openAnimation = (openAnimation * 9.0 + position.getWidth() / 2.0) / 10.0;
        StencilUtil.initStencilToWrite();
        Gui.drawRect((int) position.getX(), (int) position.getY(),
                (int) position.getX2(), (int) position.getY2(), 0x901A1A1A);
        StencilUtil.readStencilBuffer(1);

        if (navbar.active instanceof CategoryButton) {
            CategoryButton category = (CategoryButton) navbar.active;
            double s = 0.0;

            for (ModuleButton module : category.getModules()) {
                module.getPosition().setX(position.getX() + 2.5);
                module.getPosition().setY(position.getY() + 2.5 + s - scrollY);
                module.draw(mouseX, mouseY);
                s += module.getPosition().getHeight() + 2.5;
            }
        }

        this.last = this.navbar.active;
        StencilUtil.uninit();

        double save = position.getHeight();
        if (save < getBigger()) {
            double maxScrollY = getBigger() - save;
            int dWheel = Mouse.getDWheel();
            double size = (double) (dWheel / 60);
            if (size != 0.0) {
                temp += size;
            }

            if (Math.round(temp) != 0L) {
                temp = temp * 9.0 / 10.0;
                scrollY = scrollY - temp;
                if (scrollY < 0.0) {
                    scrollY = 0.0;
                } else if (scrollY > maxScrollY) {
                    scrollY = maxScrollY;
                }
            } else {
                temp = 0.0;
            }
        } else {
            scrollY = 0.0;
        }
    }

    public double getBigger() {
        if (navbar.active instanceof CategoryButton) {
            CategoryButton category = (CategoryButton) navbar.active;
            double s = 0.0;

            for (ModuleButton module : category.getModules()) {
                s += module.getPosition().getHeight() + 2.5;
            }

            return s;
        } else {
            return 0.0;
        }
    }

    public void clicked(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY)) {
            if (navbar.active instanceof CategoryButton) {
                CategoryButton category = (CategoryButton) navbar.active;

                for (ModuleButton module : category.getModules()) {
                    module.clicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void released(int mouseX, int mouseY, int button) {
        if (navbar.active instanceof CategoryButton) {
            CategoryButton category = (CategoryButton) navbar.active;

            for (ModuleButton module : category.getModules()) {
                module.released(mouseX, mouseY, button);
            }
        }
    }

    public void key(char typedChar, int keyCode) {
        if (navbar.active instanceof CategoryButton) {
            CategoryButton category = (CategoryButton) navbar.active;

            for (ModuleButton module : category.getModules()) {
                module.key(typedChar, keyCode);
            }
        }
    }

    public PositionUtils getPosition() { return position; }
    public void setPosition(PositionUtils position) { this.position = position; }

    public Navbar getNavbar() { return navbar; }
    public void setNavbar(Navbar navbar) { this.navbar = navbar; }
}
