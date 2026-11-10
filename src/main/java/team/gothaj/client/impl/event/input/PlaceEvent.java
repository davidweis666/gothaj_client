package team.gothaj.client.impl.event.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import team.gothaj.eventbus.impl.Event;

public class PlaceEvent extends Event {
	@Getter
	@Setter
	private BlockPos blockPos;
	@Getter
	@Setter
	private EnumFacing facing;

	public PlaceEvent(BlockPos pos, EnumFacing facing) {
		this.blockPos = pos;
		this.facing = facing;
	}
}
