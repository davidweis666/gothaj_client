package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;

public class PanelBooleanComponent extends PanelPropertyComponent {

    private static final IFontRenderer font = Fonts.nunito14;
    private static final float HEIGHT = 14f;
    private static final float SWITCH_WIDTH = 20f;
    private static final float SWITCH_HEIGHT = 8f;
    private static final float KNOB_SIZE = 7f;

    private final BooleanProperty property;
    private float toggleAnimation = 0f;
    private float hoverAnimation = 0f;

    public PanelBooleanComponent(BooleanProperty property) {
        this.property = property;
        this.toggleAnimation = property.getProperty() ? 1f : 0f;
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public boolean isVisible() {
        return property.getVisible().get();
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr, float alpha) {
        boolean hovered = isHovered(mouseX, mouseY);

        float targetHover = hovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.25f;

        float targetToggle = property.getProperty() ? 1f : 0f;
        toggleAnimation += (targetToggle - toggleAnimation) * 0.15f;

        float easedToggle = toggleAnimation * toggleAnimation * (3f - 2f * toggleAnimation);

        if (hoverAnimation > 0.01f) {
            Color hoverBg = new Color(255, 255, 255, (int) (6 * hoverAnimation * alpha));
            RoundedUtil.drawRound(x, y, width, HEIGHT, 2f, hoverBg);
        }

        Color textColor = new Color(180, 180, 185, (int) (255 * alpha));
        float textY = y + font.getMiddleOfBox(HEIGHT) + 1;
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        float switchX = x + width - SWITCH_WIDTH - 2f;
        float switchY = y + (HEIGHT - SWITCH_HEIGHT) / 2f + .5f;

        Color offColor = new Color(50, 50, 55, (int) (200 * alpha));
        Color onColorLeft = withAlpha(getSecondaryColor(), alpha);
        Color onColorRight = withAlpha(getPrimaryColor(), alpha);

        Color trackColorLeft = interpolateColor(offColor, onColorLeft, easedToggle);
        Color trackColorRight = interpolateColor(offColor, onColorRight, easedToggle);

        RoundedUtil.drawGradientHorizontal(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2f, trackColorLeft, trackColorRight);

        float knobLeftX = switchX + 1f;
        float knobRightX = switchX + SWITCH_WIDTH - KNOB_SIZE - 1f;
        float knobX = knobLeftX + (knobRightX - knobLeftX) * easedToggle;
        float knobY = switchY + (SWITCH_HEIGHT - KNOB_SIZE) / 2f;

        RoundedUtil.drawRound(knobX, knobY, KNOB_SIZE, KNOB_SIZE, KNOB_SIZE / 2f, new Color(255, 255, 255, (int) (255 * alpha)));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            property.setProperty(!property.getProperty());
        }
    }
}
