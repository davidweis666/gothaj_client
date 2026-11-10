package team.gothaj.client.api.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import team.gothaj.client.api.animation.Animation;
import team.gothaj.client.api.animation.Easing;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.render.RoundedUtil;
import team.gothaj.client.impl.module.visual.Hud;

import java.awt.*;

public class Notification {
    private final String name, description;

    private final Type type;

    private final TimerUtil timer = new TimerUtil();
    @Getter
    private final float width;
    private boolean finished;
    private final Animation animation = new Animation(Easing.EASE_IN_OUT_SINE, 200);
    private final long duration;

    private static final int BASE_HEIGHT = 27;

    public Notification(final String name, final String description, Type type) {
        this.name = name;
        this.description = description;
        width = Math.max(120, Fonts.rubik16.getStringWidth(description) + 35f);
        this.type = type;
        this.duration = 2000L;
    }

    public Notification(final String name, final String description, Type type, long duration) {
        this.name = name;
        this.description = description;
        width = Math.max(120, Fonts.rubik16.getStringWidth(description) + 35f);
        this.type = type;
        this.duration = duration;
    }

    public void renderShader(final float x, final float y) {
        RoundedUtil.drawShaderRound(x, y, width, BASE_HEIGHT, 6, Color.BLACK);
    }

    public void render(final float x, final float y) {
        long elapsed = timer.getTime();

        float fullWidth = width - 28;

        if (elapsed < duration) {
            if (animation.getEndPoint() != 1) {
                animation.setEndPoint(1);
                animation.reset();
            }
        } else {
            if (animation.getEndPoint() != 0) {
                animation.setEndPoint(0);
                animation.reset();
            }
        }

        animation.run(animation.getEndPoint());

        if (animation.getEndPoint() == 0 && animation.isFinished() && !finished) {
            finished = true;
        }

        float barProgress = 1f - Math.min(timer.getTime() / 2000f, 1f);
        float animatedWidth = fullWidth * barProgress;

        RoundedUtil.drawRound(x, y, width, BASE_HEIGHT, 6, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
        RoundedUtil.drawRound(x + 24F, y + 23, fullWidth, 1, 2, new Color(20, 20, 20, Hud.backgroundAlpha.getProperty().intValue()));
        RoundedUtil.drawGradientHorizontal(
                x + 24F, y + 23.5f, animatedWidth, 1, 2,
                new Color(type.getColor()).darker().darker(),
                new Color(type.getColor()).darker()
        );

        final int iconColor = type.getColor();
        Fonts.notifs28.drawString(type.getIconText(), x + 5.5F, y + Fonts.notifs28.getHeight(), iconColor);

        final int titleColor = new Color(210, 210, 210).getRGB();
        Fonts.rubik18Bold.drawString(name, x + 24.5F, y + 5.5F, titleColor);

        final int descriptionColor = new Color(156, 156, 156).getRGB();
        Fonts.rubik16.drawString(description, x + 24.5F, y + 15F, descriptionColor);
    }

    public boolean hasFinished() {
        return finished;
    }

    public double getHeight() {
        return BASE_HEIGHT * animation.getValue();
    }

    public double getBaseHeight() {
        return BASE_HEIGHT;
    }


    @AllArgsConstructor
    public enum Type {
        SUCCESS(new Color(78, 255, 78, 255).getRGB(), "s"),
        INFO(new Color(255, 255, 255).getRGB(), "i"),
        ERROR(new Color(188, 40, 40).getRGB(), "e"),
        WARNING(new Color(202, 170, 7).getRGB(), "w");

        @Getter
        private final int color;

        @Getter
        private final String iconText;

    }
}
