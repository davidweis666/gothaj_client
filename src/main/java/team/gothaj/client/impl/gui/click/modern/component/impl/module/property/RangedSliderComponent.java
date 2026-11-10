package team.gothaj.client.impl.gui.click.modern.component.impl.module.property;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import team.gothaj.client.api.property.properties.RangedNumberProperty;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.font.IFontRenderer;
import team.gothaj.client.api.util.other.MouseUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.gui.click.modern.component.Component;
import team.gothaj.client.impl.gui.click.modern.component.impl.module.property.stack.ScissorStack;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@SuppressWarnings("all")
public class RangedSliderComponent extends Component {
    private static final IFontRenderer font = Fonts.nunito18;
    private static final float COMPONENT_HEIGHT = 20f;
    private static final float LEFT_OFFSET = 5f;
    private static final float RIGHT_OFFSET = 5f;
    private static final float SLIDER_HEIGHT = 2f;
    private static final float KNOB_RADIUS = 2.5f;
    private static final float TEXT_SLIDER_SPACING = 4f;
    private static final float SCROLL_SPEED = 0.1f;
    private static final float SCROLL_PAUSE_TIME = 70f;

    private float animatedLeft, animatedRight;
    private final RangedNumberProperty parent;
    private boolean lockLeft, lockRight;

    private float sliderStart, sliderWidth, sliderY;

    private float visualLeftProgress = 0f;
    private float visualRightProgress = 1f;
    private static final float PROGRESS_LERP = 0.25f;

    private float nameScrollOffset = 0f;
    private float namePauseCounter = 0f;
    private boolean nameNeedsScrolling = false;

