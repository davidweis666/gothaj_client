package team.gothaj.client.api.util.rotation.ai;


import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.util.math.MathUtil;

/**
 * Human-like AI aim system composed of layered behavioral models:
 * <ul>
 *   <li>Smooth pursuit with inertia (delayed tracking)</li>
 *   <li>Perlin micro-tremor (hand shake)</li>
 *   <li>Overshoot &amp; correction on fast transitions</li>
 *   <li>Wandering aim point within hitbox (natural drift)</li>
 *   <li>Velocity-based target prediction (leading)</li>
 *   <li>Distance-adaptive parameters</li>
 *   <li>Flick-then-stabilize behavior</li>
 * </ul>
 */
public final class AiRotationUtil {

    private final NoiseUtil noiseX = new NoiseUtil();
    private final NoiseUtil noiseY = new NoiseUtil();
    private final NoiseUtil noiseZ = new NoiseUtil();

    private Vec3 smoothedPos = null;
    private Vec3 smoothedVelocity = Vec3.ZERO;

    private Vec3 wanderOffset = Vec3.ZERO;
    private Vec3 wanderTarget = Vec3.ZERO;
    private long lastWanderChange = 0;
    private long wanderInterval = 350;

    private Vec3 lastTargetPos = null;
    private Vec3 lastTargetVelocity = Vec3.ZERO;
    private long lastUpdateTime = 0;
    private double noiseTime = 0;

    private double overshootDecay = 0;
    private Vec3 overshootDir = Vec3.ZERO;

    private double flickProgress = 1.0;
    private Vec3 flickOrigin = null;

    public double aimRange = 4.5;

    public AiRotationUtil() {}

    public void reset() {
        smoothedPos = null;
        smoothedVelocity = Vec3.ZERO;
        wanderOffset = Vec3.ZERO;
        wanderTarget = Vec3.ZERO;
        lastTargetPos = null;
        lastTargetVelocity = Vec3.ZERO;
        lastUpdateTime = 0;
        noiseTime = 0;
        overshootDecay = 0;
        overshootDir = Vec3.ZERO;
        flickProgress = 1.0;
        flickOrigin = null;
    }

