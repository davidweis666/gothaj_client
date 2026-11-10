package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.list.ListChildComponent;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;

import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.util.EnumChatFormatting.GRAY;

public class ListComponent extends Component {

    private static final IFontRenderer font = Fonts.nunito18;
    private final ListProperty parent;
    private final List<ListChildComponent> children;
    public boolean expanded = false;
    private float animation = 0f;
    private static final float ANIMATION_SPEED = 0.25f;
    private static final float BASE_HEIGHT = 13f;
    private static final float CHILD_HEIGHT = 15f;
    private static final float SPACING = 2f;

    public ListComponent(ListProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
        this.children = parent.getOptions()
                .stream()
                .map(option -> new ListChildComponent(parent, font, option, x + 2, y))
                .collect(Collectors.toList());
    }

    @Override public float getWidth() { return width; }
    @Override public boolean shouldUpdateWidth() { return true; }
    @Override public float getHeight() {
        float childrenHeight = parent.getOptions().size() * CHILD_HEIGHT + SPACING;
        return BASE_HEIGHT + childrenHeight * animation;
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr) {
        float target = expanded ? 1f : 0f;
        animation += (target - animation) * ANIMATION_SPEED;
        if (Math.abs(animation - target) < 0.01f) animation = target;

        String displayName = GRAY + parent.getName();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.resetColor();
        font.drawString(displayName, x + 5, y + 2, -1, true);
        font.drawString(String.valueOf(parent.getProperty().size()),
                x + width - 4 - font.getStringWidth(String.valueOf(parent.getOptions().size())), y + 2, -1, true);

        if (animation > 0.01f) {
            float yOffset = y + BASE_HEIGHT + SPACING;
            int count = parent.getOptions().size();
            float visibleHeight = animation * count * CHILD_HEIGHT;

            ScissorStack.push(x, yOffset, x + width, yOffset + visibleHeight, sr);

            for (int i = 0; i < count; i++) {
                ListChildComponent child = children.get(i);
                float currentY = yOffset + i * CHILD_HEIGHT;
                float itemOffset = i * CHILD_HEIGHT;

                if (itemOffset < visibleHeight) {
                    child.moveTo(x + 2, currentY);
                    child.draw(mouseX, mouseY, sr);
                }
            }

            ScissorStack.pop();
        }

        GL11.glPopMatrix();
    }

    @Override
    public void moveTo(float x, float y) {
        super.moveTo(x, y);
        for (ListChildComponent child : children) {
            child.moveTo(x, y);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (expanded && animation > 0.8f && mouseButton == 0) {
            int count = parent.getOptions().size();
            float maxVisible = animation * count * CHILD_HEIGHT;
            for (int i = 0; i < count; i++) {
                ListChildComponent child = children.get(i);
                float itemOffset = i * CHILD_HEIGHT;
                if (itemOffset < maxVisible && child.isMouseOver(mouseX, mouseY)) {
                    child.mouseClicked(mouseX, mouseY, mouseButton);
                    return;
                }
            }
        }
    }

    @Override
    public void setWidth(float width) {
        super.setWidth(width);
        for (ListChildComponent child : children) {
            child.setWidth(width - 4);
        }
    }

    @Override
    public void setHeight(float height) {
        super.setHeight(height);
        for (ListChildComponent child : children) {
            child.setHeight(CHILD_HEIGHT);
        }
    }

    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    @Override public void mouseReleased(int mouseX, int mouseY, int state) {}
    @Override public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {}
    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, x + width, y + getHeight());
    }
}
