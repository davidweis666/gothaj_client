package team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex;

import java.util.ArrayList;
import java.util.List;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import team.gothaj.client.impl.gui.click.oldgothaj.content.ex.ModuleButton;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.Navbar;
import team.gothaj.client.main.GothajClient;
import net.minecraft.client.gui.Gui;

public class CategoryButton extends NavbarButtons {
    private ModuleCategory category;
    private List<ModuleButton> modules = new ArrayList<>();
    private Navbar navbar;

    public CategoryButton(ModuleCategory category, Navbar navbar) {
        this.category = category;
        this.navcategory = NavCategory.Modules;
        this.position = new PositionUtils(0.0, 0.0, 90.0, 15.0, 1.0F);
        this.navbar = navbar;

        for (Module m : GothajClient.getInstance().getModuleManager().getModulesInCategory(category)) {
            this.modules.add(new ModuleButton(m, new PositionUtils(0.0, 0.0, 237.5, 15.0, 1.0F)));
        }
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int color = -5592406;
        if (navbar.active == this) {
            color = -1;
        }

        String name = category.name().substring(0, 1).toUpperCase() + category.name().substring(1).toLowerCase();
        Fonts.nunito14.drawString(name,
                (float) (position.getX() + 30.0),
                (float) (position.getY() + position.getHeight() / 2.0 - Fonts.nunito14.getHeight() / 2.0),
                color);

        String icon = getCategoryIcon();
        Fonts.rubik18Bold.drawString(icon,
                (float) (position.getX() + 8.0),
                (float) (position.getY() + 2.0),
                color);
    }

    private String getCategoryIcon() {
        switch (category) {
            case COMBAT: return "C";
            case MOVEMENT: return "M";
            case PLAYER: return "P";
            case VISUAL: return "V";
            case MISC: return "?";
            case WORLD: return "W";
            case GHOST: return "G";
            default: return "?";
        }
    }

    @Override
    public void clicked(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY) && button == 0) {
            navbar.active = this;
        }
    }

    public ModuleCategory getCategory() { return category; }
    public List<ModuleButton> getModules() { return modules; }
    public Navbar getNavbar() { return navbar; }
}