    /**
     * Compute a legit-looking world-space aim position for the given target
     */
    public Vec3 getAimTarget(EntityPlayerSP player, Entity target) {
        if (target == null) {
            reset();
            return player.getPositionEyes(1.0f);
        }

        Vec3 eyePos = player.getPositionEyes(1.0f);
        AxisAlignedBB box = target.getEntityBoundingBox();
        long now = System.currentTimeMillis();
        double dt = lastUpdateTime == 0 ? 0.05 : Math.min((now - lastUpdateTime) / 1000.0, 0.1);
        lastUpdateTime = now;
        noiseTime += dt;

        double distToTarget = eyePos.distanceTo(box.getCenter());

        // --- 1. Base aim: clamp player eye Y within box, center XZ ---
        Vec3 baseTarget = MathUtil.clampingY(eyePos, box);

        // --- 2. Target velocity estimation and prediction ---
        Vec3 targetVelocity = Vec3.ZERO;
        if (lastTargetPos != null) {
            Vec3 rawVelo = target.getPositionVector().subtract(lastTargetPos);
            targetVelocity = lastTargetVelocity.multiply(0.6).add(rawVelo.multiply(0.4));
        }
        lastTargetPos = target.getPositionVector();
        lastTargetVelocity = targetVelocity;

        double predictionStrength = Math.max(0.3, Math.min(1.2, 0.4 + distToTarget * 0.08));
        Vec3 predictedTarget = baseTarget.add(targetVelocity.multiply(predictionStrength));
        predictedTarget = clampToBox(predictedTarget, box.expand(0.1, 0.1, 0.1));

        // --- 3. Wandering aim point within the hitbox ---
        if (now - lastWanderChange > wanderInterval) {
            double bodyBias = 0.25 + Math.random() * 0.5;
            wanderTarget = new Vec3(
                    (Math.random() - 0.5) * 0.6,
                    bodyBias,
                    (Math.random() - 0.5) * 0.6
            );
            wanderInterval = 200 + (long) (Math.random() * 400);
            lastWanderChange = now;
        }
        double wanderLerp = Math.max(0.01, Math.min(0.15, dt * 3.0));
        wanderOffset = lerpVec3(wanderOffset, wanderTarget, wanderLerp);

        double halfW = (box.maxX - box.minX) * 0.5;
        double halfH = (box.maxY - box.minY) * 0.5;
        double halfD = (box.maxZ - box.minZ) * 0.5;

        double wanderScale = Math.max(0.1, Math.min(2, 0.15 + distToTarget * 0.02));
        Vec3 wanderedTarget = predictedTarget.addVector(
                wanderOffset.xCoord * halfW * wanderScale,
                wanderOffset.yCoord * halfH * wanderScale,
                wanderOffset.zCoord * halfD * wanderScale
        );
        wanderedTarget = clampToBox(wanderedTarget, box);

        // --- 3.5. Distance-based bias ---
        Vec3 center = box.getCenter();
        double nDist = Math.max(0.0, Math.min(1.0, eyePos.distanceTo(wanderedTarget) / Math.max(aimRange, 0.1)));
        double dFactor = 1.0 - MathUtil.getBias(nDist, 0.5);
        double yFactor = (1.0 - dFactor) * 0.3;
        wanderedTarget = new Vec3(
                wanderedTarget.xCoord + (center.xCoord - wanderedTarget.xCoord) * dFactor,
                wanderedTarget.yCoord + (center.yCoord - wanderedTarget.yCoord) * yFactor,
                wanderedTarget.zCoord + (center.zCoord - wanderedTarget.zCoord) * dFactor
        );
        wanderedTarget = clampToBox(wanderedTarget, box);

        // --- 4. Initialize smoothed position (first tick or after reset) ---
        if (smoothedPos == null) {
            double d = eyePos.distanceTo(target.getPositionVector());
            smoothedPos = eyePos.add(player.getLookVec().multiply(d));
            flickOrigin = smoothedPos;
            flickProgress = 0.0;
        }

        // --- 5. Flick-then-stabilize on new target acquisition ---
        if (flickProgress < 1.0) {
            flickProgress = Math.min(flickProgress + dt * 3.5, 1.0);
            double ease = 1.0 - Math.pow(1.0 - flickProgress, 3.0);
            smoothedPos = lerpVec3(flickOrigin, wanderedTarget, ease);
            smoothedVelocity = wanderedTarget.subtract(smoothedPos).multiply(0.3);
            addTremor(distToTarget);
            return clampToBox(smoothedPos, box.expand(0.05, 0.05, 0.05));
        }

        // --- 6. Smooth pursuit with adaptive speed and inertia ---
        Vec3 error = wanderedTarget.subtract(smoothedPos);
        double errorDist = error.lengthVector();

        double baseSmoothFactor;
        if (errorDist > 1.5) {
            baseSmoothFactor = 0.55;
        } else if (errorDist > 0.5) {
            baseSmoothFactor = 0.35;
        } else if (errorDist > 0.15) {
            baseSmoothFactor = 0.22;
        } else {
            baseSmoothFactor = 0.12;
        }

        double closeFactor = Math.max(0.5, Math.min(1.5, 1.0 / (distToTarget * 0.5 + 0.5)));
        baseSmoothFactor *= closeFactor;

        Vec3 desiredVelo = error.multiply(baseSmoothFactor);
        double inertia = errorDist > 0.5 ? 0.55 : 0.7;
        smoothedVelocity = smoothedVelocity.multiply(inertia).add(desiredVelo.multiply(1.0 - inertia));
        smoothedPos = smoothedPos.add(smoothedVelocity);

        // --- 7. Overshoot on fast target movement ---
        double tgtSpeed = targetVelocity.lengthVector();
        if (tgtSpeed > 0.04) {
            double overshootGain = Math.max(0.0, Math.min(0.2, tgtSpeed * 0.8));
            overshootDecay = Math.min(overshootDecay + overshootGain * dt * 20, 0.12);
            overshootDir = lerpVec3(overshootDir, targetVelocity.normalize(), 0.3);
        } else {
            overshootDecay *= (1.0 - dt * 6.0);
        }
        if (overshootDecay > 0.001) {
            smoothedPos = smoothedPos.add(overshootDir.multiply(overshootDecay));
        }

        // --- 8. Perlin micro-tremor (hand shake) ---
        addTremor(distToTarget);

        // --- 9. Final clamp ---
        return clampToBox(smoothedPos, box.expand(0.05, 0.05, 0.05));
    }

    private void addTremor(double distToTarget) {
        double scale = 0.012;
        double tx = noiseX.noise(noiseTime * 3.8) * scale;
        double ty = noiseY.noise(noiseTime * 2.9 + 100) * scale;
        double tz = noiseZ.noise(noiseTime * 3.3 + 200) * scale;

        double multiplier = Math.max(0.25, Math.min(2.0, 1.2 / (distToTarget + 0.4)));
        smoothedPos = smoothedPos.addVector(tx * multiplier, ty * multiplier, tz * multiplier);
    }

    private static Vec3 clampToBox(Vec3 pos, AxisAlignedBB box) {
        return new Vec3(
                Math.max(box.minX, Math.min(box.maxX, pos.xCoord)),
                Math.max(box.minY, Math.min(box.maxY, pos.yCoord)),
                Math.max(box.minZ, Math.min(box.maxZ, pos.zCoord))
        );
    }

    private static Vec3 lerpVec3(Vec3 from, Vec3 to, double t) {
        return new Vec3(
                lerp(t, from.xCoord, to.xCoord),
                lerp(t, from.yCoord, to.yCoord),
                lerp(t, from.zCoord, to.zCoord)
        );
    }

    private static double lerp(double pct, double start, double end) {
        return start + pct * (end - start);
    }

}
