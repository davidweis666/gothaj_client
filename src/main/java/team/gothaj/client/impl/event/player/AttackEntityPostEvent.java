package team.gothaj.client.impl.event.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import team.gothaj.eventbus.impl.CancellableEvent;

@Getter
@AllArgsConstructor
public class AttackEntityPostEvent extends CancellableEvent {

    private EntityLivingBase entity;
}
