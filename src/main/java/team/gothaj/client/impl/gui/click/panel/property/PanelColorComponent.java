package team.gothaj.client.impl.gui.click.panel.property;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;

import java.awt.*;

public class PanelColorComponent extends PanelPropertyComponent {

    private static final IFontRenderer font = Fonts.nunito14;
    private static final float HEIGHT = 12f;
    private static final float PICKER_HEIGHT = 40f;

    private final ColorProperty property;
    private float hoverAnimation = 0f;
    private boolean expanded = false;
    private float expandAnimation = 0f;

    private boolean draggingPicker = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    private float[] hsb = new float[3];
    private float alpha = 1f;

    public PanelColorComponent(ColorProperty property) {
        this.property = property;
        updateHSBFromColor();
    }

    private void updateHSBFromColor() {
        Color color = property.getProperty();
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        alpha = color.getAlpha() / 255f;
    }

    @Override
    public float getHeight() {
        float expandedHeight = expanded ? (PICKER_HEIGHT + 4f) * expandAnimation : 0f;
        return HEIGHT + expandedHeight;
    }

    @Override
    public boolean isVisible() {
        return property.getVisible().get();
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr, float drawAlpha) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT;

        float targetHover = hovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.2f;

        float targetExpand = expanded ? 1f : 0f;
        expandAnimation += (targetExpand - expandAnimation) * 0.18f;

        Color textColor = new Color(150, 150, 155, (int) (255 * drawAlpha));
        float textY = y + font.getMiddleOfBox(HEIGHT) + 1;
        font.drawString(property.getName(), x + 2f, textY, textColor.getRGB());

        float previewSize = 8f;
        float previewX = x + width - previewSize - 2f;
        float previewY = y + (HEIGHT - previewSize) / 2f;

        Color previewColor = withAlpha(property.getProperty(), drawAlpha);
        RoundedUtil.drawRound(previewX, previewY, previewSize, previewSize, 2f, previewColor);
        RoundedUtil.drawRoundOutline(previewX, previewY, previewSize, previewSize, 2f, 0.5f,
            new Color(0, 0, 0, 0), new Color(80, 80, 85, (int)(200 * drawAlpha)));

