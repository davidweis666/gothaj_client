package team.gothaj.client.impl.event.player.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import team.gothaj.eventbus.impl.Event;

/**
 * @author vajzii
 * @since 2025
 */

@Getter
@Setter
@AllArgsConstructor
public class SpoofClientItemEvent extends Event {

    private ItemStack item;
}
