package team.gothaj.client.impl.event.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import team.gothaj.eventbus.impl.CancellableEvent;

/**
 * @author vajzii
 * @since 2025
 */
@Getter
@AllArgsConstructor
public class RenderNameTagEvent extends CancellableEvent {

    private Entity entity;
}
