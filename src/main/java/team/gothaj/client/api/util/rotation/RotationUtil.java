package team.gothaj.client.api.util.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import team.gothaj.client.api.property.properties.PatternProperty;
import team.gothaj.client.api.property.properties.pattern.AimPattern;
import team.gothaj.client.api.property.properties.pattern.Vec2;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.math.FastNoiseLite;
import team.gothaj.client.api.util.player.RayCastUtil;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.main.GothajClient;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RotationUtil implements Utils {

	public static EntityLivingBase lastTarget;

	private static float smoothedYawDelta;
	private static float smoothedPitchDelta;

	public void resetSmooth() {
		smoothedYawDelta = 0.0F;
		smoothedPitchDelta = 0.0F;
	}
	
	public static boolean isAngleClose(float current, float intended, float errorRate) {
		return MathHelper.wrapAngleTo180_float(Math.abs(current - intended)) <= errorRate;
	}

	public static float smoothYaw(float currentYaw, float previousYaw, float factor) {
		factor = MathHelper.clamp(factor, 0.0F, 1.0F);

		float delta = MathHelper.wrapAngleTo180_float(currentYaw - previousYaw);

		smoothedYawDelta += (delta - smoothedYawDelta) * factor;

		return smoothedYawDelta;
	}

	public static float smoothPitch(float currentPitch, float previousPitch, float factor) {
		factor = MathHelper.clamp(factor, 0.0F, 1.0F);

		float delta = currentPitch - previousPitch;

		smoothedPitchDelta += (delta - smoothedPitchDelta) * factor;

		return smoothedPitchDelta;
	}

	public static float applyYaw(float yaw, float currentYaw, float previousYaw, float factor) {
		return yaw + smoothYaw(currentYaw, previousYaw, factor);
	}

	public static float applyPitch(float pitch, float currentPitch, float previousPitch, float factor) {
		return pitch + smoothPitch(currentPitch, previousPitch, factor);
	}

	public static float[] patternRotations(EntityLivingBase target, PatternProperty rotationPattern, PatternState state,
			double minAimHeight, double maxAimHeight, boolean rayTraceRots, long lastHit, boolean rawRots) {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		state.tickCounter++;

		if (rayTraceRots
				&& RayCastUtil.canHitCustom(new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch },
						target, KillAura.interactRange.getProperty())) {
			state.patternIndex++;
			return new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch };
		}

		if (!rawRots) {
			double retentionNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.07f, 10.0f) + 1.0) * 0.5;
			double retentionChance = 0.02 + retentionNoise * 0.05;

			if (tlr.nextDouble() < retentionChance && state.lastPatternYaw != null) {
				float microDrift = (float) (tlr.nextGaussian() * 0.03 * (1.0 + state.sessionFatigue * 0.2));
				// Keep index moving during retention frames so pattern does not pin to one
				// spot.
				state.patternIndex++;
				return new float[] { state.lastPatternYaw + microDrift, state.lastPatternPitch + microDrift * 0.6f };
			}
		}

		Vec3 currentTargetPos = new Vec3(target.posX, target.posY, target.posZ);
		Vec3 aimTarget = currentTargetPos;

		if (state.lastTarget != target) {
			state.entropyState = System.nanoTime() ^ (target.getEntityId() * 31L);
			state.simplexNoise.SetSeed((int) (state.entropyState & 0x7FFFFFFF));

			long startSeed = state.entropyState ^ (System.currentTimeMillis() / 1000);

			state.patternIndex = (int) (startSeed % 10000);
			if (state.patternIndex < 0)
				state.patternIndex += 10000;

			state.patternSeed = state.entropyState;
			state.lastTarget = target;
			state.predictionConfidence = 0;
			state.lastPlayerEyeY = null;
			state.jumpFix = false;

			double reactionNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.01f, 20.0f) + 1.0) * 0.5;
			double skillFactor = state.baseReactionSkill * state.microFocusLevel;
			state.reactionDelayTicks = (int) (2 + reactionNoise * (8 - skillFactor * 4));
			state.targetSwitchIndex = state.patternIndex;

			state.concentrationCycle = 0;
			state.microFocusLevel = 0.35 + reactionNoise * 0.45;
		}

		if (state.patternIndex % 40 == 0 && !rawRots) {
			double reactionNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.01f, 20.0f) + 1.0) * 0.5;
			double skillFactor = state.baseReactionSkill * state.microFocusLevel;
			state.reactionDelayTicks = (int) (2 + reactionNoise * (8 - skillFactor * 4));
		}

		if (!rawRots && state.patternIndex - state.targetSwitchIndex < state.reactionDelayTicks) {
			if (state.lastPatternYaw != null) {
				float driftNoise = state.simplexNoise.GetNoise(state.patternIndex * 0.25f, 30.0f);
				float driftYaw = state.lastPatternYaw + driftNoise * 0.6f;
				float driftPitch = state.lastPatternPitch + driftNoise * 0.3f;
				state.lastTargetPos = currentTargetPos;
				state.patternIndex++;
				return new float[] { driftYaw, driftPitch };
			}
		}

		if (state.lastTargetPos != null && state.secondLastTargetPos != null) {
			Vec3 velocity = currentTargetPos.subtract(state.lastTargetPos);

			float distanceToTarget = mc.thePlayer.getDistanceToEntity(target);

			boolean targetJumping = velocity.yCoord > 0.2 && !target.onGround;

			boolean recentHit = (System.currentTimeMillis() - lastHit) < 500;

			int distBucket = getDistanceBucket(distanceToTarget);
			int strafeBucket = getStrafeBucket(velocity);
			int contextIndex = getContextIndex(distBucket, strafeBucket, targetJumping, recentHit);

			state.lastContextIndex = contextIndex;

			Vec3 acceleration = velocity
					.subtract(state.lastTargetVelocity != null ? state.lastTargetVelocity : velocity);

			double movementSpeed = velocity.lengthVector();

			if (movementSpeed > 0.08 && !rawRots && state.enableMovementPrediction) {
				float distanceFactor = (float) (Math.log(distanceToTarget / 0.5 + 1) / Math.log(2));

				double predictionThreshold = 0.10 + state.microFocusLevel * 0.18 + state.baseReactionSkill * 0.08;
				if (tlr.nextDouble() > (1.0 - predictionThreshold)) {
					int predictionTicks = (int) (1.5 + state.microFocusLevel * 3.5 * Math.min(distanceFactor, 1.8));

					Vec3 predictedPos = currentTargetPos;
					Vec3 currentVel = velocity;
					for (int i = 0; i < predictionTicks; i++) {
						currentVel = new Vec3(currentVel.xCoord * 0.91 + acceleration.xCoord * 0.85,
								currentVel.yCoord * 0.98 + acceleration.yCoord * 0.85 - 0.08,
								currentVel.zCoord * 0.91 + acceleration.zCoord * 0.85);
						predictedPos = predictedPos.addVector(currentVel.xCoord, currentVel.yCoord, currentVel.zCoord);
					}

					if (state.predictionConfidence < 15)
						state.predictionConfidence++;

					double distanceWeight = Math.max(0.3, 1.0 - Math.abs(distanceFactor - 1.2) * 0.4);
					double predictionWeight = Math.min(state.predictionConfidence / 15.0, 0.75) * distanceWeight;

					aimTarget = new Vec3(
							currentTargetPos.xCoord
									+ (predictedPos.xCoord - currentTargetPos.xCoord) * predictionWeight,
							currentTargetPos.yCoord
									+ (predictedPos.yCoord - currentTargetPos.yCoord) * predictionWeight,
							currentTargetPos.zCoord
									+ (predictedPos.zCoord - currentTargetPos.zCoord) * predictionWeight);

					state.predictedTargetPos = aimTarget;
				}
			}
			state.lastTargetVelocity = velocity;

			if (state.enableAdaptiveLearning) {
				state.lastProfileIndex = selectProfile(state, contextIndex);
				PatternState.AimProfile p = state.profiles[state.lastProfileIndex];
				state.currentFatigueMul = p.fatigueMul;
			}
		}

		state.secondLastTargetPos = state.lastTargetPos;

		double adjustedAimHeight;

		if (rawRots) {
			adjustedAimHeight = (minAimHeight + maxAimHeight) / 2.0;
		} else {
			if (state.brownMotionHeight == 0) {
				state.brownMotionHeight = 0.45 + tlr.nextDouble() * 0.12;
			}

			double brownNoise = state.simplexNoise.GetNoise(state.patternIndex * 0.09f, 50.0f);
			double brownStep = brownNoise * 0.11;
			state.brownMotionHeight += brownStep;

			if (state.brownMotionHeight > 0.83) {
				state.brownMotionHeight -= tlr.nextDouble() * 0.14;
			} else if (state.brownMotionHeight < 0.17) {
				state.brownMotionHeight += tlr.nextDouble() * 0.14;
			}

			state.brownMotionHeight = Math.max(0.0, Math.min(1.0, state.brownMotionHeight));

			double intentNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.03f, 60.0f) + 1.0) * 0.5;
			if (intentNoise > 0.94) {
				double targetHeight = intentNoise > 0.97 ? 0.87 : 0.52;
				double pullStrength = (0.27 + state.microFocusLevel * 0.18) * (1.0 - state.sessionFatigue * 0.2);
				state.brownMotionHeight += (targetHeight - state.brownMotionHeight) * pullStrength;
			}

			adjustedAimHeight = minAimHeight + (maxAimHeight - minAimHeight) * state.brownMotionHeight;
		}

		if (!rawRots) {
			state.concentrationCycle++;
			state.timeSinceSessionStart = (System.currentTimeMillis() - state.sessionStartTime) / 1000.0;
			state.circadianPhase = (state.timeSinceSessionStart / 120.0) % (GothajClient.getInstance().getPI() * 2);

			double circadianEffect = (Math.sin(state.circadianPhase) + 1.0) * 0.04;
			double momentumEffect = (state.consecutiveHits - state.consecutiveMisses * 0.5) * 0.01;
			momentumEffect = Math.max(-0.12, Math.min(0.12, momentumEffect));

			state.momentumConfidence += momentumEffect * 0.03;
			state.momentumConfidence = Math.max(0.3, Math.min(0.95, state.momentumConfidence));

			double fatigueRate = (state.simplexNoise.GetNoise(state.concentrationCycle * 0.0008f, 70.0f) + 1.0) * 0.5;
			state.sessionFatigue += 0.00012 * (0.6 + fatigueRate * 0.8) * state.currentFatigueMul
					* (1.0 - circadianEffect * 0.5);
			if (state.sessionFatigue > 1.0)
				state.sessionFatigue = 1.0;

			double focusNoise = state.simplexNoise.GetNoise(state.concentrationCycle * 0.004f, 80.0f);
			double focusChange = focusNoise * 0.015 * (1.0 - state.sessionFatigue * 0.3) + circadianEffect * 0.15
					+ state.momentumConfidence * 0.02;

			double baseline = 0.5 + state.baseReactionSkill * 0.1;
			double decayToBaseline = (baseline - state.microFocusLevel) * 0.003;

			state.microFocusLevel += focusChange + decayToBaseline;
			state.microFocusLevel = Math.max(0.35, Math.min(0.92, state.microFocusLevel));

			if (tlr.nextDouble() < 0.0005) {
				if (tlr.nextDouble() < 0.4) {
					state.microFocusLevel = Math.max(0.35, state.microFocusLevel - tlr.nextDouble(0.08, 0.18));
					state.sessionFatigue = Math.min(0.85, state.sessionFatigue + tlr.nextDouble(0.05, 0.12));
				} else {
					state.microFocusLevel = Math.min(0.88, state.microFocusLevel + tlr.nextDouble(0.12, 0.28));
					state.sessionFatigue = Math.max(0, state.sessionFatigue - tlr.nextDouble(0.12, 0.28));
				}
			}
		}

		if (rotationPattern != null && !rotationPattern.getSmoothedPattern().isEmpty()) {
			rotationPattern.setCurrentPattern(state.currentPattern);
		}

		final Vec3 eyes = new Vec3(mc.thePlayer.posX,
				mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

		Vec3 targetVec = getTargetEntityPoint(target, eyes, state);

		Vec3 entPos = new Vec3(aimTarget.xCoord, target.posY, aimTarget.zCoord);
		AxisAlignedBB entityBox = target.getEntityBoundingBox();
		double entHeight = target.height;

		Vec3 preferredAimVec = new Vec3(aimTarget.xCoord, target.posY + (entHeight * adjustedAimHeight),
				aimTarget.zCoord);
		boolean preferredAimVisible = mc.theWorld.rayTraceBlocks(eyes, preferredAimVec) == null;

		if (preferredAimVisible) {
			// Target is visible - use getNearestPositionInBB for accurate first-hit
			AxisAlignedBB constrainedBB = new AxisAlignedBB(entityBox.minX, target.posY + (entHeight * minAimHeight),
					entityBox.minZ, entityBox.maxX, target.posY + (entHeight * maxAimHeight), entityBox.maxZ);

			Vec3 nearestPos = getNearestPositionInBB(eyes, constrainedBB);

			// Apply brownian height preference
			double preferredY = target.posY + (entHeight * adjustedAimHeight);
			preferredY = MathHelper.clamp(preferredY, constrainedBB.minY, constrainedBB.maxY);
			targetVec = new Vec3(nearestPos.xCoord, preferredY, nearestPos.zCoord);

		} else {
			// Target is blocked - use existing visibility search
			List<Double> visibleHeights = new ArrayList<>();
			for (double y2 = entityBox.minY; y2 <= entityBox.maxY; y2 += 0.03) {
				Vec3 targetVecY = new Vec3(aimTarget.xCoord, y2, aimTarget.zCoord);
				if (mc.theWorld.rayTraceBlocks(eyes, targetVecY) == null) {
					visibleHeights.add(y2 - target.posY);
				}
			}
			if (!visibleHeights.isEmpty()) {
				double preferredY = entHeight * adjustedAimHeight;

				List<Double> candidates = new ArrayList<>();
				for (double visibleY : visibleHeights) {
					double relHeight = visibleY / entHeight;
					if (relHeight >= minAimHeight && relHeight <= maxAimHeight) {
						candidates.add(visibleY);
					}
				}

				List<Double> finalPool = candidates.isEmpty() ? visibleHeights : candidates;

				double closestY = finalPool.getFirst();
				double minDifference = Math.abs(closestY - preferredY);
				for (double visibleY : finalPool) {
					double difference = Math.abs(visibleY - preferredY);
					if (difference < minDifference) {
						minDifference = difference;
						closestY = visibleY;
					}
				}
				adjustedAimHeight = closestY / entHeight;
			}
		}

		double diffX = targetVec.xCoord - eyes.xCoord;
		double diffZ = targetVec.zCoord - eyes.zCoord;

		double currentEyeY = eyes.yCoord;

		if (state.lastPlayerEyeY == null) {
			state.lastPlayerEyeY = currentEyeY;
		}

		double eyeYDelta = currentEyeY - state.lastPlayerEyeY;
		boolean isJumping = eyeYDelta > 0.1 && !mc.thePlayer.onGround;

		double diffY;

		if (isJumping && !state.jumpFix && !rawRots) {
			double jumpReactionChance = (0.45 + state.baseReactionSkill * 0.35 + state.microFocusLevel * 0.25)
					* (1.0 - state.sessionFatigue * 0.18);
			if (tlr.nextDouble() < jumpReactionChance) {
				state.jumpFix = true;
				int jumpDuration = (int) (8 + (1.0 - state.baseReactionSkill) * 12);
				state.jumpTrackingStrength = (float) (0.65 + state.baseReactionSkill * 0.25);
			}
		}

		if (state.jumpFix && !rawRots) {
			diffY = targetVec.yCoord - eyes.yCoord + eyeYDelta * state.jumpTrackingStrength;
			state.lastPlayerEyeY += (currentEyeY - state.lastPlayerEyeY)
					* tlr.nextDouble(0.12 * state.jumpTrackingStrength, 0.18 * state.jumpTrackingStrength);
		} else {
			diffY = targetVec.yCoord - eyes.yCoord;
			if (!rawRots) {
				state.lastPlayerEyeY += (currentEyeY - state.lastPlayerEyeY) * tlr.nextDouble(0.26, 0.34);
			}
		}

		double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

		float baseYaw = (float) MathHelper.wrapAngleTo180_double(
				(float) (Math.atan2(diffZ, diffX) * 180.0D / GothajClient.getInstance().getPI()) - 90.0F);
		float basePitch = (float) MathHelper.wrapAngleTo180_double(
				(float) (-(Math.atan2(diffY, dist) * 180.0D / GothajClient.getInstance().getPI())));

		if (rotationPattern != null && !rotationPattern.getSmoothedPattern().isEmpty()) {
			PatternState.AimProfile profile = state.profiles[state.lastProfileIndex];

			float effectiveSpeed = (float) Math.max(0.1f, state.patternSpeed);
			int patternIndexToUse = (int) (state.patternIndex * effectiveSpeed) % 10000;

			float distance = (float) EntityUtil.getDistanceToEntityBox(target);
			Vec2 patternOffset = rotationPattern.getRotationAtIndex(patternIndexToUse, state.patternSeed,
					state.sessionFatigue, state.microFocusLevel, state.baseReactionSkill, distance, rawRots);

			float yawOffset = (patternOffset.x - 0.5f) * state.patternYawRange * profile.yawRangeMul
					* state.adaptiveRangeMul;
			float pitchOffset = (patternOffset.y - 0.5f) * state.patternPitchRange * profile.pitchRangeMul
					* state.adaptiveRangeMul;

			if (rawRots) {

				effectiveSpeed = (float) state.patternSpeed;
				patternIndexToUse = (int) (state.patternIndex * effectiveSpeed) % 10000;

				distance = (float) EntityUtil.getDistanceToEntityBox(target);
				patternOffset = rotationPattern.getRotationAtIndex(patternIndexToUse, state.patternSeed,
						state.sessionFatigue, state.microFocusLevel, state.baseReactionSkill, distance, rawRots);

				yawOffset = (patternOffset.x - 0.5f) * state.patternYawRange;
				pitchOffset = (patternOffset.y - 0.5f) * state.patternPitchRange;

				float cleanYaw = baseYaw + yawOffset;
				float cleanPitch = MathHelper.clamp(basePitch + pitchOffset, -90.0f, 90.0f);

				state.lastTargetPos = currentTargetPos;
				state.lastPatternYaw = cleanYaw;
				state.lastPatternPitch = cleanPitch;
				state.patternIndex++;

				return new float[] { cleanYaw, cleanPitch };
			}

			float jitterScale = Math.min(1.0f, distance / 4.0f);

			float jitterSeed1 = state.patternIndex * 0.18f;
			float jitterSeed2 = state.patternIndex * 0.27f;

			float jitterBase = (1.0f + (float) state.sessionFatigue * 0.45f) * jitterScale * profile.jitterMul;
			float focusDamping = (float) (1.05 - state.microFocusLevel * 0.15);

			double perlinLayer1 = state.simplexNoise.GetNoise(jitterSeed1, 90.0f) * 0.17f;
			double perlinLayer2 = state.simplexNoise.GetNoise(jitterSeed2, 95.0f) * 0.10f;
			double gaussianNoise = tlr.nextGaussian() * 0.08f;
			double uniformNoise = (tlr.nextDouble() - 0.5) * 0.05f;

			float microJitterYaw = (float) (perlinLayer1 + perlinLayer2 + gaussianNoise + uniformNoise) * jitterBase
					* focusDamping;

			double perlinPitchLayer1 = state.simplexNoise.GetNoise(jitterSeed1, 100.0f) * 0.14f;
			double perlinPitchLayer2 = state.simplexNoise.GetNoise(jitterSeed2, 105.0f) * 0.09f;
			double gaussianPitchNoise = tlr.nextGaussian() * 0.07f;
			double uniformPitchNoise = (tlr.nextDouble() - 0.5) * 0.04f;

			float microJitterPitch = (float) (perlinPitchLayer1 + perlinPitchLayer2 + gaussianPitchNoise
					+ uniformPitchNoise) * jitterBase * focusDamping * 0.85f;

			double burstNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.05f, 110.0f) + 1.0) * 0.5;
			double burstThreshold = 0.92 - state.microFocusLevel * 0.12 + state.sessionFatigue * 0.08;

			if (burstNoise > burstThreshold) {
				float burstIntensity = (float) ((burstNoise - burstThreshold) / (1.0 - burstThreshold));
				double momentumMod = 1.0 + (state.momentumConfidence - 0.5) * 0.3;
				float burstMagnitude = (0.28f + tlr.nextFloat() * 0.55f) * (1.0f + (float) state.sessionFatigue * 0.35f)
						* jitterScale * (1.15f - (float) state.microFocusLevel * 0.25f) * (0.7f + burstIntensity * 0.6f)
						* (float) momentumMod;

				double gaussianBurstYaw = tlr.nextGaussian() * burstMagnitude * 0.7;
				double uniformBurstYaw = (tlr.nextDouble() - 0.5) * burstMagnitude * 0.3;
				microJitterYaw += (float) (gaussianBurstYaw + uniformBurstYaw);

				double gaussianBurstPitch = tlr.nextGaussian() * burstMagnitude * 0.5;
				double uniformBurstPitch = (tlr.nextDouble() - 0.5) * burstMagnitude * 0.22;
				microJitterPitch += (float) (gaussianBurstPitch + uniformBurstPitch);
			}

			float rawYaw = baseYaw + yawOffset + microJitterYaw;
			float rawPitch = MathHelper.clamp(basePitch + pitchOffset + microJitterPitch, -90.0f, 90.0f);

			float finalYaw = rawYaw;
			float finalPitch = rawPitch;

			if (state.lastPatternYaw != null && state.lastPatternPitch != null) {
				float yawDelta = normalizeAngleDelta(rawYaw - state.lastPatternYaw);
				float pitchDelta = rawPitch - state.lastPatternPitch;

				finalYaw = humanizedSmooth(state.lastPatternYaw, rawYaw, distance, Math.abs(yawDelta), state, true);
				finalPitch = humanizedSmooth(state.lastPatternPitch, rawPitch, distance, Math.abs(pitchDelta), state,
						false);

				double overshootNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.04f, 120.0f) + 1.0) * 0.5;
				double overshootChance = (0.055 + state.sessionFatigue * 0.055) * (1.25 - state.microFocusLevel * 0.35)
						* profile.overshootMul;

				if (overshootNoise > (1.0 - overshootChance) && state.patternIndex > 5) {
					float overshootFactor = (float) ((0.016 + tlr.nextDouble() * 0.038)
							* (1.0f + (float) state.sessionFatigue * 0.28f)
							* (1.12f - (float) state.microFocusLevel * 0.18f));
					finalYaw += (finalYaw - state.lastPatternYaw) * overshootFactor * -1;
				}

				float deltaYaw = Math.abs(finalYaw - state.lastPatternYaw);
				float deltaPitch = Math.abs(finalPitch - state.lastPatternPitch);
				state.accumulatedPath += (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
			}

			float avgStep = state.patternIndex > 0 ? state.accumulatedPath / state.patternIndex : 0f;

			if (state.patternIndex > 20) {
				float stepDeg = state.targetStepDeg;
				if (avgStep > stepDeg * 1.2f) {
					profile.jitterMul *= 0.995f;
				} else if (avgStep < stepDeg * 0.8f) {
					profile.jitterMul *= 1.005f;
				}
				profile.jitterMul = Math.max(0.6f, Math.min(1.4f, profile.jitterMul));
			}

			double hesitationNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.032f, 130.0f) + 1.0) * 0.5;
			double hesitationChance = (0.045 + state.sessionFatigue * 0.048) * (1.18 - state.microFocusLevel * 0.28);

			if (hesitationNoise > (1.0 - hesitationChance)) {
				float hesitationStrength = 0.48f + tlr.nextFloat() * 0.28f;
				finalYaw = state.lastPatternYaw != null
						? state.lastPatternYaw + (finalYaw - state.lastPatternYaw) * hesitationStrength
						: finalYaw;
				finalPitch = state.lastPatternPitch != null
						? state.lastPatternPitch + (finalPitch - state.lastPatternPitch) * hesitationStrength
						: finalPitch;
			}

			state.totalRotationAttempts++;

			boolean canHit = RayCastUtil.canHitCustom(new float[] { finalYaw, finalPitch }, target,
					KillAura.interactRange.getProperty());

			if (!canHit) {
				state.rotationCorrections++;

				float correctionStrength = 0.4f + (float) state.microFocusLevel * 0.3f;

				float directYawDelta = normalizeAngleDelta(baseYaw - finalYaw);
				float directPitchDelta = basePitch - finalPitch;

				finalYaw += directYawDelta * correctionStrength;
				finalPitch += directPitchDelta * correctionStrength;

				boolean stillCantHit = !RayCastUtil.canHitCustom(new float[] { finalYaw, finalPitch }, target,
						KillAura.range.getProperty());

				if (stillCantHit) {
					for (float str = 0.5f; str <= 1.0f; str += 0.1f) {
						float testYaw = finalYaw + directYawDelta * str;
						float testPitch = finalPitch + directPitchDelta * str;

						if (RayCastUtil.canHitCustom(new float[] { testYaw, testPitch }, target,
								KillAura.range.getProperty())) {
							finalYaw = testYaw;
							finalPitch = testPitch;
							break;
						}
					}
				}
			}

			if (state.totalRotationAttempts > 0 && state.totalRotationAttempts % 50 == 0) {
				float correctionRate = (float) state.rotationCorrections / state.totalRotationAttempts;

				if (correctionRate > 0.15f) {
					state.adaptiveRangeMul *= 0.95f;
					state.adaptiveRangeMul = Math.max(0.5f, state.adaptiveRangeMul);
				} else if (correctionRate < 0.05f) {
					state.adaptiveRangeMul *= 1.02f;
					state.adaptiveRangeMul = Math.min(1.0f, state.adaptiveRangeMul);
				}
			}

			state.lastPatternYaw = finalYaw;
			state.lastPatternPitch = finalPitch;
			state.patternIndex++;

			state.lastTargetPos = currentTargetPos;

			return new float[] { finalYaw, finalPitch };
		}

		state.lastTargetPos = currentTargetPos;

		if (rawRots) {
			state.patternIndex++;
			return new float[] { baseYaw, basePitch };
		}

		float fallbackYaw = baseYaw + (float) ((tlr.nextDouble() - 0.5) * 0.16);
		float fallbackPitch = basePitch + (float) ((tlr.nextDouble() - 0.5) * 0.12);
		state.patternIndex++;
		return new float[] { fallbackYaw, fallbackPitch };
	}

	public static float normalizeAngleDelta(float delta) {
		delta = delta % 360.0f;
		if (delta > 180.0f) {
			return delta - 360.0f;
		} else if (delta < -180.0f) {
			return delta + 360.0f;
		}
		return delta;
	}

	public static void updateGlobalSkill(PatternState state, boolean hit) {
		state.totalEngagements++;
		if (hit) {
			state.totalSuccessfulHits++;
			state.consecutiveHits++;
			state.consecutiveMisses = Math.max(0, state.consecutiveMisses - 1);
		} else {
			state.consecutiveMisses++;
			state.consecutiveHits = Math.max(0, state.consecutiveHits - 1);
		}

		state.consecutiveHits = Math.min(state.consecutiveHits, 12);
		state.consecutiveMisses = Math.min(state.consecutiveMisses, 12);

		double hitRate = state.totalEngagements > 0 ? (double) state.totalSuccessfulHits / state.totalEngagements : 0.5;

		long sessionTime = System.currentTimeMillis() - state.sessionStartTime;
		double experienceMinutes = sessionTime / 60000.0;
		double experienceFactor = Math.min(1.0, experienceMinutes / 30.0);

		double engagementFactor = Math.min(1.0, state.totalEngagements / 500.0);

		double totalExperience = (experienceFactor + engagementFactor) / 2.0;

		double learningRate = 0.0015 * (1.0 - totalExperience * 0.5);

		double performanceBonus = (hitRate - 0.5) * 0.3;
		double targetSkill = state.skillFloor
				+ (state.skillCeiling - state.skillFloor) * (0.5 + totalExperience * 0.3 + performanceBonus);

		state.baseReactionSkill += (targetSkill - state.baseReactionSkill) * learningRate;
		state.baseReactionSkill = Math.max(state.skillFloor, Math.min(state.skillCeiling, state.baseReactionSkill));

		if (!hit && state.totalEngagements % 10 == 0) {
			state.baseReactionSkill *= 0.998;
		}
	}

	private static float getAdaptiveSpeed(float distance, float angleDelta, PatternState state) {
		float baseSpeed;

		if (distance < 1.5f) {
			baseSpeed = 18.0f + (1.5f - distance) * 4.0f;
		} else if (distance < 2.5f) {
			baseSpeed = 15.0f;
		} else if (distance < 4.0f) {
			baseSpeed = 12.0f;
		} else if (distance < 6.5f) {
			baseSpeed = 10.0f;
		} else {
			baseSpeed = 8.0f;
		}

		angleDelta = Math.abs(angleDelta);

		if (angleDelta < 1.0f) {
			baseSpeed *= 0.85f;
		} else if (angleDelta < 3.0f) {
			baseSpeed *= 1.1f;
		} else if (angleDelta < 6.0f) {
			baseSpeed *= 1.3f;
		} else if (angleDelta < 12.0f) {
			baseSpeed *= 1.6f;
		} else if (angleDelta < 45.0f) {
			baseSpeed *= 2.0f;
		}

		float skillMod = (float) (0.88 + state.baseReactionSkill * 0.34);
		float focusMod = (float) (0.85 + state.microFocusLevel * 0.40);
		float fatigueMod = (float) (1.0 - state.sessionFatigue * 0.18);

		return baseSpeed * skillMod * focusMod * fatigueMod;
	}

	private static float humanizedSmooth(float current, float target, float distance, float angleDelta,
			PatternState state, boolean isYaw) {

		float delta = target - current;
		if (isYaw) {
			delta = normalizeAngleDelta(delta);
		}

		float absDelta = Math.abs(delta);
		float baseSpeed = getAdaptiveSpeed(distance, angleDelta, state);

		float progress = 1.0f / (1.0f + (float) Math.exp(-absDelta * 0.62f + 1.4f));
		float speed = baseSpeed * (1.40f + progress * 0.95f);

		float tremorScale = Math.min(1.0f, absDelta / 27.0f);
		float tremorSeed = state.patternIndex * 0.22f;
		float tremor = (state.simplexNoise.GetNoise(tremorSeed, 140.0f) * 0.09f
				+ state.simplexNoise.GetNoise(tremorSeed + 1.4f, 145.0f) * 0.05f) * tremorScale
				* (1.0f + (float) state.sessionFatigue * 0.3f);

		float momentum = (isYaw ? state.lastDeltaYaw : state.lastDeltaPitch) * 0.32f;

		float result = current + Math.signum(delta) * Math.min(absDelta, speed) + tremor + momentum;

		float microCorrection = 0f;
		if (absDelta < 0.5f) {
			float scaled = absDelta / 15.0f;
			float correctionNoise = state.simplexNoise.GetNoise(state.patternIndex * 0.35f, isYaw ? 170.0f : 175.0f);
			float targetProximity = Math.min(1.0f, distance / 3.5f);
			microCorrection = correctionNoise * 0.15f * (1.0f - targetProximity)
					* (1.0f - (float) state.sessionFatigue * 0.15f) * (1.1f - (float) state.microFocusLevel * 0.2f)
					* scaled;
		}
		result += microCorrection;

		if (isYaw) {
			state.lastDeltaYaw = result - current;
		} else {
			state.lastDeltaPitch = result - current;
		}

		return result;
	}

	private static Vec3 getTargetEntityPoint(EntityLivingBase target, Vec3 eyes, PatternState state) {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();
		AxisAlignedBB bb = target.getEntityBoundingBox();

		double centerX = (bb.minX + bb.maxX) / 2;
		double centerZ = (bb.minZ + bb.maxZ) / 2;

		Vec3 targetVelocity = new Vec3(0, 0, 0);
		if (state.lastTargetPos != null) {
			targetVelocity = new Vec3(target.posX - state.lastTargetPos.xCoord,
					target.posY - state.lastTargetPos.yCoord, target.posZ - state.lastTargetPos.zCoord);
		}

		if (state.aimBiasX == null) {
			state.aimBiasX = (tlr.nextFloat() - 0.5f) * 0.3f;
			state.aimBiasZ = (tlr.nextFloat() - 0.5f) * 0.3f;
		}

		if (state.patternIndex % 100 == 0) {
			state.aimBiasX += (tlr.nextFloat() - 0.5f) * 0.02f;
			state.aimBiasZ += (tlr.nextFloat() - 0.5f) * 0.02f;
			state.aimBiasX = Math.max(-0.35f, Math.min(0.35f, state.aimBiasX));
			state.aimBiasZ = Math.max(-0.35f, Math.min(0.35f, state.aimBiasZ));
		}

		double velocityMag = Math
				.sqrt(targetVelocity.xCoord * targetVelocity.xCoord + targetVelocity.zCoord * targetVelocity.zCoord);

		double leadX = 0.0;
		double leadZ = 0.0;

		if (velocityMag > 0.05) {
			double leadFactor = state.baseReactionSkill * 0.4;
			double velocityScale = Math.min(1.5, velocityMag / 0.2);
			leadX = targetVelocity.xCoord * leadFactor * 2.0 * velocityScale;
			leadZ = targetVelocity.zCoord * leadFactor * 2.0 * velocityScale;
		}

		double targetX = centerX + (bb.maxX - bb.minX) * state.aimBiasX + leadX;
		double targetZ = centerZ + (bb.maxZ - bb.minZ) * state.aimBiasZ + leadZ;

		if (state.lastAimPointX == null) {
			state.lastAimPointX = targetX;
			state.lastAimPointZ = targetZ;
		}

		double memorySpeed = 0.25 + state.microFocusLevel * 0.15 - state.sessionFatigue * 0.1;
		state.lastAimPointX += (targetX - state.lastAimPointX) * memorySpeed;
		state.lastAimPointZ += (targetZ - state.lastAimPointZ) * memorySpeed;

		double aimX = state.lastAimPointX;
		double aimZ = state.lastAimPointZ;

		double aimNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.06f, 150.0f) + 1.0) * 0.5;
		double skillHeadBias = state.baseReactionSkill * 0.25;

		Vec3 headPoint = new Vec3(aimX, bb.minY + target.height * 0.85, aimZ);
		Vec3 torsoPoint = new Vec3(aimX, bb.minY + target.height * 0.55, aimZ);
		Vec3 lowerPoint = new Vec3(aimX, bb.minY + target.height * 0.32, aimZ);

		Vec3 preferredPoint;
		if (aimNoise < (0.52 + skillHeadBias)) {
			preferredPoint = headPoint;
		} else if (aimNoise < 0.82) {
			preferredPoint = torsoPoint;
		} else {
			preferredPoint = lowerPoint;
		}

		double distance = eyes.distanceTo(new Vec3(target.posX, target.posY, target.posZ));
		double distanceScale = Math.max(0.5, Math.min(1.5, distance / 3.0));

		double errorScale = (0.12 + state.sessionFatigue * 0.12) * (1.15 - state.microFocusLevel * 0.25)
				* distanceScale;
		double errorX = (tlr.nextDouble() - 0.5) * (bb.maxX - bb.minX) * errorScale;
		double errorY = (tlr.nextDouble() - 0.5) * target.height * (errorScale * 0.85);
		double errorZ = (tlr.nextDouble() - 0.5) * (bb.maxZ - bb.minZ) * errorScale;

		Vec3 humanizedPoint = new Vec3(preferredPoint.xCoord + errorX, preferredPoint.yCoord + errorY,
				preferredPoint.zCoord + errorZ);

		double clampedX = Math.max(bb.minX, Math.min(bb.maxX, humanizedPoint.xCoord));
		double clampedY = Math.max(bb.minY, Math.min(bb.maxY, humanizedPoint.yCoord));
		double clampedZ = Math.max(bb.minZ, Math.min(bb.maxZ, humanizedPoint.zCoord));

		Vec3 finalPoint = new Vec3(clampedX, clampedY, clampedZ);

		double distractionNoise = (state.simplexNoise.GetNoise(state.patternIndex * 0.04f, 160.0f) + 1.0) * 0.5;
		double distractionChance = (0.12 + state.sessionFatigue * 0.08) * (1.2 - state.microFocusLevel * 0.4);

		if (distractionNoise > (1.0 - distractionChance)) {
			finalPoint = new Vec3(bb.minX + tlr.nextDouble() * (bb.maxX - bb.minX),
					bb.minY + tlr.nextDouble() * target.height, bb.minZ + tlr.nextDouble() * (bb.maxZ - bb.minZ));
		}

		if (mc.theWorld.rayTraceBlocks(eyes, finalPoint) != null) {
			Vec3[] alternatives = { headPoint, torsoPoint, lowerPoint };
			for (Vec3 alt : alternatives) {
				if (mc.theWorld.rayTraceBlocks(eyes, alt) == null) {
					finalPoint = alt;
					break;
				}
			}
		}

		return finalPoint;
	}

	public static class PatternState {
		public int patternIndex = 0;
		public long patternSeed = System.currentTimeMillis();
		public EntityLivingBase lastTarget = null;
		public float patternYawRange;
		public float patternPitchRange;
		public double patternSpeed = 1.0;

		public Float lastPatternYaw = null;
		public Float lastPatternPitch = null;

		public AimPattern currentPattern = AimPattern.NORMAL;

		public int totalUpdates = 0;
		public double runningAvgReward = 0.0;

		public Vec3 lastTargetPos = null;
		public Vec3 predictedTargetPos = null;
		public int predictionConfidence = 0;

		public int lastPatternSwitchIndex = 0;

		public int transitionTicks = 0;
		public float transitionProgress = 1f;
		public long lastMovementTime = System.nanoTime();
		public float accumulatedPath = 0f;
		public float directDistance = 0f;
		public Vec3 aimStartPos = null;

		public float lastDeltaYaw = 0f;
		public float lastDeltaPitch = 0f;

		public Double lastPlayerEyeY = null;
		public boolean jumpFix = false;

		public int reactionDelayTicks = 0;
		public int targetSwitchIndex = 0;
		public double brownMotionHeight = 0.5;
		public FastNoiseLite simplexNoise;
		public long entropyState;
		public double sessionFatigue = 0.0;
		public double microFocusLevel = 0.5;
		public int concentrationCycle = 0;
		public double baseReactionSkill = 0.0;

		public boolean enableMovementPrediction = true;
		public boolean enableAdaptiveLearning = true;

		public Vec3 secondLastTargetPos = null;
		public Vec3 lastTargetVelocity = null;
		public float jumpTrackingStrength = 1.0f;

		public float targetStepDeg = 1.0f;

		public Float aimBiasX = null;
		public Float aimBiasZ = null;
		public Double lastAimPointX = null;
		public Double lastAimPointZ = null;

		public double circadianPhase = 0.0;
		public double timeSinceSessionStart = 0.0;
		public int consecutiveHits = 0;
		public int consecutiveMisses = 0;
		public double momentumConfidence = 0.5;

		public int rotationCorrections = 0;
		public int totalRotationAttempts = 0;
		public float adaptiveRangeMul = 1.0f;

		public static class AimProfile {
			public float speedMul;
			public float yawRangeMul;
			public float pitchRangeMul;
			public float jitterMul;
			public float overshootMul;
			public float fatigueMul;

			public AimProfile(float speedMul, float yawRangeMul, float pitchMul, float jitterMul, float overshootMul,
					float fatigueMul) {
				this.speedMul = speedMul;
				this.yawRangeMul = yawRangeMul;
				this.pitchRangeMul = pitchMul;
				this.jitterMul = jitterMul;
				this.overshootMul = overshootMul;
				this.fatigueMul = fatigueMul;
			}
		}

		public static final int DIST_BUCKETS = 4;
		public static final int STRAFE_BUCKETS = 3;
		public static final int PROFILE_COUNT = 16;
		public static final int TOTAL_CONTEXTS = DIST_BUCKETS * STRAFE_BUCKETS;

		public double[][] rewards = new double[TOTAL_CONTEXTS][PROFILE_COUNT];
		public double[][] counts = new double[TOTAL_CONTEXTS][PROFILE_COUNT];

		public AimProfile[] profiles = new AimProfile[] { new AimProfile(1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f),
				new AimProfile(0.95f, 0.9f, 0.9f, 1.1f, 0.8f, 0.9f),
				new AimProfile(1.05f, 1.1f, 1.0f, 0.9f, 1.2f, 1.1f), new AimProfile(0.9f, 0.8f, 0.8f, 1.2f, 0.9f, 1.0f),
				new AimProfile(1.1f, 1.0f, 1.1f, 1.0f, 1.3f, 1.0f), new AimProfile(0.85f, 0.9f, 1.1f, 1.3f, 0.9f, 0.8f),
				new AimProfile(1.15f, 1.2f, 0.9f, 0.95f, 1.1f, 1.15f),
				new AimProfile(1.0f, 0.85f, 1.15f, 1.05f, 0.95f, 0.95f),
				new AimProfile(1.20f, 0.95f, 0.85f, 0.85f, 1.4f, 1.25f),
				new AimProfile(0.80f, 0.75f, 0.75f, 1.4f, 0.7f, 0.75f),
				new AimProfile(0.92f, 1.15f, 1.2f, 0.95f, 1.0f, 0.85f),
				new AimProfile(1.08f, 1.05f, 0.95f, 1.15f, 1.15f, 0.70f),
				new AimProfile(1.12f, 0.80f, 0.90f, 1.25f, 1.25f, 1.10f),
				new AimProfile(0.88f, 1.25f, 1.15f, 0.80f, 0.85f, 0.90f),
				new AimProfile(1.03f, 1.08f, 0.88f, 1.05f, 1.05f, 0.95f),
				new AimProfile(0.97f, 0.92f, 1.08f, 1.18f, 0.90f, 1.05f) };

		public int lastProfileIndex = 0;
		public int lastContextIndex = 0;
		public double lastReward = 0.0;
		public long tickCounter = 0L;

		public float currentFatigueMul = 1.0f;
		public int totalEngagements = 0;
		public int totalSuccessfulHits = 0;
		public long sessionStartTime = System.currentTimeMillis();
		private double skillFloor = 0.34;
		private double skillCeiling = 0.87;

		public PatternState() {
			this.simplexNoise = new FastNoiseLite();
			this.simplexNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
			this.simplexNoise.SetFrequency(0.08f);
			this.entropyState = System.nanoTime();
			this.currentPattern = AimPattern.NORMAL;
			this.baseReactionSkill = skillFloor
					+ ThreadLocalRandom.current().nextDouble() * (skillCeiling - skillFloor) * 0.5;
		}
	}

	public static int getDistanceBucket(float distance) {
		if (distance < 1.5f)
			return 0;
		if (distance < 3.0f)
			return 1;
		if (distance < 6.0f)
			return 2;
		return 3;
	}

	public static int getStrafeBucket(Vec3 velocity) {
		double horizontal = Math.sqrt(velocity.xCoord * velocity.xCoord + velocity.zCoord * velocity.zCoord);
		if (horizontal < 0.05)
			return 0;
		if (horizontal < 0.25)
			return 1;
		return 2;
	}

	private static int getContextIndex(int distBucket, int strafeBucket, boolean targetJumping, boolean recentHit) {
		return distBucket * 3 + strafeBucket;
	}

	private static int selectProfile(PatternState state, int contextIndex) {
		ThreadLocalRandom tlr = ThreadLocalRandom.current();

		double epsilon = Math.max(0.08, 0.35 * Math.exp(-state.totalUpdates / 40.0));

		if (tlr.nextDouble() < epsilon) {
			return tlr.nextInt(PatternState.PROFILE_COUNT);
		}

		double[] samples = new double[PatternState.PROFILE_COUNT];
		for (int i = 0; i < PatternState.PROFILE_COUNT; i++) {
			double c = state.counts[contextIndex][i];
			double avgReward = c > 0 ? state.rewards[contextIndex][i] / c : 0.5;

			double alpha = 1.0 + avgReward * c;
			double beta = 1.0 + (1.0 - avgReward) * c;

			samples[i] = sampleBeta(alpha, beta, tlr);
		}

		int bestIdx = 0;
		double bestSample = samples[0];
		for (int i = 1; i < PatternState.PROFILE_COUNT; i++) {
			if (samples[i] > bestSample) {
				bestSample = samples[i];
				bestIdx = i;
			}
		}
		return bestIdx;
	}

	private static double sampleBeta(double alpha, double beta, ThreadLocalRandom tlr) {
		double x = sampleGamma(alpha, tlr);
		double y = sampleGamma(beta, tlr);
		return x / (x + y);
	}

	private static double sampleGamma(double shape, ThreadLocalRandom tlr) {
		if (shape < 1.0) {
			return sampleGamma(shape + 1.0, tlr) * Math.pow(tlr.nextDouble(), 1.0 / shape);
		}
		double d = shape - 1.0 / 3.0;
		double c = 1.0 / Math.sqrt(9.0 * d);
		while (true) {
			double x, v;
			do {
				x = tlr.nextGaussian();
				v = 1.0 + c * x;
			} while (v <= 0);
			v = v * v * v;
			double u = tlr.nextDouble();
			if (u < 1.0 - 0.0331 * x * x * x * x)
				return d * v;
			if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v)))
				return d * v;
		}
	}

	public static void updateProfileReward(PatternState state, double reward) {
		int ctx = state.lastContextIndex;
		int prof = state.lastProfileIndex;

		double decayFactor = 0.998;
		if (state.counts[ctx][prof] > 10) {
			state.rewards[ctx][prof] *= decayFactor;
			state.counts[ctx][prof] *= decayFactor;
		}

		state.rewards[ctx][prof] += reward;
		state.counts[ctx][prof] += 1;
		state.lastReward = reward;

		state.totalUpdates++;

		double alpha = Math.max(0.03, 0.15 * Math.exp(-state.totalUpdates / 200.0));
		state.runningAvgReward = alpha * reward + (1.0 - alpha) * state.runningAvgReward;

	}

	public static float[] getDirectionToBlock(final double x, final double y, final double z,
			final EnumFacing enumfacing) {
		final EntityEgg face = new EntityEgg(mc.theWorld);
		face.posX = x + 0.5D;
		face.posY = y + 0.5D;
		face.posZ = z + 0.5D;
		face.posX += (double) enumfacing.getDirectionVec().getX() * 0.5D;
		face.posY += (double) enumfacing.getDirectionVec().getY() * 0.5D;
		face.posZ += (double) enumfacing.getDirectionVec().getZ() * 0.5D;
		return getRotationFromPosition(face.posX, face.posY, face.posZ);
	}

	public static boolean lookingAtBlock(BlockPos blockPos, float yaw, float pitch, EnumFacing enumFacing,
			boolean strict) {
		MovingObjectPosition movingObjectPosition = mc.thePlayer
				.rayTraceCustom(mc.playerController.getBlockReachDistance(), yaw, pitch);

		if (movingObjectPosition == null || movingObjectPosition.getBlockPos() == null)
			return false;

		Vec3 hitVec = movingObjectPosition.hitVec;
		if (hitVec == null)
			return false;

		return movingObjectPosition.getBlockPos().equalsBlockPos(blockPos)
				&& (!strict || movingObjectPosition.sideHit == enumFacing);
	}

	public static float getYawBasedPitch(BlockPos blockPos, EnumFacing facing, float currentYaw, float lastPitch,
			float maxPitch) {

		float stepJitter = 0.075f + (float) (Math.random() / 10 - 0.02);

		for (float i = 50; i <= maxPitch; i += stepJitter) {
			if (lookingAtBlock(blockPos, currentYaw, i, facing, true)) {
				return i;
			}
		}

		return lastPitch;
	}

	public static float updateRotation(float current, float intended, float factor) {
		return smoothRotation(current, intended, factor);
	}

	public static double yawDist(EntityLivingBase e) {
		final Vec3 difference = e.getPositionVector().addVector(0.0, e.getEyeHeight() / 2.0f, 0.0)
				.subtract(mc.thePlayer.getPositionVector().addVector(0.0, mc.thePlayer.getEyeHeight(), 0.0));
		final double d = Math.abs(
				mc.thePlayer.rotationYaw - (Math.toDegrees(Math.atan2(difference.zCoord, difference.xCoord)) - 90.0f))
				% 360.0f;
		return (d > 180.0f) ? (360.0f - d) : d;
	}

	public static float smoothRotation(float from, float to, float speed) {
		float f = MathHelper.wrapAngleTo180_float(to - from);

		if (f > speed) {
			f = speed;
		}

		if (f < -speed) {
			f = -speed;
		}

		return from + f;
	}

	public static float[] getRotationsFromPositionToPosition(double startX, double startY, double startZ,
			final double posX, final double posY, final double posZ) {
		Vec3 to = new Vec3(posX, posY, posZ);
		Vec3 from = new Vec3(startX, startY, startZ);
		final Vec3 diff = to.subtract(from);
		final double distance = Math.hypot(diff.xCoord, diff.zCoord);
		final float yaw = (float) (Math.toDegrees(MathHelper.atan2(diff.zCoord, diff.xCoord))) - 90.0F;
		final float pitch = (float) (-(Math.toDegrees(MathHelper.atan2(diff.yCoord, distance))));
		return new float[] { yaw, pitch };
	}

	public static boolean canSeeBlock(TileEntity te) {
		Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
		Vec3 target = new Vec3(te.getPos().getX() + 0.5, te.getPos().getY() + 0.5, te.getPos().getZ() + 0.5);

		MovingObjectPosition result = mc.theWorld.rayTraceBlocks(eyes, target);

		return result == null || result.getBlockPos().equals(te.getPos());
	}

	public static float[] getAngles(EntityLivingBase entity, Vec3 pos) {
		return getRotationsFromPositionToPosition(entity.posX, entity.posY, entity.posZ, pos.xCoord, pos.yCoord,
				pos.zCoord);
	}

	public static float[] getRotations(Entity ent, boolean hitVec) {
		double x = ent.posX;
		double z = ent.posZ;
		double eyeHeight = ent.getEyeHeight();
		double y = ent.posY;

		double playerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();

		if (playerY >= y + eyeHeight) {
			y = y + eyeHeight;
			y -= 0.4;
		} else if (playerY < y) {

		} else {
			y = playerY;
			y -= 0.4;
		}

		if (hitVec) {
			Vec3 eyes = mc.thePlayer.getPositionEyes(1);
			Vec3 best = getNearestPositionInBB(eyes, ent.getEntityBoundingBox());

			double nearest = 15.0;
			AxisAlignedBB boundingBox = ent.getEntityBoundingBox();
			for (double x1 = boundingBox.minX; x1 <= boundingBox.maxX; x1 += 0.07) {
				for (double z1 = boundingBox.minZ; z1 <= boundingBox.maxZ; z1 += 0.07) {
					for (double y1 = boundingBox.minY; y1 <= boundingBox.maxY; y1 += 0.07) {
						final Vec3 pos = new Vec3(x1, y1, z1);
						if (mc.thePlayer.canPosBeSeen(pos)) {
							double dist = Math.sqrt(Math.pow(x1 - eyes.xCoord, 2.0) + Math.pow(y1 - eyes.yCoord, 2.0)
									+ Math.pow(z1 - eyes.zCoord, 2.0));
							if (dist <= nearest) {
								nearest = dist;
								best = pos;
							}
						}
					}
				}
			}
			return getRotationFromPosition(best.xCoord, y, best.zCoord);
		}

		return getRotationFromPosition(x, y, z);
	}

	public static float[] getRotationFromPosition(double x, double y, double z) {
		double xDiff = x - (Minecraft.getMinecraft()).thePlayer.posX;
		double zDiff = z - (Minecraft.getMinecraft()).thePlayer.posZ;
		double yDiff = y - (Minecraft.getMinecraft()).thePlayer.posY - 1.2D;
		double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
		float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0D / GothajClient.getInstance().getPI()) - 90.0F;
		float pitch = (float) -(Math.atan2(yDiff, dist) * 180.0D / GothajClient.getInstance().getPI());
		return new float[] { yaw, pitch };
	}

	public static float[] getRotations(Vec3 targetPos) {
		double xDiff = targetPos.xCoord - mc.thePlayer.posX;
		double zDiff = targetPos.zCoord - mc.thePlayer.posZ;
		double yDiff = targetPos.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
		double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
		float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0D / GothajClient.getInstance().getPI()) - 90.0F;
		float pitch = (float) -(Math.atan2(yDiff, dist) * 180.0D / GothajClient.getInstance().getPI());
		return new float[] { yaw, pitch };
	}

	public static float[] getFixedRotations(float[] rotations, float[] lastRotations) {
		float yaw = rotations[0];
		float pitch = rotations[1];

		float lastYaw = lastRotations[0];
		float lastPitch = lastRotations[1];

		float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
		float gcd = f * f * f * 1.2F;

		float deltaYaw = yaw - lastYaw;
		float deltaPitch = pitch - lastPitch;

		float fixedDeltaYaw = deltaYaw - (deltaYaw % gcd);
		float fixedDeltaPitch = deltaPitch - (deltaPitch % gcd);

		float fixedYaw = lastYaw + fixedDeltaYaw;
		float fixedPitch = lastPitch + fixedDeltaPitch;

		return new float[] { fixedYaw, fixedPitch };
	}

	public static Vec3 getNearestPositionInBB(Vec3 var1, AxisAlignedBB var2) {
		var2 = var2.expand(0.1, 0.1, 0.1);
		double var3 = MathHelper.clamp(var1.xCoord, var2.minX, var2.maxX);
		double var5 = MathHelper.clamp(var1.yCoord, var2.minY + (var2.maxY - var2.minY) / 4.0D,
				var2.maxY - (var2.maxY - var2.minY) / 4.0D);
		double var7 = MathHelper.clamp(var1.zCoord, var2.minZ, var2.maxZ);
		return new Vec3(var3, var5 + 0.15, var7);
	}

	public static Vec3 getBestHitVec(final Entity entity) {
		return getBestHitVec(entity, mc.thePlayer.getPositionEyes(1f));
	}

	public static Vec3 getBestHitVec(final Entity entity, final Vec3 positionEyes) {
		final float f11 = 0.1f;
		final AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox().expand(f11, f11, f11);
		final double ex = MathHelper.clamp(positionEyes.xCoord, entityBoundingBox.minX, entityBoundingBox.maxX);
		final double ey = MathHelper.clamp(positionEyes.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
		final double ez = MathHelper.clamp(positionEyes.zCoord, entityBoundingBox.minZ, entityBoundingBox.maxZ);
		return new Vec3(ex, ey - 0.4, ez);
	}

	public static Vec3 getBestHitVecPosition(final Entity entity, final double posX, final double posY,
			final double posZ) {
		final float f11 = 0.1f;

		AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox().expand(f11, f11, f11);
		double dx = posX - entity.posX;
		double dy = posY - entity.posY;
		double dz = posZ - entity.posZ;
		entityBoundingBox = entityBoundingBox.offset(dx, dy, dz);

		final Vec3 positionEyes = new Vec3(posX, posY + mc.thePlayer.getEyeHeight(), posZ);

		final double ex = MathHelper.clamp(positionEyes.xCoord, entityBoundingBox.minX, entityBoundingBox.maxX);
		final double ey = MathHelper.clamp(positionEyes.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
		final double ez = MathHelper.clamp(positionEyes.zCoord, entityBoundingBox.minZ, entityBoundingBox.maxZ);

		return new Vec3(ex, ey - 0.4, ez);
	}

	public static float[] getAngles(EntityPlayer playerIn, EntityLivingBase entity) {
		if (entity == null)
			return null;

		final double diffX = entity.posX - playerIn.posX,
				diffY = entity.posY + entity.getEyeHeight() * 0.9 - (playerIn.posY + playerIn.getEyeHeight()),
				diffZ = entity.posZ - playerIn.posZ, dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

		final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / GothajClient.getInstance().getPI()) - 90.0F,
				pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / GothajClient.getInstance().getPI());

		return new float[] { playerIn.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - playerIn.rotationYaw),
				playerIn.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - playerIn.rotationPitch) };
	}

	public static float[] getAngles(EntityLivingBase entity) {
		return getAngles(mc.thePlayer, entity);
	}

	public static class AntiMLRotationHandler {

		// ========== Configuration ==========
		private static final class SweepConfig {
			// Ornstein-Uhlenbeck (replaces sinusoidal sweep)
			static final double OU_THETA = 0.9;
			static final double OU_SIGMA = 0.40;
			static final double OU_MU_STDDEV = 0.28;

			// Follow factors
			static final double FOLLOW_CLOSE = 0.16;
			static final double FOLLOW_MED = 0.11;
			static final double FOLLOW_FAR = 0.075;

			static final double BBOX_MARGIN = 0.05;

			// Yaw variation
			static final double YAW_VAR_CLOSE = 0.35;
			static final double YAW_VAR_MED = 0.18;
			static final double YAW_VAR_FAR = 0.07;

			// FIX: these are ranges not absolute maxima
			static final long YAW_VAR_INTERVAL_MIN = 700L;
			static final long YAW_VAR_INTERVAL_RANGE = 1500L;

			// Distance thresholds (blocks)
			static final double DIST_CLOSE_THRESHOLD = 4.0;
			static final double DIST_MED_THRESHOLD = 10.0;
			static final double DIST_FAR_THRESHOLD = 16.0;

			// Reaction time model — Gaussian(mu, sigma), floored at min (ms)
			static final double REACTION_TIME_MU = 220.0;
			static final double REACTION_TIME_SIGMA = 40.0;
			static final double REACTION_TIME_MIN = 80.0;

			// OU mean refresh (body-zone attention shift)
			static final long OU_MU_INTERVAL_MIN = 1500L;
			static final long OU_MU_INTERVAL_RANGE = 2000L;

			// Bbox escape (intentional momentary aim drift outside entity)
			static final double ESCAPE_CHANCE_PER_TICK = 0.003;
			static final long ESCAPE_DURATION_MIN = 50L;
			static final long ESCAPE_DURATION_RANGE = 150L;

			// Attack correlation guard
			static final long ATTACK_GUARD_MS = 120L;

			// Distance-based noise scale multipliers
			static final double NOISE_SCALE_CLOSE = 1.00; // < 4 blocks — full noise
			static final double NOISE_SCALE_MED = 0.40; // 4–10 blocks — reduced
			static final double NOISE_SCALE_FAR = 0.15; // 10+ blocks — minimal
		}

		// ========== Rotation State ==========
		private static float headYaw = 0f;
		private static float headPitch = 0f;
		private static float headVelocityYaw = 0f;
		private static float headVelocityPitch = 0f;
		private static float bodyYaw = 0f;
		private static float bodyVelocityYaw = 0f;

		// ========== Timing ==========
		private static long lastTick = System.currentTimeMillis();
		private static boolean initialized = false;

		// ========== Reaction Time ==========
		private static boolean reactionPhase = false;
		private static long reactionDelayMs = 0L;
		private static long targetAcquiredAt = 0L;
		private static int lastTargetId = -1;

		// ========== Attack Correlation Guard ==========
		private static long lastAttackEventMs = -1L;

		// ========== OU Process ==========
		private static double ouX = 0.0, ouY = 0.55, ouZ = 0.0;
		private static double ouMuX = 0.0, ouMuY = 0.55, ouMuZ = 0.0;
		private static long lastOuMuChange = 0L;

		// ========== Bounding Box Targeting ==========
		private static double targetBoxX = 0.0;
		private static double targetBoxY = 0.0;
		private static double targetBoxZ = 0.0;
		private static boolean boxInitialized = false;

		// ========== Yaw Variation ==========
		private static float targetYawVariation = 0f;
		private static float currentYawVariation = 0f;
		private static long lastYawVarChange = 0L;

		// ========== Pink Noise Micro-jitter (Paul Kellett 7-coeff IIR) ==========
		private static double pinkB0y = 0, pinkB1y = 0, pinkB2y = 0, pinkB3y = 0, pinkB4y = 0, pinkB5y = 0, pinkB6y = 0;
		private static double pinkB0p = 0, pinkB1p = 0, pinkB2p = 0, pinkB3p = 0, pinkB4p = 0, pinkB5p = 0, pinkB6p = 0;
		private static double smoothNoiseYaw = 0.0;
		private static double smoothNoisePitch = 0.0;
		private static double noiseTargetYaw = 0.0;
		private static double noiseTargetPitch = 0.0;
		private static long lastNoiseChange = 0L;

		// ========== Hand Tremor ==========
		private static double tremorPhaseYaw = 0.0;
		private static double tremorPhasePitch = 0.0;
		private static double tremorAmpYaw = 0.03;
		private static double tremorAmpPitch = 0.015;
		private static double tremorFreqYaw = 9.0;
		private static double tremorFreqPitch = 7.5;
		private static long lastTremorUpdate = 0L;

		// ========== Attention Wander ==========
		private static double wanderOffsetYaw = 0.0;
		private static double wanderOffsetPitch = 0.0;
		private static double wanderTargetYaw = 0.0;
		private static double wanderTargetPitch = 0.0;
		private static boolean isWandering = false;
		private static long wanderStart = 0L;
		private static long wanderDuration = 0L;

		// ========== Bbox Escape ==========
		private static boolean escapeActive = false;
		private static double escapeYawOffset = 0.0;
		private static double escapePitchOffset = 0.0;
		private static long escapeStart = 0L;
		private static long escapeDuration = 0L;

		// ========== Per-engagement Log-normal Accel Multiplier ==========
		private static double engagementAccelMult = 1.0;

		// ========== Engagement Fatigue ==========
		private static long engagementStartTime = 0L;

		// ========== Movement State ==========
		private static boolean isPlayerMoving = false;
		private static boolean isPlayerSprinting = false;
		private static long lastMovementUpdate = 0L;

		// ========== Targeting Options ==========
		private static boolean targetLegs = false;
		private static long lastTargetSwitch = 0L;

		// --------------------------------------------------------
		// Math helpers
		// --------------------------------------------------------

		private static double gaussian(double mean, double stddev) {
			double u1 = 1.0 - Math.random();
			double u2 = 1.0 - Math.random();
			double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
			return mean + z * stddev;
		}

		private static float wrapDelta(float delta) {
			while (delta > 180f)
				delta -= 360f;
			while (delta < -180f)
				delta += 360f;
			return delta;
		}

		/** Paul Kellett's 7-coefficient IIR pink noise filter — yaw channel. */
		private static double pinkNoiseSampleYaw() {
			double w = gaussian(0, 1);
			pinkB0y = 0.99886 * pinkB0y + w * 0.0555179;
			pinkB1y = 0.99332 * pinkB1y + w * 0.0750759;
			pinkB2y = 0.96900 * pinkB2y + w * 0.1538520;
			pinkB3y = 0.86650 * pinkB3y + w * 0.3104856;
			pinkB4y = 0.55000 * pinkB4y + w * 0.5329522;
			pinkB5y = -0.7616 * pinkB5y - w * 0.0168980;
			double p = pinkB0y + pinkB1y + pinkB2y + pinkB3y + pinkB4y + pinkB5y + pinkB6y + w * 0.5362;
			pinkB6y = w * 0.115926;
			return p * 0.11;
		}

		/** Paul Kellett's 7-coefficient IIR pink noise filter — pitch channel. */
		private static double pinkNoiseSamplePitch() {
			double w = gaussian(0, 1);
			pinkB0p = 0.99886 * pinkB0p + w * 0.0555179;
			pinkB1p = 0.99332 * pinkB1p + w * 0.0750759;
			pinkB2p = 0.96900 * pinkB2p + w * 0.1538520;
			pinkB3p = 0.86650 * pinkB3p + w * 0.3104856;
			pinkB4p = 0.55000 * pinkB4p + w * 0.5329522;
			pinkB5p = -0.7616 * pinkB5p - w * 0.0168980;
			double p = pinkB0p + pinkB1p + pinkB2p + pinkB3p + pinkB4p + pinkB5p + pinkB6p + w * 0.5362;
			pinkB6p = w * 0.115926;
			return p * 0.11;
		}

		// --------------------------------------------------------
		// Distance-scaled getters
		// --------------------------------------------------------

		private static double getFollowFactor(double distance) {
			if (distance < SweepConfig.DIST_CLOSE_THRESHOLD)
				return SweepConfig.FOLLOW_CLOSE;
			else if (distance < SweepConfig.DIST_MED_THRESHOLD)
				return SweepConfig.FOLLOW_MED;
			return SweepConfig.FOLLOW_FAR;
		}

		private static double getYawVariation(double distance) {
			if (distance < SweepConfig.DIST_CLOSE_THRESHOLD)
				return SweepConfig.YAW_VAR_CLOSE;
			else if (distance < SweepConfig.DIST_FAR_THRESHOLD)
				return SweepConfig.YAW_VAR_MED;
			return SweepConfig.YAW_VAR_FAR;
		}

		/** Unified noise multiplier applied to every noise source by distance. */
		private static double getDistanceNoiseMult(double distance) {
			if (distance < SweepConfig.DIST_CLOSE_THRESHOLD)
				return SweepConfig.NOISE_SCALE_CLOSE;
			else if (distance < SweepConfig.DIST_MED_THRESHOLD)
				return SweepConfig.NOISE_SCALE_MED;
			return SweepConfig.NOISE_SCALE_FAR;
		}

		// --------------------------------------------------------
		// State updaters
		// --------------------------------------------------------

		private static void updateMovementState() {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			if (player == null)
				return;
			isPlayerMoving = player.motionX != 0 || player.motionZ != 0;
			isPlayerSprinting = player.isSprinting();
		}

		private static void updateTargetSwitching(long now) {
			long switchInterval = 2000L + (long) (Math.random() * 2000L);
			if (now - lastTargetSwitch > switchInterval) {
				targetLegs = Math.random() < 0.4;
				lastTargetSwitch = now;
			}
		}

		/**
		 * Tremor amplitude scales with distance noise mult; frequency stays constant.
		 */
		private static void updateTremor(long now, double dt, double distance) {
			double nm = getDistanceNoiseMult(distance);
			if (now - lastTremorUpdate > 400L + (long) (Math.random() * 400L)) {
				tremorAmpYaw = (0.02 + Math.random() * 0.05) * nm;
				tremorAmpPitch = (0.01 + Math.random() * 0.03) * nm;
				tremorFreqYaw = 6.0 + Math.random() * 8.0;
				tremorFreqPitch = 5.0 + Math.random() * 7.0;
				lastTremorUpdate = now;
			}
			tremorPhaseYaw += tremorFreqYaw * dt;
			tremorPhasePitch += tremorFreqPitch * dt;
		}

		private static double getTremorYaw() {
			return Math.sin(tremorPhaseYaw) * tremorAmpYaw;
		}

		private static double getTremorPitch() {
			return Math.sin(tremorPhasePitch) * tremorAmpPitch;
		}

		/**
		 * Wander amplitude and trigger probability both scale with distance noise mult.
		 */
		private static void updateAttentionWander(long now, double dt, double distance) {
			double nm = getDistanceNoiseMult(distance);
			if (!isWandering) {
				if (Math.random() < 0.005 * nm) {
					isWandering = true;
					wanderTargetYaw = (float) gaussian(0, 0.6 * nm);
					wanderTargetPitch = (float) gaussian(0, 0.25 * nm);
					wanderDuration = 150L + (long) (Math.random() * 250L);
					wanderStart = now;
				}
			} else {
				long elapsed = now - wanderStart;
				if (elapsed > wanderDuration) {
					wanderOffsetYaw += (0.0 - wanderOffsetYaw) * Math.min(1.0, 4.0 * dt);
					wanderOffsetPitch += (0.0 - wanderOffsetPitch) * Math.min(1.0, 4.0 * dt);
					if (Math.abs(wanderOffsetYaw) < 0.01 && Math.abs(wanderOffsetPitch) < 0.01) {
						isWandering = false;
						wanderOffsetYaw = wanderOffsetPitch = 0.0;
					}
				} else {
					double t = (double) elapsed / wanderDuration;
					double ease = t * t * (3.0 - 2.0 * t);
					wanderOffsetYaw += (wanderTargetYaw * ease - wanderOffsetYaw) * Math.min(1.0, 5.0 * dt);
					wanderOffsetPitch += (wanderTargetPitch * ease - wanderOffsetPitch) * Math.min(1.0, 5.0 * dt);
				}
			}
		}

		/** Escape amplitude and trigger probability scale with distance noise mult. */
		private static void updateBboxEscape(long now, double dt, double distance) {
			double nm = getDistanceNoiseMult(distance);
			if (!escapeActive) {
				if (Math.random() < SweepConfig.ESCAPE_CHANCE_PER_TICK * nm) {
					escapeActive = true;
					escapeYawOffset = gaussian(0, 0.8 * nm);
					escapePitchOffset = gaussian(0, 0.3 * nm);
					escapeDuration = SweepConfig.ESCAPE_DURATION_MIN
							+ (long) (Math.random() * SweepConfig.ESCAPE_DURATION_RANGE);
					escapeStart = now;
				}
			} else {
				if (now - escapeStart > escapeDuration) {
					escapeYawOffset *= (1.0 - Math.min(1.0, 6.0 * dt));
					escapePitchOffset *= (1.0 - Math.min(1.0, 6.0 * dt));
					if (Math.abs(escapeYawOffset) < 0.05 && Math.abs(escapePitchOffset) < 0.05) {
						escapeActive = false;
						escapeYawOffset = escapePitchOffset = 0.0;
					}
				}
			}
		}

		private static void updateNaturalYawVariation(long now, double distance, double dt) {
			// FIX: use RANGE not absolute MAX
			long varInterval = SweepConfig.YAW_VAR_INTERVAL_MIN
					+ (long) (Math.random() * SweepConfig.YAW_VAR_INTERVAL_RANGE);
			if (now - lastYawVarChange > varInterval) {
				targetYawVariation = (float) gaussian(0, getYawVariation(distance));
				lastYawVarChange = now;
			}
			currentYawVariation += (targetYawVariation - currentYawVariation) * Math.min(1.0, 0.08 * dt * 20.0);
		}

		/**
		 * Ornstein-Uhlenbeck process: dX = θ(μ−X)dt + σ√dt·N(0,1) OU sigma is scaled by
		 * distance noise mult — less spatial wandering at range.
		 */
		private static void updateOUProcess(long now, double dt, double distance) {
			long muInterval = SweepConfig.OU_MU_INTERVAL_MIN
					+ (long) (Math.random() * SweepConfig.OU_MU_INTERVAL_RANGE);
			if (now - lastOuMuChange > muInterval) {
				ouMuX = Math.max(-0.70, Math.min(0.70, gaussian(0.0, SweepConfig.OU_MU_STDDEV)));
				ouMuY = targetLegs ? Math.max(0.15, Math.min(0.50, gaussian(0.32, 0.06)))
						: Math.max(0.40, Math.min(0.85, gaussian(0.62, 0.08)));
				ouMuZ = Math.max(-0.70, Math.min(0.70, gaussian(0.0, SweepConfig.OU_MU_STDDEV)));
				lastOuMuChange = now;
			}

			double sqrtDt = Math.sqrt(dt);
			// Scale diffusion by distance noise mult — tighter aim at range
			double sigma = SweepConfig.OU_SIGMA * getDistanceNoiseMult(distance);

			ouX += SweepConfig.OU_THETA * (ouMuX - ouX) * dt + sigma * sqrtDt * gaussian(0, 1);
			ouY += SweepConfig.OU_THETA * (ouMuY - ouY) * dt + sigma * 0.5 * sqrtDt * gaussian(0, 1);
			ouZ += SweepConfig.OU_THETA * (ouMuZ - ouZ) * dt + sigma * sqrtDt * gaussian(0, 1);

			ouX = Math.max(-0.92, Math.min(0.92, ouX));
			ouY = Math.max(0.06, Math.min(0.94, ouY));
			ouZ = Math.max(-0.92, Math.min(0.92, ouZ));
		}

		private static void updateBoundingBoxTarget(EntityLivingBase target, long now, double distance, double dt) {
			if (target == null)
				return;
			AxisAlignedBB bbox = target.getEntityBoundingBox();
			if (bbox == null)
				return;

			double halfWidth = (bbox.maxX - bbox.minX) * 0.5;
			double halfDepth = (bbox.maxZ - bbox.minZ) * 0.5;
			double height = bbox.maxY - bbox.minY;

			updateOUProcess(now, dt, distance);

			double cx = (bbox.minX + bbox.maxX) * 0.5;
			double cz = (bbox.minZ + bbox.maxZ) * 0.5;

			double desiredX = cx + ouX * halfWidth;
			double desiredY = bbox.minY + ouY * height;
			double desiredZ = cz + ouZ * halfDepth;

			desiredX = Math.max(bbox.minX + SweepConfig.BBOX_MARGIN,
					Math.min(bbox.maxX - SweepConfig.BBOX_MARGIN, desiredX));
			desiredY = Math.max(bbox.minY + SweepConfig.BBOX_MARGIN,
					Math.min(bbox.maxY - SweepConfig.BBOX_MARGIN, desiredY));
			desiredZ = Math.max(bbox.minZ + SweepConfig.BBOX_MARGIN,
					Math.min(bbox.maxZ - SweepConfig.BBOX_MARGIN, desiredZ));

			if (!boxInitialized) {
				targetBoxX = desiredX;
				targetBoxY = desiredY;
				targetBoxZ = desiredZ;
				boxInitialized = true;
				return;
			}

			double follow = Math.max(0.03, Math.min(0.30, getFollowFactor(distance) * (1.0 + gaussian(0, 0.08))));
			targetBoxX += (desiredX - targetBoxX) * follow;
			targetBoxY += ((desiredY - 0.15 - targetBoxY) * follow);
			targetBoxZ += (desiredZ - targetBoxZ) * follow;
		}

		/**
		 * Max 15% responsiveness degradation after 2 minutes of continuous engagement.
		 */
		private static double getFatigueFactor() {
			long elapsed = System.currentTimeMillis() - engagementStartTime;
			return 1.0 - Math.min(0.15, elapsed / 120000.0);
		}

		// --------------------------------------------------------
		// Public API
		// --------------------------------------------------------

		/**
		 * Call this from your attack/swing event handler. Prevents tracking from
		 * starting on the exact tick of an attack.
		 */
		public static void notifyAttackEvent() {
			lastAttackEventMs = System.currentTimeMillis();
		}

		public static float[] getAntiMLRotation(EntityLivingBase targetPos) {
			if (targetPos == null)
				return new float[] { 0f, 0f };

			long now = System.currentTimeMillis();
			double dt = Math.min((now - lastTick) / 1000.0, 0.1);
			lastTick = now;

			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			if (player == null)
				return new float[] { 0f, 0f };

			// ---- New target → sample reaction delay, refresh engagement params ----
			int currentTargetId = targetPos.getEntityId();
			if (currentTargetId != lastTargetId) {
				lastTargetId = currentTargetId;
				reactionPhase = true;
				reactionDelayMs = (long) Math.max(SweepConfig.REACTION_TIME_MIN,
						gaussian(SweepConfig.REACTION_TIME_MU, SweepConfig.REACTION_TIME_SIGMA));
				targetAcquiredAt = now;
				engagementStartTime = now;
				engagementAccelMult = Math.exp(gaussian(0, 0.15)); // log-normal per engagement
				boxInitialized = false;
			}

			// ---- Attack correlation guard ----
			boolean attackedRecently = lastAttackEventMs >= 0
					&& (now - lastAttackEventMs) < SweepConfig.ATTACK_GUARD_MS;

			// ---- Reaction phase: hold current heading until delay expires ----
			if (reactionPhase) {
				if ((now - targetAcquiredAt) < reactionDelayMs || attackedRecently) {
					return new float[] { player.rotationYaw, player.rotationPitch };
				}
				reactionPhase = false;
			}

			if (!initialized) {
				headYaw = player.rotationYaw;
				headPitch = player.rotationPitch;
				bodyYaw = player.rotationYaw;
				initialized = true;
			}

			if (now - lastMovementUpdate > 100L) {
				updateMovementState();
				lastMovementUpdate = now;
			}

			float distToTarget = player.getDistanceToEntity(targetPos);

			updateTargetSwitching(now);
			updateTremor(now, dt, distToTarget);
			updateAttentionWander(now, dt, distToTarget);
			updateBboxEscape(now, dt, distToTarget);
			updateBoundingBoxTarget(targetPos, now, distToTarget, dt);
			updateNaturalYawVariation(now, distToTarget, dt);

			float[] headTarget = getRotationFromPosition(targetBoxX, targetBoxY, targetBoxZ);
			float targetHeadYaw = headTarget[0] + currentYawVariation;
			float targetHeadPitch = headTarget[1];

			float deltaHeadYaw = wrapDelta(targetHeadYaw - headYaw);
			float deltaHeadPitch = targetHeadPitch - headPitch;
			double headDist = Math.sqrt(deltaHeadYaw * deltaHeadYaw + deltaHeadPitch * deltaHeadPitch);

			// Log-normal engagement multiplier + fatigue applied to base acceleration
			double headAccel = calculateHeadAcceleration(headDist, isPlayerSprinting) * engagementAccelMult
					* getFatigueFactor();
			float maxHeadStep = calculateMaxHeadStep(headDist);

			float targetHeadVelYaw = (float) (deltaHeadYaw * headAccel);
			float targetHeadVelPitch = (float) (deltaHeadPitch * headAccel * 0.8);

			double headInertia = calculateInertia(headDist, headVelocityYaw, headVelocityPitch, deltaHeadYaw,
					deltaHeadPitch, dt);
			headVelocityYaw = (float) (headVelocityYaw * headInertia + targetHeadVelYaw * (1.0 - headInertia));
			headVelocityPitch = (float) (headVelocityPitch * headInertia + targetHeadVelPitch * (1.0 - headInertia));
			headVelocityYaw = Math.max(-maxHeadStep, Math.min(maxHeadStep, headVelocityYaw));
			headVelocityPitch = Math.max(-maxHeadStep * 0.65f, Math.min(maxHeadStep * 0.65f, headVelocityPitch));

			// Body lags behind head
			float deltaBodyYaw = wrapDelta(headYaw - bodyYaw);
			double bodyLagFactor = isPlayerMoving ? 0.35 : 0.50;
			bodyVelocityYaw = (float) (deltaBodyYaw * bodyLagFactor * (1.0 - Math.exp(-1.5 * dt)));
			bodyVelocityYaw = Math.max(-6.0f, Math.min(6.0f, bodyVelocityYaw));

			// Pink noise micro-jitter — scaled by both bbox safety and distance noise mult
			double nm = getDistanceNoiseMult(distToTarget);
			boolean inBB = isAimWithinBoundingBox(targetPos);
			if (inBB) {
				double noiseScaleYaw, noiseScalePitch;
				if (headDist < 2.5) {
					double cf = 1.0 - headDist / 2.5;
					noiseScaleYaw = 0.22 + cf * 0.38;
					noiseScalePitch = 0.08 + cf * 0.12;
				} else if (headDist < 50.0) {
					noiseScaleYaw = 0.055 * Math.exp(-headDist * 0.015);
					noiseScalePitch = 0.030 * Math.exp(-headDist * 0.015);
				} else {
					noiseScaleYaw = 0.015;
					noiseScalePitch = 0.010;
				}
				double bbSafetyFactor = getNoiseScaleFactor(targetPos, headYaw, headPitch);
				noiseScaleYaw *= bbSafetyFactor * nm;
				noiseScalePitch *= bbSafetyFactor * nm;

				long noiseInterval = 280L + (long) (Math.random() * 220L);
				if (now - lastNoiseChange > noiseInterval) {
					noiseTargetYaw = pinkNoiseSampleYaw() * noiseScaleYaw * 9.0;
					noiseTargetPitch = pinkNoiseSamplePitch() * noiseScalePitch * 9.0;
					lastNoiseChange = now;
				}
				double noiseLerp = headDist < 2.5 ? 1.0 - Math.exp(-2.5 * dt) : 1.0 - Math.exp(-4.0 * dt);
				smoothNoiseYaw += (noiseTargetYaw - smoothNoiseYaw) * noiseLerp;
				smoothNoisePitch += (noiseTargetPitch - smoothNoisePitch) * noiseLerp;
			} else {
				double decayLerp = 1.0 - Math.exp(-3.5 * dt);
				smoothNoiseYaw += (0.0 - smoothNoiseYaw) * decayLerp;
				smoothNoisePitch += (0.0 - smoothNoisePitch) * decayLerp;
				noiseTargetYaw = noiseTargetPitch = 0.0;
			}

			// Compose: tracking + pink jitter + tremor + wander
			float newHeadYaw = headYaw + headVelocityYaw + (float) (smoothNoiseYaw + getTremorYaw() + wanderOffsetYaw);
			float newHeadPitch = Math.max(-90f, Math.min(90f,
					headPitch + headVelocityPitch + (float) (smoothNoisePitch + getTremorPitch() + wanderOffsetPitch)));

			// FIX: validate using all 8 corners; skip during bbox escape
			if (!escapeActive) {
				float[] finalRot = validateRotationInBounds(newHeadYaw, newHeadPitch, targetPos);
				newHeadYaw = finalRot[0];
				newHeadPitch = finalRot[1];
			} else {
				newHeadYaw += (float) escapeYawOffset;
				newHeadPitch = Math.max(-90f, Math.min(90f, newHeadPitch + (float) escapePitchOffset));
			}

			float newBodyYaw = bodyYaw + bodyVelocityYaw;
			headYaw = newHeadYaw;
			headPitch = newHeadPitch;
			bodyYaw = newBodyYaw;

			return new float[] { newHeadYaw, newHeadPitch };
		}

		// --------------------------------------------------------
		// Helpers — all use all 8 bbox corners (FIX)
		// --------------------------------------------------------

		private static double getNoiseScaleFactor(EntityLivingBase target, float currentYaw, float currentPitch) {
			try {
				AxisAlignedBB bbox = target.getEntityBoundingBox();
				float minYawMargin = Float.MAX_VALUE, minPitchMargin = Float.MAX_VALUE;
				double[] xs = { bbox.minX, bbox.maxX };
				double[] ys = { bbox.minY, bbox.maxY };
				double[] zs = { bbox.minZ, bbox.maxZ };
				for (double x : xs)
					for (double y : ys)
						for (double z : zs) {
							float[] rot = getRotationFromPosition(x, y, z);
							float dyaw = Math.abs(wrapDelta(currentYaw - rot[0]));
							float dpit = Math.abs(currentPitch - rot[1]);
							if (dyaw < minYawMargin)
								minYawMargin = dyaw;
							if (dpit < minPitchMargin)
								minPitchMargin = dpit;
						}
				return Math.min(1.0, Math.max(0.2, (minYawMargin + minPitchMargin) / 10.0));
			} catch (Exception e) {
				return 0.8;
			}
		}

		// FIX: 8 corners + normalise yaw relative to headYaw to avoid wrap ambiguity
		private static float[] validateRotationInBounds(float yaw, float pitch, EntityLivingBase target) {
			try {
				AxisAlignedBB bbox = target.getEntityBoundingBox();
				float minYaw = Float.MAX_VALUE, maxYaw = -Float.MAX_VALUE;
				float minPit = Float.MAX_VALUE, maxPit = -Float.MAX_VALUE;
				double[] xs = { bbox.minX, bbox.maxX };
				double[] ys = { bbox.minY, bbox.maxY };
				double[] zs = { bbox.minZ, bbox.maxZ };
				for (double x : xs)
					for (double y : ys)
						for (double z : zs) {
							float[] rot = getRotationFromPosition(x, y, z);
							float cy = headYaw + wrapDelta(rot[0] - headYaw);
							if (cy < minYaw)
								minYaw = cy;
							if (cy > maxYaw)
								maxYaw = cy;
							if (rot[1] < minPit)
								minPit = rot[1];
							if (rot[1] > maxPit)
								maxPit = rot[1];
						}
				yaw = Math.max(minYaw, Math.min(maxYaw, yaw));
				pitch = Math.max(minPit, Math.min(maxPit, pitch));
				return new float[] { yaw, pitch };
			} catch (Exception e) {
				return new float[] { yaw, pitch };
			}
		}

		private static double calculateHeadAcceleration(double distance, boolean sprinting) {
			if (distance > 60.0) {
				return 0.09 + Math.min(0.10, distance * 0.0035);
			} else if (distance > 4.0) {
				double accel = 1.0 - Math.exp(-distance * 0.072);
				return Math.max(0.18, Math.min(0.55, accel));
			} else {
				return Math.max(0.06, Math.min(0.60, distance * 0.15));
			}
		}

		private static float calculateMaxHeadStep(double distance) {
			if (distance > 50.0)
				return 14.0f;
			if (distance > 5.0)
				return 9.5f;
			return 4.0f;
		}

		private static double calculateInertia(double distance, float velYaw, float velPitch, float deltaYaw,
				float deltaPitch, double dt) {
			boolean overshoot = (velYaw > 0 && deltaYaw < 0) || (velYaw < 0 && deltaYaw > 0)
					|| (velPitch > 0 && deltaPitch < 0) || (velPitch < 0 && deltaPitch > 0);
			if (overshoot)
				return Math.exp(-8.5 * dt);
			return distance > 50.0 ? Math.exp(-1.6 * dt) : Math.exp(-3.2 * dt);
		}

		private static boolean isAimWithinBoundingBox(EntityLivingBase target) {
			AxisAlignedBB bb = target.getEntityBoundingBox().contract(0.1, 0.1, 0.1);
			float minDeltaYaw = Float.MAX_VALUE, maxDeltaYaw = -Float.MAX_VALUE;
			float minPitch = Float.MAX_VALUE, maxPitch = -Float.MAX_VALUE;
			double[] xs = { bb.minX, bb.maxX };
			double[] ys = { bb.minY, bb.maxY };
			double[] zs = { bb.minZ, bb.maxZ };
			for (double x : xs)
				for (double y : ys)
					for (double z : zs) {
						float[] rot = getRotationFromPosition(x, y, z);
						float dYaw = wrapDelta(rot[0] - headYaw);
						if (dYaw < minDeltaYaw)
							minDeltaYaw = dYaw;
						if (dYaw > maxDeltaYaw)
							maxDeltaYaw = dYaw;
						if (rot[1] < minPitch)
							minPitch = rot[1];
						if (rot[1] > maxPitch)
							maxPitch = rot[1];
					}
			return minDeltaYaw <= 0f && maxDeltaYaw >= 0f && headPitch >= minPitch && headPitch <= maxPitch;
		}

		public static void reset() {
			initialized = boxInitialized = false;
			headYaw = headPitch = headVelocityYaw = headVelocityPitch = 0f;
			bodyYaw = bodyVelocityYaw = 0f;
			smoothNoiseYaw = smoothNoisePitch = noiseTargetYaw = noiseTargetPitch = 0.0;
			targetBoxX = targetBoxY = targetBoxZ = 0.0;
			ouX = 0.0;
			ouY = 0.55;
			ouZ = 0.0;
			ouMuX = 0.0;
			ouMuY = 0.55;
			ouMuZ = 0.0;
			tremorPhaseYaw = tremorPhasePitch = 0.0;
			wanderOffsetYaw = wanderOffsetPitch = 0.0;
			isWandering = false;
			escapeActive = false;
			escapeYawOffset = escapePitchOffset = 0.0;
			engagementAccelMult = 1.0;
			pinkB0y = pinkB1y = pinkB2y = pinkB3y = pinkB4y = pinkB5y = pinkB6y = 0.0;
			pinkB0p = pinkB1p = pinkB2p = pinkB3p = pinkB4p = pinkB5p = pinkB6p = 0.0;
			targetYawVariation = currentYawVariation = 0f;
			targetLegs = false;
			reactionPhase = false;
			lastTargetId = -1;
			lastAttackEventMs = -1L;
		}
	}

}
