package team.gothaj.client.impl.event.player.move;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
public class PreMoveEvent extends Event {

	@Setter
	private boolean safeWalk;

	public PreMoveEvent(boolean safeWalk) {
		this.safeWalk = safeWalk;
	}
}
