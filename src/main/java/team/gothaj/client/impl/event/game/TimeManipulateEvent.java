package team.gothaj.client.impl.event.game;

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
public class TimeManipulateEvent extends Event {

    private long time;
}
