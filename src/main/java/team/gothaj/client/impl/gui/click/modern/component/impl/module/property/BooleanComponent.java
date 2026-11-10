package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;

import java.awt.*;

@SuppressWarnings("all")
public class BooleanComponent extends Component {
    private static final IFontRenderer font = Fonts.nunito17;
    private static final float CHECKBOX_SIZE = 10f;
    private static final float COMPONENT_HEIGHT = 15f;
    private static final float LEFT_OFFSET = 5f;
    private static final float CHECKBOX_TEXT_SPACING = 5f;
    private static final float SCROLL_SPEED = 0.2f;
    private static final float SCROLL_PAUSE_TIME = 70f;

    private final BooleanProperty parent;

    private float knobAlpha = Float.NaN;
    private float scrollOffset = 0f;
    private float pauseCounter = 0f;
    private boolean needsScrolling = false;


    public BooleanComponent(BooleanProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
        this.identifier = parent.getName();
    }

    @Override
    public float getWidth() {
        float textWidth = font.getStringWidth(identifier);
        return 73;
    }

    @Override
    public float getHeight() {
        return COMPONENT_HEIGHT + 2;
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
    public void draw(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float checkboxX = x + 75f;
        float textY = y + (COMPONENT_HEIGHT / 2f - font.getHeight() / 2f);
        float checkboxY = textY - 0.5f;

        boolean isToggled = parent.getProperty();

        float leftX = checkboxX + 1.5f;
        float rightX = checkboxX + 13f;

        float targetAlpha = isToggled ? 1f : 0f;
        if (Float.isNaN(knobAlpha)) knobAlpha = targetAlpha;

        knobAlpha += (targetAlpha - knobAlpha) * 0.13f;

        if (Math.abs(targetAlpha - knobAlpha) < 0.001f) knobAlpha = targetAlpha;
        knobAlpha = Math.max(0f, Math.min(1f, knobAlpha));

        float te = knobAlpha * knobAlpha * (3f - 2f * knobAlpha);

        float animX = leftX + (rightX - leftX) * te;

        Color offColor = new Color(110, 110, 110, 50);
        Color onColorR = getPrimaryColor();
        Color onColorL = getSecondaryColor();
        Color colorLeft = lerpColor(offColor, onColorL, te);
        Color colorRight = lerpColor(offColor, onColorR, te);

        float textStartX = x + 5f;
        float maxTextWidth = checkboxX - textStartX - 3f;
        float actualTextWidth = font.getStringWidth(identifier);
        needsScrolling = actualTextWidth > maxTextWidth;

        float textDrawX = textStartX;

        if (needsScrolling) {
            if (pauseCounter < SCROLL_PAUSE_TIME) {
                pauseCounter++;
                scrollOffset = 0f;
            } else {
                scrollOffset += SCROLL_SPEED;

                float maxScroll = actualTextWidth - maxTextWidth + 10f;

                if (scrollOffset > maxScroll) {
                    scrollOffset = 0f;
                    pauseCounter = 0f;
                }
            }

            textDrawX = textStartX - scrollOffset;
        } else {
            scrollOffset = 0f;
            pauseCounter = 0f;
        }

        ScissorStack.push(textStartX, y, textStartX + maxTextWidth, y + COMPONENT_HEIGHT + 2, scaledResolution);

        font.drawString(identifier, textDrawX, textY - 0.5f, -1, true);

        if (needsScrolling && scrollOffset > 0) {
            float duplicateX = textDrawX + actualTextWidth + 20f;
            font.drawString(identifier, duplicateX, textY - 0.5f, -1, true);
        }

        ScissorStack.pop();

        RoundedUtil.drawGradientHorizontal(checkboxX, checkboxY, 20f, 7f, 3.3f, colorLeft, colorRight);
        RoundedUtil.drawRound(animX, checkboxY + 0.5f, 6f, 6f, 3f, Color.WHITE);

        GL11.glPopMatrix();
    }



    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        parent.toggle();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {

    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        float width = getWidth();
        float checkboxX = x + 75;
        float checkboxY = y + (COMPONENT_HEIGHT - CHECKBOX_SIZE) / 2f;
        return MouseUtil.isHovered(mouseX, mouseY, checkboxX, checkboxY, checkboxX + 22, checkboxY + COMPONENT_HEIGHT);
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl= Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        int al= Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }
}
