package team.gothaj.client.api.util.render;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.animation.Animation;
import team.gothaj.client.api.animation.Easing;

@Getter
@Setter
public final class Translate {

    private float x, y;

    private Animation arrayListAnimation;

    public Translate(float x, float y) {
        this.x = x;
        this.y = y;

        this.arrayListAnimation = new Animation(Easing.EASE_OUT_BACK, 300);
        this.arrayListAnimation.setValue(0.0);
    }

    public void animate(float newX, float newY) {
        x = RenderUtil.animate(newX, x, 10.0F);
        y = RenderUtil.animate(newY, y, 10.0F);
    }

    public void animateSlide(boolean isEnabled) {
        arrayListAnimation.run(isEnabled ? 1.0 : 0.0);
    }
}
