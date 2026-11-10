package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.CancellableEvent;
import team.gothaj.eventbus.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class MotionEvent extends Event {

	private double posX, posY, posZ;
	private float yaw, pitch;
	private boolean onGround;
}
