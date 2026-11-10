package team.gothaj.client.impl.event.player.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@Setter
@AllArgsConstructor
public class BlockCollisionEvent extends CancellableEvent {

    private AxisAlignedBB axisAlignedBB;
    private BlockPos pos;
    private final Block block;
    private int x, y, z;
}
