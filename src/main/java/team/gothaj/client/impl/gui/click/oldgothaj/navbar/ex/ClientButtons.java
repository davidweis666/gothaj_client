package team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex;

import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.Navbar;

public class ClientButtons extends NavbarButtons {
    private String name;
    private Navbar navbar;

    public ClientButtons(String name, Navbar navbar) {
        this.name = name;
        this.navcategory = NavCategory.Client;
        this.position = new PositionUtils(0.0, 0.0, 90.0, 15.0, 1.0F);
        this.navbar = navbar;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int color = -5592406;
        if (navbar.active == this) {
            color = -1;
        }

        Fonts.nunito14.drawString(
                name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase(),
                (float) (position.getX() + 30.0),
                (float) (position.getY() + position.getHeight() / 2.0 - Fonts.nunito14.getHeight() / 2.0),
                color
        );

        Fonts.rubik18Bold.drawString("S",
                (float) (position.getX() + 8.0),
                (float) (position.getY() + 2.0),
                color);
    }

    @Override
    public void clicked(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY) && button == 0) {
            navbar.active = this;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Navbar getNavbar() { return navbar; }
    public void setNavbar(Navbar navbar) { this.navbar = navbar; }
}