        if (expandAnimation > 0.01f) {
            drawColorPicker(mouseX, mouseY, drawAlpha * expandAnimation);
        }
    }

    private void drawColorPicker(int mouseX, int mouseY, float drawAlpha) {
        float pickerY = y + HEIGHT + 2f;
        float sliderWidth = 10f;
        float sliderGap = 3f;

        float totalSliderSpace = (sliderWidth * 2) + (sliderGap * 2);
        float sbWidth = width - totalSliderSpace - 2f;
        float sbHeight = PICKER_HEIGHT;

        float sbX = x + 1f;
        float sbY = pickerY;

        Color baseColor = Color.getHSBColor(hsb[0], 1f, 1f);
        drawSBGradient(sbX, sbY, sbWidth, sbHeight, baseColor, drawAlpha);

        float sbCursorX = sbX + sbWidth * hsb[1];
        float sbCursorY = sbY + sbHeight * (1f - hsb[2]);
        RoundedUtil.drawRound(sbCursorX - 2f, sbCursorY - 2f, 4f, 4f, 2f,
            new Color(255, 255, 255, (int) (255 * drawAlpha)));

        float hueX = sbX + sbWidth + sliderGap;
        float hueY = sbY;
        float hueHeight = sbHeight;

        drawHueGradient(hueX, hueY, sliderWidth, hueHeight, drawAlpha);

        float hueCursorY = hueY + hueHeight * hsb[0];
        RoundedUtil.drawRound(hueX - 1f, hueCursorY - 1.5f, sliderWidth + 2f, 3f, 1.5f,
            new Color(255, 255, 255, (int) (255 * drawAlpha)));

        float alphaX = hueX + sliderWidth + sliderGap;
        float alphaY = sbY;
        float alphaHeight = sbHeight;

        Color fullColor = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        drawAlphaGradient(alphaX, alphaY, sliderWidth, alphaHeight, fullColor, drawAlpha);

        float alphaCursorY = alphaY + alphaHeight * (1f - alpha);
        RoundedUtil.drawRound(alphaX - 1f, alphaCursorY - 1.5f, sliderWidth + 2f, 3f, 1.5f,
            new Color(255, 255, 255, (int) (255 * drawAlpha)));
    }

    private void drawSBGradient(float x, float y, float width, float height, Color baseColor, float alpha) {
        RoundedUtil.drawShaderGradientRound(x, y, width, height, 2f,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                baseColor);
    }

    private void drawHueGradient(float x, float y, float width, float height, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);

        int steps = 36;
        float stepSize = height / steps;
        for (int i = 0; i < steps; i++) {
            float hue = (float) i / (float) (steps - 1);
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f) | ((int)(alpha * 255) << 24);
            float rectY = y + i * stepSize;
            RenderUtil.rect(x, rectY, x + width, rectY + stepSize, rgb);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawAlphaGradient(float x, float y, float width, float height, Color color, float drawAlpha) {
        int light = new Color(255, 255, 255, (int)(drawAlpha * 255)).getRGB();
        int dark = new Color(128, 128, 128, (int)(drawAlpha * 255)).getRGB();
        float tile = 4f;
        for (float yy = 0; yy < height; yy += tile) {
            boolean offset = (((int) (yy / tile)) % 2) == 1;
            for (float xx = 0; xx < width; xx += tile) {
                boolean darkCell = ((int) (xx / tile) + (offset ? 1 : 0)) % 2 == 1;
                int col = darkCell ? dark : light;
                RenderUtil.rect(x + xx, y + yy, x + Math.min(xx + tile, width), y + Math.min(yy + tile, height), col);
            }
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int steps = 36;
        float stepSize = height / steps;
        int baseRgb = color.getRGB() & 0x00FFFFFF;
        for (int i = 0; i < steps; i++) {
            float a = 1f - ((float) i / (float) (steps - 1));
            int argb = ((int) (a * drawAlpha * 255) << 24) | baseRgb;
            float rectY = y + i * stepSize;
            RenderUtil.rect(x, rectY, x + width, rectY + stepSize, argb);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
                expanded = !expanded;
                if (expanded) updateHSBFromColor();
                return;
            }

            if (expanded && expandAnimation > 0.5f) {
                float pickerY = y + HEIGHT + 2f;
                float sliderWidth = 10f;
                float sliderGap = 3f;
                float totalSliderSpace = (sliderWidth * 2) + (sliderGap * 2);
                float sbX = x + 1f;
                float sbWidth = width - totalSliderSpace - 2f;
                float sbHeight = PICKER_HEIGHT;
                float hueX = sbX + sbWidth + sliderGap;
                float alphaX = hueX + sliderWidth + sliderGap;

                if (mouseX >= sbX && mouseX <= sbX + sbWidth && mouseY >= pickerY && mouseY <= pickerY + sbHeight) {
                    draggingPicker = true;
                    updateSB(mouseX, mouseY);
                }
                else if (mouseX >= hueX && mouseX <= hueX + sliderWidth && mouseY >= pickerY && mouseY <= pickerY + sbHeight) {
                    draggingHue = true;
                    updateHue(mouseY);
                }
                else if (mouseX >= alphaX && mouseX <= alphaX + sliderWidth && mouseY >= pickerY && mouseY <= pickerY + sbHeight) {
                    draggingAlpha = true;
                    updateAlpha(mouseY);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        draggingPicker = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY, int button) {
        if (draggingPicker) updateSB(mouseX, mouseY);
        if (draggingHue) updateHue(mouseY);
        if (draggingAlpha) updateAlpha(mouseY);
    }

    private void updateSB(int mouseX, int mouseY) {
        float pickerY = y + HEIGHT + 2f;
        float sliderWidth = 10f;
        float sliderGap = 3f;
        float totalSliderSpace = (sliderWidth * 2) + (sliderGap * 2);
        float sbX = x + 1f;
        float sbWidth = width - totalSliderSpace - 2f;
        float sbHeight = PICKER_HEIGHT;

        hsb[1] = MathHelper.clamp((mouseX - sbX) / sbWidth, 0f, 1f);
        hsb[2] = MathHelper.clamp(1f - (mouseY - pickerY) / sbHeight, 0f, 1f);
        applyColor();
    }

    private void updateHue(int mouseY) {
        float pickerY = y + HEIGHT + 2f;
        hsb[0] = MathHelper.clamp((mouseY - pickerY) / PICKER_HEIGHT, 0f, 1f);
        applyColor();
    }

    private void updateAlpha(int mouseY) {
        float pickerY = y + HEIGHT + 2f;
        alpha = MathHelper.clamp(1f - (mouseY - pickerY) / PICKER_HEIGHT, 0f, 1f);
        applyColor();
    }

    private void applyColor() {
        Color rgb = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        Color finalColor = new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), (int) (alpha * 255));
        property.setProperty(finalColor);
    }

    @Override
    public void onClose() {
        expanded = false;
    }
}

