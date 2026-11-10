package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.PatternProperty;
import team.gothaj.client.api.property.properties.pattern.Vec2;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class PatternComponent extends Component {
    private static final IFontRenderer font = Fonts.nunito18;
    private final PatternProperty parent;

    @Getter
    private boolean expanded = false;

    private float animation = 0f;
    private static final float ANIMATION_SPEED = 0.20f;
    private static final float BASE_HEIGHT = 13f;
    private static final float EXPANDED_HEIGHT = 104f;

    private static final float CANVAS_PADDING = 10f;
    private static final float CANVAS_HEIGHT = 80f;
    private static final float BUTTON_HEIGHT = 15f;
    private static final float BUTTON_SPACING = 3f;

    private boolean isDragging = false;

    public PatternComponent(PatternProperty parent, float x, float y) {
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
            base += EXPANDED_HEIGHT * animation;
        }
        return base;
    }

    @Override
    public float getX() { return x; }
    @Override
    public float getY() { return y; }

    @Override
    public void draw(int mouseX, int mouseY, ScaledResolution sr) {
        float target = expanded ? 1f : 0f;
        animation += (target - animation) * ANIMATION_SPEED;
        if (Math.abs(animation - target) < 0.01f) animation = target;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        font.drawString("§7" + identifier, x + 5, y + 2, -1, true);
        String status = parent.getPatternPoints().size() + " pts";
        if (parent.isRecording()) status = "§cREC";
        font.drawString(status, x + width - 4 - font.getStringWidth(status), y + 2, -1, true);

        if (animation > 0.01f) {
            float yOffset = y + BASE_HEIGHT;
            float visibleHeight = animation * EXPANDED_HEIGHT;

            ScissorStack.push(x, yOffset, x + width, yOffset + visibleHeight, sr);

            float canvasY = yOffset + 5;
            float canvasHeight = CANVAS_HEIGHT * animation;
            float canvasX = x + CANVAS_PADDING;
            float maxCanvasWidth = width - CANVAS_PADDING * 2;

            float canvasWidth = Math.min(maxCanvasWidth, canvasHeight);

            if (canvasWidth < maxCanvasWidth) {
                canvasX += (maxCanvasWidth - canvasWidth) / 2f;
            }

            int bgAlpha = (int)(200 * animation);
            RenderUtil.rect(canvasX, canvasY, canvasX + canvasWidth,
                    canvasY + canvasHeight, ColorUtil.setAlpha(new Color(40, 40, 40).getRGB(), bgAlpha));

            if (animation > 0.4f) {
                renderGrid(canvasX, canvasY, canvasWidth, canvasHeight, (int)(80 * animation));
            }

            if (!parent.getPatternPoints().isEmpty() && animation > 0.4f) {
                renderPattern(canvasX, canvasY, canvasWidth, canvasHeight, (int)(255 * animation));
            }

            if (animation > 0.7f) {
                float buttonY = canvasY + canvasHeight + BUTTON_SPACING;
                renderControls(x + CANVAS_PADDING, buttonY, maxCanvasWidth, mouseX, mouseY, (int)(255 * animation));
            }

            ScissorStack.pop();
        }

        GL11.glPopMatrix();
    }

    private void renderGrid(float x, float y, float width, float height, int alpha) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(0.5f);

        GL11.glColor4f(0.5f, 0.5f, 0.5f, alpha / 255f * 0.3f);

        int gridLines = 8;

        for (int i = 1; i < gridLines; i++) {
            float lineX = x + (width * i / (float)gridLines);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(lineX, y);
            GL11.glVertex2f(lineX, y + height);
            GL11.glEnd();
        }

        for (int i = 1; i < gridLines; i++) {
            float lineY = y + (height * i / (float)gridLines);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(x, lineY);
            GL11.glVertex2f(x + width, lineY);
            GL11.glEnd();
        }

        GL11.glLineWidth(1.5f);
        GL11.glColor4f(1.0f, 0.0f, 0.0f, alpha / 255f * 0.8f);

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        float crosshairSize = 6f;

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(centerX - crosshairSize, centerY);
        GL11.glVertex2f(centerX + crosshairSize, centerY);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(centerX, centerY - crosshairSize);
        GL11.glVertex2f(centerX, centerY + crosshairSize);
        GL11.glEnd();

        GL11.glPointSize(3.0f);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(centerX, centerY);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void renderPattern(float canvasX, float canvasY, float canvasWidth, float canvasHeight, int alpha) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        if (parent.getSmoothedPattern().size() > 1) {
            GL11.glLineWidth(3.5f);
            GL11.glColor4f(0.2f, 1.0f, 0.3f, alpha / 255f * 0.85f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (Vec2 point : parent.getSmoothedPattern()) {
                float screenX = canvasX + (point.x * canvasWidth);
                float screenY = canvasY + (point.y * canvasHeight);
                GL11.glVertex2f(screenX, screenY);
            }
            GL11.glEnd();
        }

        if (!parent.getPatternPoints().isEmpty()) {
            GL11.glPointSize(4.0f);
            GL11.glColor4f(1.0f, 0.2f, 0.2f, alpha / 255f * 0.8f);
            GL11.glBegin(GL11.GL_POINTS);
            for (Vec2 point : parent.getPatternPoints()) {
                float screenX = canvasX + (point.x * canvasWidth);
                float screenY = canvasY + (point.y * canvasHeight);
                GL11.glVertex2f(screenX, screenY);
            }
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void renderControls(float x, float y, float totalWidth, int mouseX, int mouseY, int alpha) {
        float buttonWidth = (totalWidth - BUTTON_SPACING * 2) / 3f;

        boolean recordHovered = MouseUtil.isHovered(mouseX, mouseY, x, y, x + buttonWidth, y + BUTTON_HEIGHT);
        int recordColor = parent.isRecording() ?
                new Color(220, 50, 50).getRGB() : new Color(100, 100, 100).getRGB();
        RenderUtil.rect(x, y, x + buttonWidth, y + BUTTON_HEIGHT,
                ColorUtil.setAlpha(recordColor, recordHovered ? alpha : (int)(alpha * 0.7f)));
        font.drawCenteredString(parent.isRecording() ? "Stop" : "Draw",
                x + buttonWidth / 2f, y + 3, ColorUtil.setAlpha(Color.WHITE.getRGB(), alpha));

        float clearX = x + buttonWidth + BUTTON_SPACING;

        float saveX = clearX + buttonWidth + BUTTON_SPACING;
        boolean clearHovered = MouseUtil.isHovered(mouseX, mouseY, saveX, y, saveX + buttonWidth, y + BUTTON_HEIGHT);
        RenderUtil.rect(saveX, y, saveX + buttonWidth, y + BUTTON_HEIGHT,
                ColorUtil.setAlpha(new Color(180, 100, 50).getRGB(), clearHovered ? alpha : (int)(alpha * 0.7f)));
        font.drawCenteredString("Clear", saveX + buttonWidth / 2f, y + 3,
                ColorUtil.setAlpha(Color.WHITE.getRGB(), alpha));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            if (isMouseOverHeader(mouseX, mouseY)) {
                expanded = !expanded;
                return;
            }

            if (expanded && animation > 0.8f) {
                float canvasY = y + BASE_HEIGHT + 5;
                float canvasHeight = CANVAS_HEIGHT * animation;
                float canvasX = x + CANVAS_PADDING;
                float maxCanvasWidth = width - CANVAS_PADDING * 2;
                float canvasWidth = Math.min(maxCanvasWidth, canvasHeight);

                if (canvasWidth < maxCanvasWidth) {
                    canvasX += (maxCanvasWidth - canvasWidth) / 2f;
                }

                if (MouseUtil.isHovered(mouseX, mouseY, canvasX, canvasY,
                        canvasX + canvasWidth, canvasY + canvasHeight)) {
                    if (parent.isRecording()) {
                        isDragging = true;
                        addPointFromMouse(mouseX, mouseY, canvasX, canvasY, canvasWidth, canvasHeight);
                    }
                    return;
                }

                float buttonY = canvasY + canvasHeight + BUTTON_SPACING;
                float buttonWidth = (maxCanvasWidth - BUTTON_SPACING * 2) / 3f;
                float buttonX = x + CANVAS_PADDING;

                if (MouseUtil.isHovered(mouseX, mouseY, buttonX, buttonY,
                        buttonX + buttonWidth, buttonY + BUTTON_HEIGHT)) {
                    parent.setRecording(!parent.isRecording());
                    return;
                }

                float clearX = buttonX + buttonWidth + BUTTON_SPACING;

                float saveX = clearX + buttonWidth + BUTTON_SPACING;
                if (MouseUtil.isHovered(mouseX, mouseY, saveX, buttonY,
                        saveX + buttonWidth, buttonY + BUTTON_HEIGHT)) {
                    parent.clearPattern();
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            isDragging = false;
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDragging && parent.isRecording() && expanded && animation > 0.8f) {
            float canvasY = y + BASE_HEIGHT + 5;
            float canvasHeight = CANVAS_HEIGHT * animation;
            float canvasX = x + CANVAS_PADDING;
            float maxCanvasWidth = width - CANVAS_PADDING * 2;
            float canvasWidth = Math.min(maxCanvasWidth, canvasHeight);


            if (canvasWidth < maxCanvasWidth) {
                canvasX += (maxCanvasWidth - canvasWidth) / 2f;
            }

            if (MouseUtil.isHovered(mouseX, mouseY, canvasX, canvasY,
                    canvasX + canvasWidth, canvasY + canvasHeight)) {
                addPointFromMouse(mouseX, mouseY, canvasX, canvasY, canvasWidth, canvasHeight);
            }
        }
    }

    private void addPointFromMouse(int mouseX, int mouseY, float canvasX, float canvasY, float canvasWidth, float canvasHeight) {
        float normalizedX = (mouseX - canvasX) / canvasWidth;
        float normalizedY = (mouseY - canvasY) / canvasHeight;

        normalizedX = Math.max(0, Math.min(1, normalizedX));
        normalizedY = Math.max(0, Math.min(1, normalizedY));

        parent.addPoint(normalizedX, normalizedY);
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, x + width, y + BASE_HEIGHT);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        float h = getHeight();
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + h);
    }
}