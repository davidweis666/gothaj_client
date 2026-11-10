package team.gothaj.client.impl.event.player.move;

import lombok.Getter;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
public class PostMoveEvent extends Event {

	private final boolean wasUsedFlag;

	public PostMoveEvent(boolean wasUsedFlag) {
		this.wasUsedFlag = wasUsedFlag;
	}
}
