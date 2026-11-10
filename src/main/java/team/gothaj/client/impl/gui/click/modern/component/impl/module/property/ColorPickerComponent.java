package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.ColorProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;

import java.awt.*;

@SuppressWarnings("all")
public class ColorPickerComponent extends Component {

    private final ColorProperty parent;
    private boolean draggingSV, draggingHue, draggingAlpha;

    private static final float SQUARE_WIDTH = 76f;
    private static final float SQUARE_HEIGHT = 66f;
    private static final float LEFT_OFFSET = 5f;
    private static final float HUE_SLIDER_WIDTH = 9f;
    private static final float ALPHA_SLIDER_HEIGHT = 9f;
    private static final float SPACING = 5f;
    private static final float HEADER_HEIGHT = 11f;

    private static final int SV_STEPS = 36;
    private static final int HUE_STEPS = 36;
    private static final int ALPHA_STEPS = 36;

    public ColorPickerComponent(ColorProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
    }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution scaledResolution) {
        float renderY = y - 2.5f;
        IFontRenderer font = Fonts.nunito18;
        font.drawString(parent.getName(), x + LEFT_OFFSET - 1, renderY + 1, -1, true);
        float startY = renderY + HEADER_HEIGHT;
        float startX = x + LEFT_OFFSET;
        drawSVSquare(startX, startY);
        float hueX = startX + SQUARE_WIDTH + SPACING;
        drawHueSlider(hueX, startY);
        float alphaY = startY + SQUARE_HEIGHT + SPACING;
        drawAlphaSlider(startX, alphaY);
        if (draggingSV) updateSVFromMouse(mouseX, mouseY, startX, startY);
        if (draggingHue) updateHueFromMouse(mouseY, startY);
        if (draggingAlpha) updateAlphaFromMouse(mouseX, startX, alphaY);
    }

    private void drawSVSquare(float startX, float startY) {
        float hue = parent.getHue();
        RoundedUtil.drawShaderGradientRound(startX, startY, SQUARE_WIDTH, SQUARE_HEIGHT,
                3,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.getHSBColor(parent.getHue(), 1.0f, 1.0f));
        float currentX = startX + parent.getSaturation() * SQUARE_WIDTH;
        float currentY = startY + (1.0f - parent.getBrightness()) * SQUARE_HEIGHT;
        float boxSize = 3f;
        float halfBox = boxSize / 2f;
        RenderUtil.rect(currentX - halfBox, currentY - halfBox, currentX + halfBox, currentY + halfBox, 0xFFFFFFFF);
        int curRgb = Color.HSBtoRGB(parent.getHue(), parent.getSaturation(), parent.getBrightness()) | 0xFF000000;
        RenderUtil.rect(currentX - halfBox + 0.5f, currentY - halfBox + 0.5f, currentX + halfBox - 0.5f, currentY + halfBox - 0.5f, curRgb);
    }

    private void drawHueSlider(float startX, float startY) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        float stepSize = SQUARE_HEIGHT / HUE_STEPS;
        for (int i = 0; i < HUE_STEPS; i++) {
            float hue = (float) i / (float) (HUE_STEPS - 1);
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
            float rectY = startY + i * stepSize;
            RenderUtil.rect(startX, rectY, startX + HUE_SLIDER_WIDTH, rectY + stepSize, rgb);
        }
        float selectorY = startY + parent.getHue() * SQUARE_HEIGHT;
        RenderUtil.rect(startX - 1, selectorY - 0.5f, startX + HUE_SLIDER_WIDTH + 1, selectorY + 0.5f, 0xFFFFFFFF);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void drawAlphaSlider(float startX, float startY) {
        drawCheckeredBackground(startX, startY, startX + SQUARE_WIDTH, startY + ALPHA_SLIDER_HEIGHT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float stepSize = SQUARE_WIDTH / ALPHA_STEPS;
        int baseRgb = Color.HSBtoRGB(parent.getHue(), parent.getSaturation(), parent.getBrightness()) & 0x00FFFFFF;
        for (int i = 0; i < ALPHA_STEPS; i++) {
            float a = (float) i / (float) (ALPHA_STEPS - 1);
            int argb = ((int) (a * 255) << 24) | baseRgb;
            float rectX = startX + i * stepSize;
            RenderUtil.rect(rectX, startY, rectX + stepSize, startY + ALPHA_SLIDER_HEIGHT, argb);
        }
        float selectorX = startX + (parent.getAlpha() / 255.0f) * SQUARE_WIDTH;
        RenderUtil.rect(selectorX - 0.5f, startY - 1, selectorX + 0.5f, startY + ALPHA_SLIDER_HEIGHT + 1, 0xFFFFFFFF);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void drawCheckeredBackground(float x, float y, float x2, float y2) {
        int light = 0xFFFFFFFF;
        int dark = 0xFF808080;
        float tile = 4f;
        for (float yy = y; yy < y2; yy += tile) {
            boolean offset = (((int) ((yy - y) / tile)) % 2) == 1;
            for (float xx = x; xx < x2; xx += tile) {
                boolean darkCell = ((int) ((xx - x) / tile) + (offset ? 1 : 0)) % 2 == 1;
                int col = darkCell ? dark : light;
                Gui.drawRect(xx, yy, Math.min(xx + tile, x2), Math.min(yy + tile, y2), col);
            }
        }
    }

    private void updateSVFromMouse(int mouseX, int mouseY, float startX, float startY) {
        float relativeX = MathHelper.clamp(mouseX - startX, 0, SQUARE_WIDTH);
        float relativeY = MathHelper.clamp(mouseY - startY, 0, SQUARE_HEIGHT);
        float saturation = relativeX / SQUARE_WIDTH;
        float brightness = 1.0f - (relativeY / SQUARE_HEIGHT);
        parent.setSaturation(saturation);
        parent.setBrightness(brightness);
    }

    private void updateHueFromMouse(int mouseY, float startY) {
        float relativeY = MathHelper.clamp(mouseY - startY, 0, SQUARE_HEIGHT);
        float hue = relativeY / SQUARE_HEIGHT;
        parent.setHue(hue);
    }

    private void updateAlphaFromMouse(int mouseX, float startX, float startY) {
        float relativeX = MathHelper.clamp(mouseX - startX, 0, SQUARE_WIDTH);
        float alpha = relativeX / SQUARE_WIDTH;
        parent.setAlpha(Math.round(alpha * 255));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        float renderY = y - 2.5f;
        float startY = renderY + HEADER_HEIGHT;
        float startX = x + LEFT_OFFSET;
        if (MouseUtil.isHovered(mouseX, mouseY, startX, startY, startX + SQUARE_WIDTH, startY + SQUARE_HEIGHT)) {
            draggingSV = true;
            updateSVFromMouse(mouseX, mouseY, startX, startY);
        } else if (MouseUtil.isHovered(mouseX, mouseY, startX + SQUARE_WIDTH + SPACING, startY,
                startX + SQUARE_WIDTH + SPACING + HUE_SLIDER_WIDTH, startY + SQUARE_HEIGHT)) {
            draggingHue = true;
            updateHueFromMouse(mouseY, startY);
        } else if (MouseUtil.isHovered(mouseX, mouseY, startX, startY + SQUARE_HEIGHT + SPACING,
                startX + SQUARE_WIDTH, startY + SQUARE_HEIGHT + SPACING + ALPHA_SLIDER_HEIGHT)) {
            draggingAlpha = true;
            updateAlphaFromMouse(mouseX, startX, startY + SQUARE_HEIGHT + SPACING);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSV = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        float renderY = y - 2.5f;
        float totalWidth = LEFT_OFFSET + SQUARE_WIDTH + SPACING + HUE_SLIDER_WIDTH;
        float totalHeight = HEADER_HEIGHT + SQUARE_HEIGHT + SPACING + ALPHA_SLIDER_HEIGHT;
        return MouseUtil.isHovered(mouseX, mouseY, x, renderY, x + totalWidth, renderY + totalHeight);
    }

    @Override
    public boolean shouldUpdateWidth() {
        return true;
    }

    @Override
    public float getWidth() {
        return LEFT_OFFSET + SQUARE_WIDTH + SPACING + HUE_SLIDER_WIDTH;
    }

    @Override
    public float getHeight() {
        return HEADER_HEIGHT + SQUARE_HEIGHT + SPACING + ALPHA_SLIDER_HEIGHT + 4;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }
}