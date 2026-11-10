package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.property.properties.InvProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.property.InvConfigScreen;

import java.awt.Color;

public class InvComponent extends Component {

    private final InvProperty parent;
    private static final IFontRenderer font = Fonts.nunito18;

    public InvComponent(InvProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        font.drawString(parent.getName(), x + 5, y + 6, -1, true);

        float btnWidth = 40;
        float btnHeight = 14;
        float btnX = x + width - btnWidth - 5;
        float btnY = y + 4;

        boolean hovered = MouseUtil.isHovered(mouseX, mouseY, btnX, btnY, btnX + btnWidth, btnY + btnHeight);

        int color = hovered ? new Color(60, 60, 60).getRGB() : new Color(40, 40, 40).getRGB();
        RenderUtil.rect(btnX, btnY, btnX + btnWidth, btnY + btnHeight, color);

        String btnText = "Edit";
        font.drawString(btnText, btnX + (btnWidth - font.getStringWidth(btnText)) / 2, btnY + 4, -1, true);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float btnWidth = 40;
        float btnHeight = 14;
        float btnX = x + width - btnWidth - 5;
        float btnY = y + 4;

        if (MouseUtil.isHovered(mouseX, mouseY, btnX, btnY, btnX + btnWidth, btnY + btnHeight) && mouseButton == 0) {
            mc.displayGuiScreen(new InvConfigScreen(parent, mc.currentScreen));
        }
    }

    @Override
    public float getHeight() {
        return 22;
    }

    @Override public float getWidth() {
        return width;
    }
    @Override public float getX() {
        return x;
    }
    @Override public float getY() {
        return y;
    }

    @Override public boolean shouldUpdateWidth() { return true; }

    @Override public void mouseReleased(int mouseX, int mouseY, int state) {

    }
    @Override public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }
    @Override public boolean isMouseOver(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, x + width, y + getHeight());
    }
}
