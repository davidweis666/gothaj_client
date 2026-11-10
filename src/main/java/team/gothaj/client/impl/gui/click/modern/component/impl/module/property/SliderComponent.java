package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private static final IFontRenderer font = Fonts.nunito18;
    private static final float COMPONENT_HEIGHT = 20f;
    private static final float LEFT_OFFSET = 5f;
    private static final float RIGHT_OFFSET = 5f;
    private static final float SLIDER_HEIGHT = 2f;
    private static final float KNOB_RADIUS = 2.5f;
    private static final float TEXT_SLIDER_SPACING = 4f;
    private static final float SCROLL_SPEED = 0.1f;
    private static final float SCROLL_PAUSE_TIME = 70f;

    private float animatedValue;
    private final NumberProperty parent;
    private boolean lock;

    private float sliderStart, sliderWidth, sliderY;

    private float visualProgress = 0f;
    private float dragAnim = 0f;
    private static final float PROGRESS_LERP = 0.25f;
    private static final float DRAG_LERP = 0.20f;

    private float nameScrollOffset = 0f;
    private float namePauseCounter = 0f;
    private boolean nameNeedsScrolling = false;

    private boolean typing;
    private String typed = "";

    public SliderComponent(NumberProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;
    }

    @Override
    public float getWidth() {
        return width;
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
        if (typing) {
            Keyboard.enableRepeatEvents(true);
        }

        float textY = y + 1.5f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        String valueText = typing ? typed : (parent.getType() == NumberProperty.Type.INT ?
                String.valueOf(parent.getProperty()).split("\\.")[0] :
                String.valueOf(parent.getProperty()));

        float valueTextWidth = font.getStringWidth(valueText);
        float nameTextWidth = font.getStringWidth(parent.getName());
        float nameMaxWidth = width - LEFT_OFFSET - valueTextWidth - RIGHT_OFFSET - 10f;

        nameNeedsScrolling = nameTextWidth > nameMaxWidth;

        float nameDrawX = x + LEFT_OFFSET;

        if (nameNeedsScrolling) {
            if (namePauseCounter < SCROLL_PAUSE_TIME) {
                namePauseCounter++;
                nameScrollOffset = 0f;
            } else {
                nameScrollOffset += SCROLL_SPEED;
                float maxScroll = nameTextWidth - nameMaxWidth + 10f;
                if (nameScrollOffset > maxScroll) {
                    nameScrollOffset = 0f;
                    namePauseCounter = 0f;
                }
            }
            nameDrawX = x + LEFT_OFFSET - nameScrollOffset;
        } else {
            nameScrollOffset = 0f;
            namePauseCounter = 0f;
        }

        ScissorStack.push(x + LEFT_OFFSET, y, x + LEFT_OFFSET + nameMaxWidth, y + COMPONENT_HEIGHT + 2, scaledResolution);
        font.drawString(parent.getName(), nameDrawX, textY, -1, true);
        if (nameNeedsScrolling && nameScrollOffset > 0) {
            float duplicateX = nameDrawX + nameTextWidth + 20f;
            font.drawString(parent.getName(), duplicateX, textY, -1, true);
        }
        ScissorStack.pop();

        font.drawString(valueText,
                x + width - font.getStringWidth(valueText) - RIGHT_OFFSET,
                textY, -1, true);

        if(!Mouse.isButtonDown(0))
            lock = false;

        sliderStart = x + LEFT_OFFSET;
        sliderWidth = width - LEFT_OFFSET - RIGHT_OFFSET;
        sliderY = y + font.getHeight() + TEXT_SLIDER_SPACING + 1;

        drag(mouseX, mouseY);

        float sliderProgress;
        if(parent.getType() == NumberProperty.Type.DOUBLE) {
            final float valueClamped = MathHelper.clamp(parent.getProperty(), parent.getMin(), parent.getMax());
            sliderProgress = (valueClamped - parent.getMin()) / (parent.getMax() - parent.getMin());
        } else {
            final float valueClamped = MathHelper.clamp(parent.getProperty().intValue(), parent.getMin(), parent.getMax());
            sliderProgress = (valueClamped - parent.getMin()) / (parent.getMax() - parent.getMin());
        }
        sliderProgress = MathHelper.clamp(sliderProgress, 0f, 1f);

        visualProgress += (sliderProgress - visualProgress) * PROGRESS_LERP;
        visualProgress = MathHelper.clamp(visualProgress, 0f, 1f);

        float targetDrag = lock ? 1f : 0f;
        dragAnim += (targetDrag - dragAnim) * DRAG_LERP;

        drawSlider(sliderStart, sliderY, sliderWidth * visualProgress, dragAnim);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0 && MouseUtil.isHovered(mouseX, mouseY,
                sliderStart - KNOB_RADIUS, sliderY - KNOB_RADIUS,
                sliderStart + sliderWidth + KNOB_RADIUS, sliderY + KNOB_RADIUS + SLIDER_HEIGHT)) {
            lock = true;
            drag(mouseX, mouseY);
        }

        if (mouseButton == 1) {
            float valueTextWidth = font.getStringWidth(String.valueOf(parent.getProperty()));
            float valueX = x + width - valueTextWidth - RIGHT_OFFSET;
            float valueY = y + 1.5f;

            if (MouseUtil.isHovered(mouseX, mouseY, valueX, valueY,
                    valueX + valueTextWidth, valueY + font.getHeight())) {
                typing = !typing;
                if (typing) {
                    typed = "...";
                } else {
                    typed = "";
                }
            }
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (typing) {
            if (typed.equals("...")) {
                typed = "";
            }
            if (keyCode == Keyboard.KEY_ESCAPE) {
                typing = false;
                typed = "";
                Keyboard.enableRepeatEvents(false);
                return;
            }


            if (Character.isDigit(key) || (key == '.' && !typed.contains(".") && parent.getType() == NumberProperty.Type.DOUBLE)) {
                typed += key;
            } else if (keyCode == Keyboard.KEY_BACK && !typed.isEmpty()) {
                typed = typed.substring(0, typed.length() - 1);
            } else if (keyCode == Keyboard.KEY_RETURN && !typed.isEmpty()) {
                try {
                    if (parent.getType() == NumberProperty.Type.DOUBLE) {
                        parent.setProperty(Float.parseFloat(typed));
                    } else {
                        parent.setProperty(Integer.parseInt(typed));
                    }
                    typing = false;
                    typed = "";
                    Keyboard.enableRepeatEvents(false);
                } catch (NumberFormatException e) {
                    typed = "";
                }
            }
            return;
        }
        super.keyTyped(key, keyCode);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if(lock)
            lock = false;
    }

    private void drag(int mouseX, int mouseY) {
        if (lock) {
            float clampedMouseX = MathHelper.clamp(mouseX, sliderStart, sliderStart + sliderWidth);
            float progress = (clampedMouseX - sliderStart) / sliderWidth;
            progress = MathHelper.clamp(progress, 0f, 1f);

            final float range = parent.getMax() - parent.getMin();
            float target = parent.getMin() + progress * range;

            if (progress >= 0.98f) {
                target = parent.getMax();
            } else if (progress <= 0.02f) {
                target = parent.getMin();
            } else {
                target = Math.round(target / parent.getIncrement()) * parent.getIncrement();
            }

            if (Float.isNaN(animatedValue) || animatedValue == 0f) {
                animatedValue = target;
            }

            final float smoothing = 0.4f;
            animatedValue += smoothing * (target - animatedValue);

            if (parent.getType() == NumberProperty.Type.DOUBLE) {
                float value = new BigDecimal(animatedValue)
                        .setScale(2, RoundingMode.HALF_UP)
                        .floatValue();
                parent.setProperty(MathHelper.clamp(value, parent.getMin(), parent.getMax()));
            } else {
                int value = new BigDecimal(animatedValue)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
                parent.setProperty(MathHelper.clamp(value, (int)parent.getMin(), (int)parent.getMax()));
            }
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {

    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, x, y, x + width, y + COMPONENT_HEIGHT);
    }

    private void drawSlider(final float x, final float y, final float progressWidth) {
        drawSlider(x, y, progressWidth, 0f);
    }

    private void drawSlider(final float x, final float y, final float progressWidth, final float dragT) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float trackH = SLIDER_HEIGHT + 1.5f * dragT;
        float trackY = y - (trackH - SLIDER_HEIGHT) / 2f;

        Color base1 = getPrimaryColor();
        Color base2 = getSecondaryColor();

        RoundedUtil.drawRound(x, trackY, sliderWidth, trackH, 3f, new Color(110, 110, 110, 50));

        RoundedUtil.drawGradientHorizontal(x, trackY, progressWidth, trackH, 3f, base1, base2);

        float knobSize = 4.5f + 2.0f * dragT;
        float knobX = x + progressWidth - knobSize / 2f;
        float knobY = trackY + (trackH - knobSize) / 2f;

        RoundedUtil.drawRound(knobX, knobY, knobSize, knobSize, knobSize / 2f, new Color(255,255,255,230));

        GL11.glPopMatrix();
    }

    private Color blend(Color a, Color b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int r = (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bch= (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bch, al);
    }

    @Override
    public boolean shouldLockDragging() {
        return lock;
    }

    @Override
    public boolean shouldUpdateWidth() {
        return true;
    }

    @Override
    public boolean isAcceptingKeypresses() {
        return typing;
    }

    @Override
    public void onGuiClosed() {
        typing = false;
        typed = "";
        Keyboard.enableRepeatEvents(false);
    }
}
