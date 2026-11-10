package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@Setter
@AllArgsConstructor
public class MoveFlyingEvent extends CancellableEvent {
	private float yaw, strafe, forward, friction;
}
