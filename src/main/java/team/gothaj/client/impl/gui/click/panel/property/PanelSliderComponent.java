package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PanelSliderComponent extends PanelPropertyComponent {

    private static final IFontRenderer font = Fonts.nunito14;
    private static final IFontRenderer smallFont = Fonts.nunito14;
    private static final float HEIGHT = 24f;
    private static final float SLIDER_HEIGHT = 2f;
    private static final float NAME_SLIDER_GAP = 4f;

    private final NumberProperty property;
    private float hoverAnimation = 0f;
    private float valueAnimation = 0f;
    private boolean dragging = false;

    public PanelSliderComponent(NumberProperty property) {
        this.property = property;
        this.valueAnimation = getProgress();
    }

    private float getProgress() {
        return (property.getProperty() - property.getMin()) / (property.getMax() - property.getMin());
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

        float targetHover = (hovered || dragging) ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.25f;

        float targetValue = getProgress();
        valueAnimation += (targetValue - valueAnimation) * 0.25f;

        if (hoverAnimation > 0.01f) {
            Color hoverBg = new Color(255, 255, 255, (int) (4 * hoverAnimation * alpha));
            RoundedUtil.drawRound(x, y, width, HEIGHT, 2f, hoverBg);
        }

        Color textColor = new Color(140, 140, 145, (int) (255 * alpha));
        float textY = y + 3f;
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        String valueStr = formatValue(property.getProperty()).replaceAll(",", ".");
        Color valueColor = withAlpha(getPrimaryColor(), alpha * 0.9f);
        float valueWidth = smallFont.getStringWidth(valueStr);
        smallFont.drawString(valueStr, x + width - valueWidth - 2f, textY, valueColor.getRGB());

        float sliderX = x + 2f;
        float sliderY = y + font.getHeight() + NAME_SLIDER_GAP + 4f;
        float sliderWidth = width - 4f;

        Color trackBg = new Color(25, 25, 30, (int) (255 * alpha));
        RoundedUtil.drawRound(sliderX, sliderY, sliderWidth, SLIDER_HEIGHT, 1.5f, trackBg);

        float filledWidth = Math.max(SLIDER_HEIGHT, sliderWidth * valueAnimation);
        if (filledWidth > 0) {
            RoundedUtil.drawGradientHorizontal(sliderX, sliderY, filledWidth, SLIDER_HEIGHT, 1.5f,
                withAlpha(getPrimaryColor(), alpha), withAlpha(getSecondaryColor(), alpha));
        }

        float knobSize = 6f + hoverAnimation * 1.2f;
        float knobX = sliderX + (sliderWidth * valueAnimation) - knobSize / 2f;
        knobX = Math.max(sliderX - knobSize / 4f, Math.min(knobX, sliderX + sliderWidth - knobSize / 2f));
        float knobY = sliderY + (SLIDER_HEIGHT - knobSize) / 2f;
        Color knobColor = new Color(220, 220, 225, (int) (255 * alpha));
        RoundedUtil.drawRound(knobX, knobY, knobSize, knobSize, knobSize / 2f, knobColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            updateValue(mouseX);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (dragging) {
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

        // Clamp
        value = MathHelper.clamp(value, property.getMin(), property.getMax());

        // Set property based on type
        if (property.getType() == NumberProperty.Type.INT) {
            property.setProperty((int) value);
        } else {
            property.setProperty(new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).floatValue());
        }
    }

    private String formatValue(float value) {
        if (property.getType() == NumberProperty.Type.INT) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value);
        }
    }
}

