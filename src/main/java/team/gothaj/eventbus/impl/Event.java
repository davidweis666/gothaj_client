package team.gothaj.eventbus.impl;

import lombok.AllArgsConstructor;

/**
 * @author horizon
 * @since 8/7/2025
 */
@AllArgsConstructor
public class Event {
    private final boolean pre;

    protected Event() {
        this.pre = true;
    }

    public enum Stage {
        PRE,
        POST
    }
}
