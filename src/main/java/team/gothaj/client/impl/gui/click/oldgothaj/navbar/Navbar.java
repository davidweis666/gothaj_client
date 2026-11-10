package team.gothaj.client.impl.gui.click.oldgothaj.navbar;

import java.util.ArrayList;
import java.util.List;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.api.util.render.StencilUtil;
import team.gothaj.client.impl.gui.click.oldgothaj.PositionUtils;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex.CategoryButton;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex.ClientButtons;
import team.gothaj.client.impl.gui.click.oldgothaj.navbar.ex.NavbarButtons;
import net.minecraft.client.gui.Gui;

public class Navbar {
    private PositionUtils position;
    public List<CategoryButton> categories = new ArrayList<>();
    public List<ClientButtons> client = new ArrayList<>();
    public NavbarButtons active;
    private double yanimation;

    public Navbar(PositionUtils position) {
        this.position = position;
        this.categories.clear();

        for (ModuleCategory c : ModuleCategory.values()) {
            this.categories.add(new CategoryButton(c, this));
        }

        this.active = this.categories.get(0);
        this.client.add(new ClientButtons("Configs", this));
    }

    public void draw(int mouseX, int mouseY) {
        StencilUtil.initStencilToWrite();
        Gui.drawRect((int) position.getX(), (int) position.getY(),
                (int) position.getX2(), (int) position.getY2(), 0xFF242424);
        StencilUtil.readStencilBuffer(1);

        Gui.drawRect((int) position.getX(), (int) position.getY(),
                (int) position.getX2(), (int) position.getY2(), 0xEE1A1A1A);
        Gui.drawRect((int) position.getX(), (int) position.getY2() - 25.0,
                (int) position.getX2(), (int) position.getY2(), 0xFF222222);
        Gui.drawRect((int) position.getY(), (int) position.getY(),
                (int) position.getX2(), (int) position.getY() + 25.0, 0xFF222222);

        RoundedUtil.drawRound((float) (position.getX() + 5.0), (float) (position.getY() + 3.0), 18, 18, 5, new java.awt.Color(0x191919));
        Fonts.rubik18Bold.drawString("G",
                (float) (position.getX() + 14.0 - Fonts.rubik18Bold.getStringWidth("G") / 2.0),
                (float) (position.getY() + 4.0), -12424715);

        Fonts.nunito16Bold.drawString(
                "GOTHAJ", (float) (position.getX() + 14.0 + 18.0 + 5.0),
                (float) (position.getY() + 25.0 - Fonts.nunito16Bold.getHeight() / 2.0),
                -12424715
        );

        double i = 15.0;
        Gui.drawRect(
                (int) active.getPosition().getX(),
                (int) yanimation,
                (int) active.getPosition().getX2(),
                (int) (yanimation + active.getPosition().getHeight()),
                -12424715
        );
        i += drawCategory(mouseX, mouseY, position.getX(), position.getY() + 30.0 + i, position.getX2()) + 10.0;
        drawClientButtons(mouseX, mouseY, position.getX(), position.getY() + 30.0 + i, position.getX2());
        StencilUtil.uninit();
    }

    public void clicked(int mouseX, int mouseY, int button) {
        if (position.isInside(mouseX, mouseY)) {
            for (CategoryButton c : categories) {
                c.clicked(mouseX, mouseY, button);
            }
            for (NavbarButtons c : client) {
                c.clicked(mouseX, mouseY, button);
            }
        }
    }

    public PositionUtils getPosition() { return position; }
    public void setPosition(PositionUtils position) { this.position = position; }

    private double drawCategory(int mouseX, int mouseY, double startX, double startY, double endX) {
        double s = 0.0;
        double w = endX - startX - 10.0;

        String catLabel = "Category";
        Gui.drawRect((int) (startX + 5.0), (int) startY,
                (int) (startX + 5.0 + w / 2.0 - Fonts.nunito12.getStringWidth(catLabel) / 2.0 - 2.0),
                (int) startY + 1, -5592406);
        Gui.drawRect((int) (startX + 5.0 + w / 2.0 + Fonts.nunito12.getStringWidth(catLabel) / 2.0 + 2.0),
                (int) startY, (int) (endX - 5.0), (int) startY + 1, -5592406);
        Fonts.nunito12.drawString(catLabel,
                (float) (startX + 5.0 + w / 2.0 - Fonts.nunito12.getStringWidth(catLabel) / 2.0),
                (float) (startY - 4.0), -5592406);
        s += 5.0;

        yanimation = (yanimation * 9.0 + active.getPosition().getY()) / 10.0;

        for (CategoryButton c : categories) {
            c.getPosition().setX(startX + 5.0);
            c.getPosition().setY(startY + s);
            c.draw(mouseX, mouseY);
            s += c.getPosition().getHeight();
        }

        return s;
    }

    private double drawClientButtons(int mouseX, int mouseY, double startX, double startY, double endX) {
        double s = 0.0;
        double w = endX - startX - 10.0;

        String clientLabel = "Client";
        Gui.drawRect((int) (startX + 5.0), (int) startY,
                (int) (startX + 5.0 + w / 2.0 - Fonts.nunito12.getStringWidth(clientLabel) / 2.0 - 2.0),
                (int) startY + 1, -5592406);
        Gui.drawRect((int) (startX + 5.0 + w / 2.0 + Fonts.nunito12.getStringWidth(clientLabel) / 2.0 + 2.0),
                (int) startY, (int) (endX - 5.0), (int) startY + 1, -5592406);
        Fonts.nunito12.drawString(clientLabel,
                (float) (startX + 5.0 + w / 2.0 - Fonts.nunito12.getStringWidth(clientLabel) / 2.0),
                (float) (startY - 4.0), -5592406);
        s += 5.0;

        for (ClientButtons c : client) {
            c.getPosition().setX(startX + 5.0);
            c.getPosition().setY(startY + s);
            c.draw(mouseX, mouseY);
            s += c.getPosition().getHeight();
        }

        return s;
    }
}
