package team.gothaj.client.impl.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.tileentity.TileEntity;
import team.gothaj.eventbus.impl.CancellableEvent;

/**
 * @author vajzii
 * @since 2025
 */

@Getter
@AllArgsConstructor
public class ModelChestRenderEvent extends CancellableEvent {

    private final TileEntity entity;
    private final Runnable chestRenderer;

    public void renderChest() {
        if (!isCancelled()) {
            chestRenderer.run();
        }
    }
}
