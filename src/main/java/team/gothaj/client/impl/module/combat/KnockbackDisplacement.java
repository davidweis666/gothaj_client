package team.gothaj.client.impl.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.eventbus.Listen;

public class KnockbackDisplacement extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Static", "Static", "Void");

    private final ModeProperty direction = new ModeProperty("Direction", "Right", "Left", "Right", "Back", "Custom")
            .setVisible(() -> mode.is("Static"));

    private final NumberProperty yawOffset = new NumberProperty("Yaw offset", 90, 10, 180, 1)
            .setVisible(() -> mode.is("Static") && direction.is("Custom"));

    private final NumberProperty cooldown = new NumberProperty("Cooldown (ticks)", 1, 1, 40, 1);

    private final BooleanProperty polarClamp = new BooleanProperty("Polar Clamp", true);

    private final NumberProperty clampHalfWidth = new NumberProperty("Clamp Half-Width", 0.2f, 0.05f, 0.3f, 0.05f)
            .setVisible(() -> polarClamp.getProperty());

    private final NumberProperty offsetDist = new NumberProperty("Offset Distance", 0.15f, 0.0f, 0.4f, 0.01f);

    private enum State {
        IDLE, FLICKING, RESTORING
    }

    private State state = State.IDLE;
    private EntityLivingBase pendingTarget;
    private float flickYaw;
    private int sinceLastFlick = 0;
    private boolean sendingAttack = false;

    public KnockbackDisplacement() {
        super("Knockback Displacement",
                "Displaces the target's knockback.",
                ModuleCategory.COMBAT);
        registerProperties(mode, direction, yawOffset, cooldown, offsetDist, polarClamp, clampHalfWidth);
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

    private void reset() {
        state = State.IDLE;
        pendingTarget = null;
        flickYaw = 0f;
        sinceLastFlick = 0;
        sendingAttack = false;
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.SEND) return;
        if (!(event.getPacket() instanceof C02PacketUseEntity packet)) return;
        if (packet.getAction() != C02PacketUseEntity.Action.ATTACK) return;
        if (sendingAttack) return;

        if (state != State.IDLE || sinceLastFlick < cooldown.getProperty()) {
            return;
        }

        Entity entity = packet.getEntityFromWorld(mc.theWorld);
        if (!(entity instanceof EntityLivingBase living)) return;

        event.setCancelled(true);
        armFlick(living);
    }

    private void armFlick(EntityLivingBase target) {
        pendingTarget = target;
        float originalYaw = mc.thePlayer.rotationYaw;
        flickYaw = MathHelper.wrapAngleTo180_float(originalYaw + getFlickAngle());
        state = State.FLICKING;
    }

    @Listen
    public void onMoveInput(MoveInputEvent event) {
        if (state == State.FLICKING) {
            MovementUtil.correctMovement(event, flickYaw);
        }
    }

    @Listen
    public void onMotion(MotionEvent event) {
        if (sendingAttack) return;

        switch (state) {
            case FLICKING:
                event.setYaw(flickYaw);
                event.setPitch(mc.thePlayer.rotationPitch);

                double dist = offsetDist.getProperty();
                if (dist > 0.0) {
                    double rad = Math.toRadians(flickYaw);
                    event.setPosX(event.getPosX() - Math.sin(rad) * dist);
                    event.setPosZ(event.getPosZ() + Math.cos(rad) * dist);
                }

                mc.thePlayer.setSprinting(true);

                state = State.RESTORING;
                break;

            case RESTORING:
                if (pendingTarget != null) {
                    sendAttack(pendingTarget);
                    pendingTarget = null;
                }
                state = State.IDLE;
                sinceLastFlick = 0;
                break;

            case IDLE:
                sinceLastFlick++;
                break;
        }
    }

    private void sendAttack(EntityLivingBase target) {
        sendingAttack = true;
        mc.thePlayer.swingItem();
        mc.thePlayer.sendQueue.addToSendQueue(
                new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
        sendingAttack = false;
    }

    private float getFlickAngle() {
        float angle;
        if (mode.is("Void")) {
            angle = findVoidFlickAngle();
            if (Float.isNaN(angle)) angle = 90f;
        } else {
            switch (direction.getProperty()) {
                case "Left":   angle = -90f; break;
                case "Right":  angle = 90f; break;
                case "Back":   angle = 180f; break;
                case "Custom": angle = yawOffset.getProperty(); break;
                default:       angle = 90f; break;
            }
        }

        if (polarClamp.getProperty() && pendingTarget != null) {
            float maxOffset = maxClampedOffset(pendingTarget);
            if (angle > 0) angle = Math.min(angle, maxOffset);
            else angle = Math.max(angle, -maxOffset);
        }

        return angle;
    }

    private float findVoidFlickAngle() {
        if (mc.thePlayer == null || mc.theWorld == null || pendingTarget == null) return Float.NaN;

        double dx = pendingTarget.posX - mc.thePlayer.posX;
        double dz = pendingTarget.posZ - mc.thePlayer.posZ;
        final double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return Float.NaN;

        dx /= dist;
        dz /= dist;

        final double rightX = -dz, rightZ = dx;
        final double eyeY = pendingTarget.posY + pendingTarget.getEyeHeight();

        int leftVoidCount = 0, rightVoidCount = 0;

        for (int i = 1; i <= 12; i++) {
            final double off = i * 0.5;

            if (isOpenDown(pendingTarget.posX + rightX * off, eyeY, pendingTarget.posZ + rightZ * off))
                rightVoidCount++;

            if (isOpenDown(pendingTarget.posX - rightX * off, eyeY, pendingTarget.posZ - rightZ * off))
                leftVoidCount++;
        }

        if (leftVoidCount == 0 && rightVoidCount == 0) return Float.NaN;
        if (leftVoidCount == rightVoidCount) return Float.NaN;

        final boolean voidIsLeft = leftVoidCount > rightVoidCount;
        final double voidDx = voidIsLeft ? -rightX : rightX;
        final double voidDz = voidIsLeft ? -rightZ : rightZ;

        final float voidYaw = (float) Math.toDegrees(Math.atan2(-voidDx, voidDz));
        return MathHelper.wrapAngleTo180_float(voidYaw - mc.thePlayer.rotationYaw);
    }

    private boolean isOpenDown(double x, double y, double z) {
        return mc.theWorld.rayTraceBlocks(
                new Vec3(x, y, z), new Vec3(x, y - 10, z)) == null;
    }

    private float maxClampedOffset(EntityLivingBase target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.01) return 0.0f;
        return (float) Math.toDegrees(Math.atan(clampHalfWidth.getProperty() / dist));
    }

    @Override
    public String getSuffix() {
        return mode.is("Static") ? direction.getProperty() : mode.getProperty();
    }
}
