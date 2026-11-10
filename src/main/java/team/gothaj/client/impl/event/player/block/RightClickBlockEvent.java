package team.gothaj.client.impl.event.player.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.BlockPos;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
@AllArgsConstructor
public class RightClickBlockEvent extends Event {

    private BlockPos pos;
}
