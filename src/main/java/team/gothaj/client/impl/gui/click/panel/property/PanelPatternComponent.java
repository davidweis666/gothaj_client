package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.PatternProperty;
import team.gothaj.client.api.property.properties.pattern.Vec2;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;
import java.util.List;

public class PanelPatternComponent extends PanelPropertyComponent {

    private static final IFontRenderer font = Fonts.nunito14;
    private static final float HEADER_HEIGHT = 12f;
    private static final float PREVIEW_HEIGHT = 30f;

    private final PatternProperty property;
    private float hoverAnimation = 0f;
    private boolean expanded = false;
    private float expandAnimation = 0f;

    public PanelPatternComponent(PatternProperty property) {
        this.property = property;
    }

    @Override
    public float getHeight() {
        float expandedHeight = expanded ? (PREVIEW_HEIGHT + 4f) * expandAnimation : 0f;
        return HEADER_HEIGHT + expandedHeight;
    }

    @Override
    public boolean isVisible() {
        return property.getVisible().get();
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;

        // Animations
        float targetHover = hovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.25f;

        float targetExpand = expanded ? 1f : 0f;
        expandAnimation += (targetExpand - expandAnimation) * 0.22f;

        // Background
        Color bgColor = new Color(30, 30, 35, (int) (180 * alpha));
        RoundedUtil.drawRound(x, y, width, HEADER_HEIGHT, 2f, bgColor);

        // Hover effect
        if (hoverAnimation > 0.01f) {
            Color hoverBg = new Color(255, 255, 255, (int) (10 * hoverAnimation * alpha));
            RoundedUtil.drawRound(x, y, width, HEADER_HEIGHT, 2f, hoverBg);
        }

        // Property name - vertically centered using getMiddleOfBox
        Color textColor = new Color(150, 150, 155, (int) (255 * alpha));
        float textY = y + font.getMiddleOfBox(HEADER_HEIGHT);
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        // Pattern info - same vertical position
        List<Vec2> points = property.getPatternPoints();
        String infoStr = points.size() + " pts";
        Color infoColor = new Color(getPrimaryColor().getRed(), getPrimaryColor().getGreen(), getPrimaryColor().getBlue(), (int) (200 * alpha));
        float infoWidth = font.getStringWidth(infoStr);
        font.drawString(infoStr, x + width - infoWidth - 2f, textY, infoColor.getRGB());

        // Pattern preview when expanded
        if (expandAnimation > 0.01f) {
            float previewY = y + HEADER_HEIGHT + 2f;

            // Background
            Color previewBg = new Color(20, 20, 25, (int) (220 * alpha * expandAnimation));
            RoundedUtil.drawRound(x, previewY, width, PREVIEW_HEIGHT, 2f, previewBg);

            // Draw pattern preview
            if (points.size() >= 2) {
                float previewX = x + 2f;
                float previewW = width - 4f;
                float previewH = PREVIEW_HEIGHT - 4f;
                float centerX = previewX + previewW / 2f;
                float centerY = previewY + 2f + previewH / 2f;
                float scale = Math.min(previewW, previewH) / 2f;

                // Draw pattern lines using GL11
                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glLineWidth(1.5f);
                GL11.glBegin(GL11.GL_LINE_STRIP);

                Color lineColor = withAlpha(getPrimaryColor(), alpha * expandAnimation * 0.8f);
                GL11.glColor4f(lineColor.getRed() / 255f, lineColor.getGreen() / 255f,
                              lineColor.getBlue() / 255f, lineColor.getAlpha() / 255f);

                for (int i = 0; i < Math.min(points.size(), 50); i++) {
                    Vec2 p = points.get(i);
                    float px = centerX + p.x * scale;
                    float py = centerY + p.y * scale;
                    GL11.glVertex2f(px, py);
                }

                GL11.glEnd();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glPopMatrix();
            } else {
                // No pattern
                Color noPatternColor = new Color(100, 100, 105, (int) (180 * alpha * expandAnimation));
                String noPatternStr = "No pattern";
                float npWidth = font.getStringWidth(noPatternStr);
                font.drawString(noPatternStr, x + (width - npWidth) / 2f, previewY + 10f, noPatternColor.getRGB());
            }

            // Clear button
            float btnX = x + width - 30f;
            float btnY = previewY + PREVIEW_HEIGHT - 12f;
            boolean btnHovered = mouseX >= btnX && mouseX <= btnX + 28f && mouseY >= btnY && mouseY <= btnY + 10f;
            Color btnBg = btnHovered ? new Color(180, 60, 60, (int) (200 * alpha * expandAnimation))
                                     : new Color(60, 40, 40, (int) (180 * alpha * expandAnimation));
            RoundedUtil.drawRound(btnX, btnY, 28f, 10f, 2f, btnBg);
            font.drawString("Clear", btnX + 4f, btnY + 1f, new Color(255, 255, 255, (int) (220 * alpha * expandAnimation)).getRGB());
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

            // Clear button
            if (expanded && expandAnimation > 0.5f) {
                float previewY = y + HEADER_HEIGHT + 2f;
                float btnX = x + width - 30f;
                float btnY = previewY + PREVIEW_HEIGHT - 12f;
                if (mouseX >= btnX && mouseX <= btnX + 28f && mouseY >= btnY && mouseY <= btnY + 10f) {
                    property.clearPattern();
                    return;
                }
            }
        }
    }

    @Override
    public void onClose() {
        expanded = false;
    }
}

