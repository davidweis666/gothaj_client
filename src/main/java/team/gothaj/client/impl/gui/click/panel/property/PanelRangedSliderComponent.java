package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.property.properties.RangedNumberProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PanelRangedSliderComponent extends PanelPropertyComponent {


    private static final IFontRenderer font = Fonts.nunito14;
    private static final IFontRenderer smallFont = Fonts.nunito14;
    private static final float HEIGHT = 24f;
    private static final float SLIDER_HEIGHT = 2f;
    private static final float NAME_SLIDER_GAP = 4f;

    private final RangedNumberProperty property;
    private float hoverAnimation = 0f;
    private float leftAnimation = 0f;
    private float rightAnimation = 0f;
    private boolean draggingLeft = false;
    private boolean draggingRight = false;

    public PanelRangedSliderComponent(RangedNumberProperty property) {
        this.property = property;
        this.leftAnimation = getLeftProgress();
        this.rightAnimation = getRightProgress();
    }

    private float getLeftProgress() {
        return (property.getLeft() - property.getMin()) / (property.getMax() - property.getMin());
    }

    private float getRightProgress() {
        return (property.getRight() - property.getMin()) / (property.getMax() - property.getMin());
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

        float targetHover = (hovered || draggingLeft || draggingRight) ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.25f;

        leftAnimation += (getLeftProgress() - leftAnimation) * 0.25f;
        rightAnimation += (getRightProgress() - rightAnimation) * 0.25f;

        if (hoverAnimation > 0.01f) {
            Color hoverBg = new Color(255, 255, 255, (int) (4 * hoverAnimation * alpha));
            RoundedUtil.drawRound(x, y, width, HEIGHT, 2f, hoverBg);
        }

        Color textColor = new Color(140, 140, 145, (int) (255 * alpha));
        float textY = y + 3f;
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        String leftStr = formatValue(property.getLeft());
        String rightStr = formatValue(property.getRight());
        String valueStr = leftStr + " - " + rightStr;
        Color valueColor = withAlpha(getPrimaryColor(), alpha * 0.9f);
        float valueWidth = smallFont.getStringWidth(valueStr);
        smallFont.drawString(valueStr.replaceAll(",", "."), x + width - valueWidth - 2f, textY, valueColor.getRGB());

        float sliderX = x + 2f;
        float sliderY = y + font.getHeight() + NAME_SLIDER_GAP + 4f;
        float sliderWidth = width - 4f;

        Color trackBg = new Color(25, 25, 30, (int) (255 * alpha));
        RoundedUtil.drawRound(sliderX, sliderY, sliderWidth, SLIDER_HEIGHT, 1.5f, trackBg);

        float leftX = sliderX + sliderWidth * leftAnimation;
        float rightX = sliderX + sliderWidth * rightAnimation;
        float filledWidth = rightX - leftX;
        if (filledWidth > 0) {
            RoundedUtil.drawGradientHorizontal(leftX, sliderY + 1, filledWidth, SLIDER_HEIGHT, 1.5f,
                    withAlpha(getPrimaryColor(), alpha), withAlpha(getSecondaryColor(), alpha));
        }

        float knobSize = 6f + hoverAnimation * 1.2f;

        float knobY = sliderY + (SLIDER_HEIGHT - knobSize) / 2f;
        Color knobColor = new Color(220, 220, 225, (int) (255 * alpha));

        float leftKnobX = leftX - knobSize / 2f;
        leftKnobX = Math.max(sliderX - knobSize / 4f, leftKnobX);
        RoundedUtil.drawRound(leftKnobX, knobY + 1, knobSize, knobSize, knobSize / 2f, knobColor);

        float rightKnobX = rightX - knobSize / 2f;
        rightKnobX = Math.min(sliderX + sliderWidth - knobSize / 2f, rightKnobX);
        RoundedUtil.drawRound(rightKnobX, knobY + 1, knobSize, knobSize, knobSize / 2f, knobColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            float sliderX = x + 4f;
            float sliderWidth = width - 8f;
            float leftX = sliderX + sliderWidth * leftAnimation;
            float rightX = sliderX + sliderWidth * rightAnimation;

            // Check which knob is closer
            float distToLeft = Math.abs(mouseX - leftX);
            float distToRight = Math.abs(mouseX - rightX);

            if (distToLeft < distToRight) {
                draggingLeft = true;
            } else {
                draggingRight = true;
            }
            updateValue(mouseX);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        draggingLeft = false;
        draggingRight = false;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (draggingLeft || draggingRight) {
            updateValue(mouseX);
        }
    }

    private void updateValue(int mouseX) {
        float sliderX = x + 2f;
        float sliderWidth = width - 4f;

        float progress = (mouseX - sliderX) / sliderWidth;
        progress = MathHelper.clamp(progress, 0f, 1f);

        float range = property.getMax() - property.getMin();
        float value = property.getMin() + progress * range;

        // Snap to increment
        float increment = property.getIncrement();
        value = Math.round(value / increment) * increment;
        value = MathHelper.clamp(value, property.getMin(), property.getMax());

        if (draggingLeft) {
            value = Math.min(value, property.getRight());
            setLeftValue(value);
        } else if (draggingRight) {
            value = Math.max(value, property.getLeft());
            setRightValue(value);
        }
    }

    private void setLeftValue(float value) {
        if (property.getType() == RangedNumberProperty.Type.INT) {
            property.setLeft(Math.round(value));
        } else {
            property.setLeft(new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).floatValue());
        }
    }

    private void setRightValue(float value) {
        if (property.getType() == RangedNumberProperty.Type.INT) {
            property.setRight(Math.round(value));
        } else {
            property.setRight(new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).floatValue());
        }
    }

    private String formatValue(float value) {
        if (property.getType() == RangedNumberProperty.Type.INT) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value);
        }
    }
}

