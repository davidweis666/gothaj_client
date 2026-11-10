package team.gothaj.client.api.util.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;

@Getter
@Setter
@AllArgsConstructor
public class TimedPacket {
	private Packet packet;
	private long created;

}