    public RangedSliderComponent(RangedNumberProperty parent, float x, float y) {
        super(x, y);
        this.parent = parent;

        float range = parent.getMax() - parent.getMin();
        this.animatedLeft = parent.getLeft();
        this.animatedRight = parent.getRight();
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
        float textY = y + 1.5f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        String leftText = parent.getType() == RangedNumberProperty.Type.INT ?
                String.valueOf((int)parent.getLeft()) :
                String.valueOf(parent.getLeft());

        String rightText = parent.getType() == RangedNumberProperty.Type.INT ?
                String.valueOf((int)parent.getRight()) :
                String.valueOf(parent.getRight());

        String rangeText = leftText + " - " + rightText;

        float rangeTextWidth = font.getStringWidth(rangeText);
        float nameTextWidth = font.getStringWidth(parent.getName());
        float nameMaxWidth = width - LEFT_OFFSET - rangeTextWidth - RIGHT_OFFSET - 2f;

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

        font.drawString(rangeText,
                x + width - font.getStringWidth(rangeText) - RIGHT_OFFSET,
                textY, -1, true);

        if(!Mouse.isButtonDown(0)) {
            lockLeft = false;
            lockRight = false;
        }

        sliderStart = x + LEFT_OFFSET;
        sliderWidth = width - LEFT_OFFSET - RIGHT_OFFSET;
        sliderY = y + font.getHeight() + TEXT_SLIDER_SPACING + 1;

        drag(mouseX, mouseY);

        float leftProgress = (parent.getLeft() - parent.getMin()) / (parent.getMax() - parent.getMin());
        float rightProgress = (parent.getRight() - parent.getMin()) / (parent.getMax() - parent.getMin());

        leftProgress = MathHelper.clamp(leftProgress, 0f, 1f);
        rightProgress = MathHelper.clamp(rightProgress, 0f, 1f);

        visualLeftProgress += (leftProgress - visualLeftProgress) * PROGRESS_LERP;
        visualRightProgress += (rightProgress - visualRightProgress) * PROGRESS_LERP;

        visualLeftProgress = MathHelper.clamp(visualLeftProgress, 0f, 1f);
        visualRightProgress = MathHelper.clamp(visualRightProgress, 0f, 1f);

        drawRangeSlider(sliderStart, sliderY, visualLeftProgress, visualRightProgress);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            float leftKnobX = sliderStart + sliderWidth * visualLeftProgress;
            float rightKnobX = sliderStart + sliderWidth * visualRightProgress;

            boolean onLeftKnob = MouseUtil.isHovered(mouseX, mouseY,
                    leftKnobX - KNOB_RADIUS * 2, sliderY - KNOB_RADIUS * 2,
                    leftKnobX + KNOB_RADIUS * 2, sliderY + KNOB_RADIUS * 2 + SLIDER_HEIGHT);

            boolean onRightKnob = MouseUtil.isHovered(mouseX, mouseY,
                    rightKnobX - KNOB_RADIUS * 2, sliderY - KNOB_RADIUS * 2,
                    rightKnobX + KNOB_RADIUS * 2, sliderY + KNOB_RADIUS * 2 + SLIDER_HEIGHT);

            boolean onSlider = MouseUtil.isHovered(mouseX, mouseY,
                    sliderStart, sliderY - 6, sliderStart + sliderWidth, sliderY + 12);

            if (onRightKnob) {
                lockRight = true;
            } else if (onLeftKnob) {
                lockLeft = true;
            } else if (onSlider) {
                boolean clickedLeftOfRange = mouseX < leftKnobX - KNOB_RADIUS * 2;
                boolean clickedRightOfRange = mouseX > rightKnobX + KNOB_RADIUS * 2;

                if (clickedLeftOfRange || clickedRightOfRange) {
                    float currentRangeSize = parent.getRight() - parent.getLeft();
                    float range = parent.getMax() - parent.getMin();

                    float clampedMouseX = MathHelper.clamp(mouseX, sliderStart, sliderStart + sliderWidth);
                    float progress = (clampedMouseX - sliderStart) / sliderWidth;
                    progress = MathHelper.clamp(progress, 0f, 1f);

                    float clickedValue = parent.getMin() + progress * range;

                    float newLeft, newRight;
                    if (clickedLeftOfRange) {
                        newLeft = clickedValue;
                        newRight = newLeft + currentRangeSize;

                        if (newRight > parent.getMax()) {
                            newRight = parent.getMax();
                            newLeft = newRight - currentRangeSize;
                        }
                    } else {
                        newRight = clickedValue;
                        newLeft = newRight - currentRangeSize;

                        if (newLeft < parent.getMin()) {
                            newLeft = parent.getMin();
                            newRight = newLeft + currentRangeSize;
                        }
                    }

                    newLeft = Math.round(newLeft / parent.getIncrement()) * parent.getIncrement();
                    newRight = Math.round(newRight / parent.getIncrement()) * parent.getIncrement();

                    newLeft = MathHelper.clamp(newLeft, parent.getMin(), parent.getMax());
                    newRight = MathHelper.clamp(newRight, parent.getMin(), parent.getMax());

                    if (parent.getType() == RangedNumberProperty.Type.DOUBLE) {
                        parent.setLeft(new BigDecimal(newLeft).setScale(2, RoundingMode.HALF_UP).floatValue());
                        parent.setRight(new BigDecimal(newRight).setScale(2, RoundingMode.HALF_UP).floatValue());
                    } else {
                        parent.setLeft(Math.round(newLeft));
                        parent.setRight(Math.round(newRight));
                    }

                    animatedLeft = parent.getLeft();
                    animatedRight = parent.getRight();
                }
            }

            if (parent.getLeft() == parent.getMax() && parent.getRight() == parent.getMax()) {
                if (onSlider && !onLeftKnob && !onRightKnob) {
                    lockLeft = true;
                    drag(mouseX, mouseY);
                }
            }

            drag(mouseX, mouseY);
        }
    }


    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        lockLeft = false;
        lockRight = false;
    }

    private void drag(int mouseX, int mouseY) {
        float range = parent.getMax() - parent.getMin();

        if (lockLeft) {
            float clampedMouseX = MathHelper.clamp(mouseX, sliderStart, sliderStart + sliderWidth);
            float progress = (clampedMouseX - sliderStart) / sliderWidth;
            progress = MathHelper.clamp(progress, 0f, 1f);

            float target = parent.getMin() + progress * range;

            if (progress >= 0.98f) {
                target = parent.getMax();
            } else if (progress <= 0.02f) {
                target = parent.getMin();
            } else {
                target = Math.round(target / parent.getIncrement()) * parent.getIncrement();
            }

            target = Math.min(target, parent.getRight());


            if (Float.isNaN(animatedLeft) || animatedLeft == 0f) {
                animatedLeft = target;
            }

            final float smoothing = 0.4f;
            animatedLeft += smoothing * (target - animatedLeft);

            if (parent.getType() == RangedNumberProperty.Type.DOUBLE) {
                float value = new BigDecimal(animatedLeft)
                        .setScale(2, RoundingMode.HALF_UP)
                        .floatValue();
                parent.setLeft(MathHelper.clamp(value, parent.getMin(), parent.getMax()));
            } else {
                int value = new BigDecimal(animatedLeft)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
                parent.setLeft(MathHelper.clamp(value, parent.getMin(), parent.getMax()));
            }
        }

        if (lockRight) {
            float clampedMouseX = MathHelper.clamp(mouseX, sliderStart, sliderStart + sliderWidth);
            float progress = (clampedMouseX - sliderStart) / sliderWidth;
            progress = MathHelper.clamp(progress, 0f, 1f);

            float target = parent.getMin() + progress * range;

            if (progress >= 0.98f) {
                target = parent.getMax();
            } else if (progress <= 0.02f) {
                target = parent.getMin();
            } else {
                target = Math.round(target / parent.getIncrement()) * parent.getIncrement();
            }

            target = Math.max(target, parent.getLeft());

            if (Float.isNaN(animatedRight) || animatedRight == 0f) {
                animatedRight = target;
            }

            final float smoothing = 0.4f;
            animatedRight += smoothing * (target - animatedRight);

            if (parent.getType() == RangedNumberProperty.Type.DOUBLE) {
                float value = new BigDecimal(animatedRight)
                        .setScale(2, RoundingMode.HALF_UP)
                        .floatValue();
                parent.setRight(MathHelper.clamp(value, parent.getMin(), parent.getMax()));
            } else {
                int value = new BigDecimal(animatedRight)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
                parent.setRight(MathHelper.clamp(value, (int)parent.getMin(), (int)parent.getMax()));
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

    private void drawRangeSlider(final float x, final float y, final float leftProgress, final float rightProgress) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float trackY = y;

        RoundedUtil.drawRound(x, trackY, sliderWidth, SLIDER_HEIGHT, 3f, new Color(110, 110, 110, 50));

        float rangeStartX = x + sliderWidth * leftProgress;
        float rangeWidth = sliderWidth * (rightProgress - leftProgress);

        Color base1 = getPrimaryColor();
        Color base2 = getSecondaryColor();

        RoundedUtil.drawGradientHorizontal(rangeStartX, trackY, rangeWidth, SLIDER_HEIGHT, 3f, base1, base2);

        float knobSize = 4.5f;
        float leftKnobX = rangeStartX - knobSize / 2f;
        float leftKnobY = trackY + (SLIDER_HEIGHT - knobSize) / 2f;

        RoundedUtil.drawRound(leftKnobX, leftKnobY, knobSize, knobSize, knobSize / 2f, new Color(255,255,255,230));

        float rightKnobX = rangeStartX + rangeWidth - knobSize / 2f;
        float rightKnobY = trackY + (SLIDER_HEIGHT - knobSize) / 2f;

        RoundedUtil.drawRound(rightKnobX, rightKnobY, knobSize, knobSize, knobSize / 2f, new Color(255,255,255,230));

        GL11.glPopMatrix();
    }

    @Override
    public boolean shouldLockDragging() {
        return lockLeft || lockRight;
    }

    @Override
    public boolean shouldUpdateWidth() {
        return true;
    }
}
