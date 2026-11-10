package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
@Setter
@AllArgsConstructor
public class AttackMotionEvent extends Event {

    private float yaw;
    private double multiplier;
    private boolean sprint;
}
