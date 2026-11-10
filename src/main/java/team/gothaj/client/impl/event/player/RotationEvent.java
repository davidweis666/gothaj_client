package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@Setter
@AllArgsConstructor
public class RotationEvent extends CancellableEvent {
	private float deltaX, deltaY, partialTicks;
}
