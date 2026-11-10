package team.gothaj.client.api.animation;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.client.api.util.math.TimerUtil;

/**
 * This code is part of Liticane's Animation Library.
 *
 * @author Liticane
 * @since 22/03/2024
 */
@Getter
@Setter
public class Animation {

    private final TimerUtil timeUtil = new TimerUtil();
    private Easing easing;

    private long duration;
    private double startPoint, endPoint, value;

    private boolean finished = true;

    public Animation(final Easing easing, final long duration) {
        this.easing = easing;
        this.duration = duration;
    }

    public void run(final double endPoint) {
        if (this.endPoint != endPoint) {
            this.endPoint = endPoint;
            this.reset();
        } else {
            this.finished = timeUtil.hasTimeReached(duration);

            if (this.finished) {
                this.value = endPoint;
                return;
            }
        }

        final double newValue = this.easing.getFunction().apply(this.getProgress());

        if (this.value > endPoint) {
            this.value = this.startPoint - (this.startPoint - endPoint) * newValue;
        } else {
            this.value = this.startPoint + (endPoint - this.startPoint) * newValue;
        }
    }

    public double getProgress() {
        return (double) timeUtil.getTime() / (double) this.duration;
    }

    public void reset() {
        this.timeUtil.reset();
        this.startPoint = value;
        this.finished = false;
    }
}
