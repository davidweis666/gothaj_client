package team.gothaj.client.impl.event.player.block;

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
public class DamageBlockEvent extends Event {

    private float damage;

    private int delay;
}
