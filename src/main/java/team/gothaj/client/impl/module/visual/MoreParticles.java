package team.gothaj.client.impl.module.visual;

import net.minecraft.util.EnumParticleTypes;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.player.AttackEntityEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class MoreParticles extends Module {

    public final ListProperty particles = new ListProperty("Particles", "Crit", "Enchant Crit");

    public final NumberProperty multiplier = new NumberProperty("Multiplier", 1, 1, 10, 1);

    public MoreParticles() {
        super("MoreParticles", "", ModuleCategory.VISUAL);
        registerProperties(particles, multiplier);
    }

    @Listen
    public void onAttack(AttackEntityEvent event) {

        EnumParticleTypes type = particles.getOptionState("Crit") ? EnumParticleTypes.CRIT : particles.getOptionState("Enchant Crit") ? EnumParticleTypes.CRIT_MAGIC : null;

        // Use event entity — attacks from KillAura/TimerRange call attackEntityEvent(player, target) without
        // updating objectMouseOver; crosshair-based checks would skip particles entirely.
        if (type != null && event.getEntity() != null && event.getEntity().isEntityAlive()) {

                for (int i2 = 0; i2 < multiplier.getProperty(); ++i2) {
                    mc.effectRenderer.emitParticleAtEntity(event.getEntity(), type);

            }
        }
    }
}
