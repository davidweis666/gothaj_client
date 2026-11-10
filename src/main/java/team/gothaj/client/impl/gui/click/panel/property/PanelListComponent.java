package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;
import java.util.List;

public class PanelListComponent extends PanelPropertyComponent {

    private static final IFontRenderer font = Fonts.nunito14;
    private static final float HEADER_HEIGHT = 14f;
    private static final float ITEM_HEIGHT = 12f;

    private final ListProperty property;
    private float hoverAnimation = 0f;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private float[] itemHoverAnimations;

    public PanelListComponent(ListProperty property) {
        this.property = property;
        this.itemHoverAnimations = new float[property.getOptions().size()];
    }

    @Override
    public float getHeight() {
        float expandedHeight = expanded ? (property.getOptions().size() * ITEM_HEIGHT + 2f) * expandAnimation : 0f;
        return HEADER_HEIGHT + expandedHeight;
    }

    @Override
    public boolean isVisible() {
        return property.getVisible().get();
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;

        float targetHover = hovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.25f;

        float targetExpand = expanded ? 1f : 0f;
        expandAnimation += (targetExpand - expandAnimation) * 0.22f;

        Color bgColor = new Color(17,17,17, (int) (255 * alpha));
        RoundedUtil.drawRound(x, y, width, HEADER_HEIGHT, 2f, bgColor);

        if (hoverAnimation > 0.01f) {
            Color hoverBg = new Color(255, 255, 255, (int) (6 * hoverAnimation * alpha));
            RoundedUtil.drawRound(x, y, width, HEADER_HEIGHT, 2f, hoverBg);
        }

        Color textColor = new Color(120, 120, 125, (int) (255 * alpha));
        float textY = y + font.getMiddleOfBox(HEADER_HEIGHT) + 1;
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        List<String> options = property.getOptions();
        int selectedCount = (int) options.stream().filter(property::getOptionState).count();
        String countStr = selectedCount + "/" + options.size();
        Color countColor = withAlpha(getPrimaryColor(), alpha * 0.85f);
        float countWidth = font.getStringWidth(countStr);
        font.drawString(countStr, x + width - countWidth - 2f, textY, countColor.getRGB());

        if (expandAnimation > 0.01f) {
            float itemY = y + HEADER_HEIGHT + 1f;

            float dropdownHeight = options.size() * ITEM_HEIGHT;
            Color dropBg = new Color(16, 16, 16, (int) (255 * alpha * expandAnimation));
            RoundedUtil.drawRound(x, itemY, width, dropdownHeight, 2f, dropBg);

            for (int i = 0; i < options.size(); i++) {
                String option = options.get(i);
                float currentItemY = itemY + i * ITEM_HEIGHT;
                boolean itemHovered = mouseX >= x && mouseX <= x + width
                    && mouseY >= currentItemY && mouseY <= currentItemY + ITEM_HEIGHT;
                boolean isEnabled = property.getOptionState(option);

                float targetItemHover = itemHovered ? 1f : 0f;
                itemHoverAnimations[i] += (targetItemHover - itemHoverAnimations[i]) * 0.3f;

                if (itemHoverAnimations[i] > 0.01f) {
                    Color itemBg = new Color(255, 255, 255, (int) (10 * itemHoverAnimations[i] * alpha * expandAnimation));
                    RoundedUtil.drawRound(x + 2f, currentItemY + 1f, width - 4f, ITEM_HEIGHT - 2f, 2f, itemBg);
                }

                float checkSize = 1.5f;
                float checkX = x + 4f;
                float checkY = currentItemY + 3f;

                if (isEnabled) {
                    Color indicatorColor = withAlpha(getPrimaryColor(), alpha * expandAnimation);
                    RoundedUtil.drawRound(checkX, checkY, checkSize, 6f, 1.5f, indicatorColor);
                } else {
                    Color checkOutline = new Color(20, 20, 20, (int) (255 * alpha * expandAnimation));
                    RoundedUtil.drawRound(checkX, checkY, checkSize, 6f, 1.5f, checkOutline);
                }

                Color itemTextColor = isEnabled
                    ? new Color(200, 200, 205, (int) (255 * alpha * expandAnimation))
                    : new Color(100, 100, 105, (int) (255 * alpha * expandAnimation));
                float textYY = currentItemY + font.getMiddleOfBox(ITEM_HEIGHT);
                font.drawString(option, x + 10f, textYY + 1f, itemTextColor.getRGB());
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            // Click on header
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                expanded = !expanded;
                return;
            }

            // Click on items
            if (expanded && expandAnimation > 0.5f) {
                float itemY = y + HEADER_HEIGHT + 1f;
                List<String> options = property.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    float currentItemY = itemY + i * ITEM_HEIGHT;
                    if (mouseX >= x && mouseX <= x + width
                        && mouseY >= currentItemY && mouseY <= currentItemY + ITEM_HEIGHT) {
                        property.toggleOption(options.get(i));
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

