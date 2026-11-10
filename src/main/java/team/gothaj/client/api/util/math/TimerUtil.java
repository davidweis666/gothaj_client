package team.gothaj.client.api.util.math;

public class TimerUtil {

    private long millis = System.currentTimeMillis();

    public boolean hasTimeReached(final long ms, boolean autoReset) {
        if (System.currentTimeMillis() >= millis + ms) {
            if (autoReset)
                reset();

            return true;
        }
        return false;
    }

    public boolean hasTimeReached(final long ms, final long delay, boolean autoReset) {
        if (System.currentTimeMillis() >= ms + delay) {
            if (autoReset)
                reset();

            return true;
        }
        return false;
    }

    public boolean hasTimeReached(final long ms) {
        return hasTimeReached(ms, false);
    }


    public void reset() {
        millis = System.currentTimeMillis();
    }

    public long getTime() {
        return System.currentTimeMillis() - millis;
    }

}
