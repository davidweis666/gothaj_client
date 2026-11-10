package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@AllArgsConstructor
public class AttackEntityEvent extends CancellableEvent {

    private EntityLivingBase entity;
}
