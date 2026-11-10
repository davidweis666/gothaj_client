package team.gothaj.client.impl.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class AntiFireball extends Module {

    public final NumberProperty range = new NumberProperty("Range", 5.0f, 1.0f, 7.0f, 0.1f);

    private boolean clicked;
    public static EntityFireball target;

    public AntiFireball() {
        super("AntiFireball", "", ModuleCategory.COMBAT);
        registerProperties(range);
    }


    @Listen
    public void onClick(MouseClickEvent event) {
        target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity ent : mc.theWorld.getLoadedEntityList()) {
            if (ent instanceof EntityFireball fireEnt && !fireEnt.isDead) {
                double dist = EntityUtil.getDistanceToEntityBox(fireEnt);

                Vec3 playerPos = mc.thePlayer.getPositionEyes(1.0f);
                Vec3 fireballPos = fireEnt.getPositionEyes(1.0f);
                Vec3 motion = new Vec3(fireEnt.motionX, fireEnt.motionY, fireEnt.motionZ);

                double dotToPlayer = motion.dotProduct(playerPos.subtract(fireballPos).normalize());

                if (dist <= range.getProperty() && dist < closestDist && dotToPlayer > 0.1 && mc.thePlayer.ticksExisted >= 10 && fireEnt.shootingEntity != mc.thePlayer) {
                    target = fireEnt;
                    closestDist = dist;
                }
            }
        }

        if (target == null) {
            reset();
            return;
        }

        float[] rots = RotationUtil.getRotations(target, false);
        RotationComponent.setRotations(rots[0], rots[1], 180, 180, true);

        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit == target) {
            mc.clickMouse();
            clicked = true;
        }
    }

    private void reset() {

        target = null;

        if (clicked && (!getCastedModule(Scaffold.class).isToggled() || (!getCastedModule(KillAura.class).isToggled() && KillAura.target == null))) {
            RotationComponent.setRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, MathUtil.random(15, 45), MathUtil.random(15, 45), true);
            clicked = false;
        }
    }


    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }
}
