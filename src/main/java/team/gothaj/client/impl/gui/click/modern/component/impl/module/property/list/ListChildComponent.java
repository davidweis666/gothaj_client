package team.gothaj.client.impl.gui.click.modern.component.impl.module.property.list;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.font.FontUtil;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;

import java.awt.*;

public class ListChildComponent extends Component {

    private final ListProperty parent;
    private final IFontRenderer font;
    private final String value;

    public ListChildComponent(ListProperty parent, IFontRenderer font, String value, float x, float y) {
        super(x, y);
        this.parent = parent;
        this.font = font;
        this.value = value;
    }

    @Override
    public float getWidth() {
        return font.getStringWidth(value) + 12f;
    }

    @Override
    public float getHeight() {
        return font.getHeight() + 4f;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    @SuppressWarnings("all")
    public void draw(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        boolean hovered = isMouseOver(mouseX, mouseY);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


        int baseColor;
        if (hovered) {
            baseColor = new Color(205, 205, 205, 170).getRGB();
        } else if (parent.getOptionState(value)) {
            baseColor = Color.WHITE.getRGB();
        } else {
            baseColor = new Color(205, 205, 205, 120).getRGB();
        }

        font.drawCenteredStringWithShadow(value, x + width / 2f, y + 2, baseColor);

        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isMouseOver(mouseX, mouseY)) {
            parent.setOptionState(value, !parent.getOptionState(value));
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth() * 2, getY() + getHeight());
    }
}
