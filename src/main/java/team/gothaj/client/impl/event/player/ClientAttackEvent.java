package team.gothaj.client.impl.event.player;

import net.minecraft.entity.Entity;

public class ClientAttackEvent {
    private Entity entity;

    public ClientAttackEvent(final Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}