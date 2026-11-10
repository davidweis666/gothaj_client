package team.gothaj.client.impl.event.player;

import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@Setter
public class SlowDownEvent extends CancellableEvent {
	private float slowDown;
	private boolean shouldSprint, forceSlowDown;

	public SlowDownEvent(float slowDown, boolean forceSlowDown) {
		this.forceSlowDown = forceSlowDown;
        this.slowDown = slowDown;
	}
}