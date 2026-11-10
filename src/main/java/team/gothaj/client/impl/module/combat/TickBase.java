package team.gothaj.client.impl.module.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.property.properties.RangedNumberProperty;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.player.SimulatedPlayer;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TimeManipulateEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TickBase extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", "Past", "Past", "Adaptive");
    public final NumberProperty delay = new NumberProperty("Delay", 50, 0, 1000, 50);
    public final RangedNumberProperty range = new RangedNumberProperty("Range", 3.0f, 6.0f, 0.1f, 7f, 0.1f);
    public final NumberProperty maxTick = new NumberProperty("Ticks", 4, 1, 20, 1);

    public final NumberProperty chance = new NumberProperty("Chance", 100, 1, 100, 1);

    public final BooleanProperty auraOnly = new BooleanProperty("Aura Only", true);
    public final BooleanProperty fovCheck = new BooleanProperty("FOV Check", true);
    public final BooleanProperty hurtCheck = new BooleanProperty("Hurt Check", true);

    public TimerUtil timer = new TimerUtil();
    public int skippedTick = 0;
    private long shifted, previousTime;
    public boolean working;
    private final List<SimulationProcess> simulationProcesses = new ArrayList<>();
    public EntityPlayer target;

    public TickBase() {
        super("TickBase", "Manipulates game ticks for combat advantage", ModuleCategory.COMBAT);
        registerProperties(mode, range, delay, maxTick, chance, auraOnly, hurtCheck, fovCheck);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Override
    public void onEnable() {
        shifted = 0;
        previousTime = 0;
        skippedTick = 0;
        working = false;
        simulationProcesses.clear();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        shifted = 0;
        skippedTick = 0;
        working = false;
        simulationProcesses.clear();
        super.onDisable();
    }

    public EntityPlayer findTarget() {
        if (auraOnly.getProperty() && getCastedModule(KillAura.class).isToggled() && KillAura.target != null) {
            return KillAura.target instanceof EntityPlayer ? (EntityPlayer) KillAura.target : null;
        } else {

            EntityLivingBase ent = EntityUtil.getTarget(range.getRight() * 3);
            return ent instanceof EntityPlayer ? (EntityPlayer) ent : null;
        }
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        target = findTarget();
    }

    @Listen
    public void onTimeManipulate(TimeManipulateEvent event) {
        if (mode.is("Past") || mode.is("Adaptive")) {

            if (simulationProcesses.isEmpty() || target == null || (hurtCheck.getProperty() && mc.thePlayer.hurtTime != 0)) {
                target = null;
                shifted = 0;
                return;
            }

            if (auraOnly.getProperty() && KillAura.target == null) return;

            if (canStart() && timer.hasTimeReached(delay.getProperty().longValue())) {
                shifted += event.getTime() - previousTime;
            }

            if (shifted >= maxTick.getProperty().intValue() * (1000 / 20f)) {
                shifted = 0;
                timer.reset();
            }

            previousTime = event.getTime();
            event.setTime(event.getTime() - shifted);
        }
    }


    @Listen
    public void onMoveInput(MoveInputEvent event) {

        if (getCastedModule(Scaffold.class).isToggled() || (findTarget() == null || (findTarget() != null && findTarget().getDistanceToEntity(mc.thePlayer) > 10))) return;

        simulationProcesses.clear();

        SimulatedPlayer simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simulatedPlayer.rotationYaw = RotationComponent.isActive()
                ? RotationComponent.rotations[0]
                : mc.thePlayer.rotationYaw;

        int allowedTicks = skippedTick != 0 ? skippedTick : maxTick.getProperty().intValue();

        SimulatedPlayer.enterSpeculativeMovement();
        try {
            for (int i = 0; i < allowedTicks; i++) {
                simulatedPlayer.tick();
                simulationProcesses.add(new SimulationProcess(
                        simulatedPlayer.getPos(),
                        simulatedPlayer.isCollidedHorizontally,
                        simulatedPlayer.onGround
                ));
            }
        } finally {
            SimulatedPlayer.leaveSpeculativeMovement();
        }
    }

    public boolean canStart() {
        if (target == null) {
            return false;
        }

        if (Math.random() > chance.getProperty() / 100 || simulationProcesses.isEmpty()) {
            return false;
        }

        int selectedIndex = maxTick.getProperty().intValue() - 1;

        if (mode.is("Adaptive")) {
            int adaptiveIndex = getBestAdaptiveTickIndex();
            if (adaptiveIndex < 0) {
                skippedTick = 0;
                return false;
            }
            selectedIndex = adaptiveIndex;
            skippedTick = adaptiveIndex + 1;
        } else {
            if (simulationProcesses.size() < maxTick.getProperty().intValue()) {
                return false;
            }
            skippedTick = 0;
        }

        Vec3 predictedPos = simulationProcesses.get(selectedIndex).position;
        Vec3 currentPos = mc.thePlayer.getPositionVector();
        Vec3 targetPos = target.getPositionVector();

        double predictedDist = predictedPos.distanceTo(targetPos);
        double currentDist = currentPos.distanceTo(targetPos);

        boolean closerAfterSkip = mode.is("Adaptive") || predictedDist < currentDist;
        boolean inRange = predictedDist >= range.getLeft() && predictedDist <= range.getRight();
        boolean canSee = mc.thePlayer.canEntityBeSeen(target);
        boolean notCollided = !simulationProcesses.get(selectedIndex).isCollidedHorizontally;

        boolean scaffoldOff = !getCastedModule(Scaffold.class).isToggled();

        boolean rotationCheck = true;

        if (fovCheck.getProperty()) {
            float[] playerToTargetRots = RotationUtil.getAngles(target);
            float playerYawToTarget = Math.abs(RotationUtil.normalizeAngleDelta(mc.thePlayer.rotationYaw - playerToTargetRots[0]));
            boolean playerCanAim = playerYawToTarget <= 80;

            float[] targetToPlayerRots = RotationUtil.getAngles(mc.thePlayer);
            float targetYawToPlayer = Math.abs(RotationUtil.normalizeAngleDelta(target.rotationYaw - targetToPlayerRots[0]));
            boolean targetFacingPlayer = targetYawToPlayer <= 105;

            rotationCheck = (mode.is("Adaptive") || playerCanAim) && targetFacingPlayer;
        }

        boolean adaptiveBehindCheck = true;
        if (mode.is("Adaptive")) {
            adaptiveBehindCheck = isBehindTarget(predictedPos, target);
        }

        return rotationCheck && adaptiveBehindCheck && closerAfterSkip && inRange && canSee && notCollided && scaffoldOff;
    }

    private int getBestAdaptiveTickIndex() {
        int limit = Math.min(simulationProcesses.size(), maxTick.getProperty().intValue());
        if (limit <= 0) {
            return -1;
        }

        Vec3 targetPos = target.getPositionVector();
        Vec3 currentPos = mc.thePlayer.getPositionVector();
        double currentDist = currentPos.distanceTo(targetPos);

        int bestIndex = -1;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < limit; i++) {
            SimulationProcess process = simulationProcesses.get(i);
            Vec3 predictedPos = process.position;
            double predictedDist = predictedPos.distanceTo(targetPos);

            if (predictedDist < range.getLeft() || predictedDist > range.getRight()) {
                continue;
            }

            if (process.isCollidedHorizontally) {
                continue;
            }

            if (predictedDist >= currentDist) {
                continue;
            }

            double behindDot = getBehindDot(predictedPos, target);
            if (behindDot >= -0.08) {
                continue;
            }

            double score = (-behindDot) * 10.0 - (i * 0.35);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private boolean isBehindTarget(Vec3 predictedPos, EntityPlayer targetPlayer) {
        return getBehindDot(predictedPos, targetPlayer) < -0.08;
    }

    private double getBehindDot(Vec3 predictedPos, EntityPlayer targetPlayer) {
        double targetYawRad = Math.toRadians(targetPlayer.rotationYaw);
        Vec3 targetForward = new Vec3(-Math.sin(targetYawRad), 0.0, Math.cos(targetYawRad));
        Vec3 toPredicted = predictedPos.subtract(targetPlayer.getPositionVector()).normalize();
        return targetForward.dotProduct(toPredicted);
    }

    public record SimulationProcess(Vec3 position, boolean isCollidedHorizontally, boolean onGround) {
    }
}
