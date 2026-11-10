package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;
import java.util.List;

public class PanelModeComponent extends PanelPropertyComponent {

    private static final IFontRenderer font = Fonts.nunito14;
    private static final IFontRenderer smallFont = Fonts.nunito14;
    private static final float HEIGHT = 14f;
    private static final float DROPDOWN_ITEM_HEIGHT = 12f;

    private final ModeProperty property;
    private float hoverAnimation = 0f;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private float[] itemHoverAnimations;

    public PanelModeComponent(ModeProperty property) {
        this.property = property;
        this.itemHoverAnimations = new float[property.getModes().size()];
    }

    @Override
    public float getHeight() {
        float expandedHeight = expanded ? (property.getModes().size() * DROPDOWN_ITEM_HEIGHT + 3f) * expandAnimation : 0f;
        return HEIGHT + expandedHeight;
    }

    @Override
    public boolean isVisible() {
        return property.getVisible().get();
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT;

        float targetHover = hovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.25f;

        float targetExpand = expanded ? 1f : 0f;
        expandAnimation += (targetExpand - expandAnimation) * 0.22f;

        Color bgColor = new Color(17, 17, 17, (int) (255 * alpha));
        RoundedUtil.drawRound(x, y, width, HEIGHT, 2f, bgColor);

        if (hoverAnimation > 0.01f) {
            Color hoverBg = new Color(255, 255, 255, (int) (6 * hoverAnimation * alpha));
            RoundedUtil.drawRound(x, y, width, HEIGHT, 2f, hoverBg);
        }

        Color textColor = new Color(120, 120, 120, (int) (255 * alpha));
        float textY = y + font.getMiddleOfBox(HEIGHT) + 1;
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        Color valueColor = withAlpha(getPrimaryColor(), alpha * 0.85f);
        String value = property.getProperty();
        float valueWidth = smallFont.getStringWidth(value);
        smallFont.drawString(value, x + width - valueWidth - 2f, textY, valueColor.getRGB());

        if (expandAnimation > 0.01f) {
            float dropdownY = y + HEIGHT + 1f;
            float dropdownHeight = property.getModes().size() * DROPDOWN_ITEM_HEIGHT;

            Color dropBg = new Color(16, 16, 16, (int) (255 * alpha * expandAnimation));
            RoundedUtil.drawRound(x, dropdownY, width, dropdownHeight, 3f, dropBg);

            List<String> modes = property.getModes();
            for (int i = 0; i < modes.size(); i++) {
                String mode = modes.get(i);
                float itemY = dropdownY + i * DROPDOWN_ITEM_HEIGHT;
                boolean itemHovered = mouseX >= x && mouseX <= x + width
                    && mouseY >= itemY && mouseY <= itemY + DROPDOWN_ITEM_HEIGHT;
                boolean isSelected = mode.equals(property.getProperty());

                float targetItemHover = itemHovered ? 1f : 0f;
                itemHoverAnimations[i] += (targetItemHover - itemHoverAnimations[i]) * 0.3f;

                if (itemHoverAnimations[i] > 0.01f || isSelected) {
                    Color itemBg = new Color(255, 255, 255, (int) (8 * itemHoverAnimations[i] * alpha * expandAnimation));
                    RoundedUtil.drawRound(x + 3f, itemY + 1f, width - 6f, DROPDOWN_ITEM_HEIGHT - 2f, 2f, itemBg);
                }

                if (isSelected) {
                    Color indicatorColor = withAlpha(getPrimaryColor(), alpha * expandAnimation);
                      RoundedUtil.drawRound(x + 4f, itemY + 3f, 1.5f, 6f, 1.5f, indicatorColor);
                }

                Color itemTextColor = isSelected
                    ? new Color(220, 220, 225, (int) (255 * alpha * expandAnimation))
                    : new Color(130, 130, 135, (int) (255 * alpha * expandAnimation));
                smallFont.drawString(mode, x + 10f, itemY + 3.5f, itemTextColor.getRGB());
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
                expanded = !expanded;
                return;
            }

            if (expanded && expandAnimation > 0.5f) {
                float dropdownY = y + HEIGHT + 1f;
                List<String> modes = property.getModes();
                for (int i = 0; i < modes.size(); i++) {
                    float itemY = dropdownY + i * DROPDOWN_ITEM_HEIGHT;
                    if (mouseX >= x && mouseX <= x + width
                        && mouseY >= itemY && mouseY <= itemY + DROPDOWN_ITEM_HEIGHT) {
                        property.setProperty(modes.get(i));
                        expanded = false;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onClose() {
        expanded = false;
    }
}

