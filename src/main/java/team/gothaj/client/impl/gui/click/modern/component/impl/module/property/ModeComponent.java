package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;

import java.awt.*;

import static java.lang.String.valueOf;

public class ModeComponent extends Component {
    private static final IFontRenderer font = Fonts.nunito18;
    private final ModeProperty parent;
    @Getter
    public boolean expanded = false;

    private float animation = 0f;
    private static final float ANIMATION_SPEED = 0.25f;
    private static final float BASE_HEIGHT = 13f;
    private static final float OPTION_HEIGHT = 13f;
    private static final float SPACING = 2f;
    private static final float SCROLL_SPEED = 0.2f;
    private static final float SCROLL_PAUSE_TIME = 70f;

    private float scrollOffset = 0f;
    private float pauseCounter = 0f;
    private boolean needsScrolling = false;

    public ModeComponent(ModeProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
        this.identifier = parent.getName();
    }

    @Override
    public boolean shouldUpdateWidth() { return true; }
    @Override
    public float getWidth() { return width; }
    @Override
    public float getHeight() {
        float base = BASE_HEIGHT;
        if (animation > 0.01f) {
            float expandedHeight = parent.getModes().size() * OPTION_HEIGHT + SPACING;
            base += expandedHeight * animation;
        }
        return base;
    }
    @Override public float getX() { return x; }
    @Override public float getY() { return y; }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr) {
        float target = expanded ? 1f : 0f;
        animation += (target - animation) * ANIMATION_SPEED;
        if (Math.abs(animation - target) < 0.01f) animation = target;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final String value = valueOf(parent.getProperty());
        float valueWidth = font.getStringWidth(value);
        float rightPadding = 4f;
        float leftPadding = 5f;
        float spacing = 3f;

        float valueX = x + width - rightPadding - valueWidth;
        float maxIdentifierWidth = valueX - (x + leftPadding) - spacing;
        float actualIdentifierWidth = font.getStringWidth("§7" + identifier);

        needsScrolling = actualIdentifierWidth > maxIdentifierWidth;

        float identifierDrawX = x + leftPadding;

        if (needsScrolling) {
            if (pauseCounter < SCROLL_PAUSE_TIME) {
                pauseCounter++;
                scrollOffset = 0f;
            } else {
                scrollOffset += SCROLL_SPEED;
                float maxScroll = actualIdentifierWidth - maxIdentifierWidth + 10f;

                if (scrollOffset > maxScroll) {
                    scrollOffset = 0f;
                    pauseCounter = 0f;
                }
            }

            identifierDrawX = x + leftPadding - scrollOffset;

            ScissorStack.push(x + leftPadding, y, valueX - spacing, y + BASE_HEIGHT, sr);

            font.drawString("§7" + identifier, identifierDrawX, y + 2, -1, true);

            if (scrollOffset > 0) {
                float duplicateX = identifierDrawX + actualIdentifierWidth + 20f;
                font.drawString("§7" + identifier, duplicateX, y + 2, -1, true);
            }

            ScissorStack.pop();
        } else {
            scrollOffset = 0f;
            pauseCounter = 0f;
            font.drawString("§7" + identifier, identifierDrawX, y + 2, -1, true);
        }

        font.drawString(value, valueX, y + 2, -1, true);

        if (animation > 0.01f) {
            float yOffset = y + BASE_HEIGHT + SPACING;
            int modeCount = parent.getModes().size();
            float visibleHeight = animation * modeCount * OPTION_HEIGHT;

            ScissorStack.push(x, yOffset, x + width, yOffset + visibleHeight, sr);
            for (int i = 0; i < modeCount; i++) {
                String mode = parent.getModes().get(i);
                float currentY = yOffset + i * OPTION_HEIGHT;
                float itemOffset = i * OPTION_HEIGHT;

                if (itemOffset < visibleHeight) {
                    boolean hovered = MouseUtil.isHovered(mouseX, mouseY, x, currentY, x + getWidth(), currentY + OPTION_HEIGHT);

                    int baseColor;
                    if (hovered) {
                        baseColor = new Color(205, 205, 205).getRGB();
                    } else if (parent.is(mode)) {
                        baseColor = Color.WHITE.getRGB();
                    } else {
                        baseColor = new Color(205, 205, 205).getRGB();
                    }

                    float itemProgress = Math.min(1f, (visibleHeight - itemOffset) / OPTION_HEIGHT);
                    int alpha = (int) (255 * Math.max(0, itemProgress));

                    if (alpha > 10) {
                        font.drawCenteredStringWithShadow(mode, x + width / 2f, currentY + 2, ColorUtil.setAlpha(baseColor, hovered ? 170 : parent.is(mode) ? 255 : 120));
                    }
                }
            }
            ScissorStack.pop();
        }

        GL11.glPopMatrix();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (expanded && animation > 0.8f && mouseButton == 0) {
            float yOffset = y + BASE_HEIGHT + SPACING;
            for (int i = 0; i < parent.getModes().size(); i++) {
                String mode = parent.getModes().get(i);
                float currentY = yOffset + i * OPTION_HEIGHT;

                if (MouseUtil.isHovered(mouseX, mouseY, x, currentY, x + getWidth(), currentY + OPTION_HEIGHT)) {
                    parent.setProperty(mode);
                    expanded = false;
                    return;
                }
            }
        }
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, x + width, y + BASE_HEIGHT);
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int state) {}

    @Override public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {}

    @Override public boolean isMouseOver(int mouseX, int mouseY) {
        float h = getHeight();
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + h);
    }
}
