package team.gothaj.client.impl.event.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@Setter
@AllArgsConstructor
public class PacketEvent extends CancellableEvent {

	private Packet<?> packet;

	private Direction direction;

	public enum Direction {
		SEND, RECEIVE
	}

}
