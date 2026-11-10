package team.gothaj.client.impl.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.*;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.notification.NotificationManager;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.property.properties.pattern.AimPattern;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.font.Fonts;
import team.gothaj.client.api.util.game.ClickHandler;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.math.FastNoiseLite;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.api.util.player.RayCastUtil;
import team.gothaj.client.api.util.rotation.RotationDebugger;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.api.util.rotation.ai.AiRotationUtil;
import team.gothaj.client.api.util.rotation.library.LibraryClient;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.UnchangedTimerEvent;
import team.gothaj.client.impl.event.input.MiddleClickEvent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.player.AttackMotionEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.player.PostMotionEvent;
import team.gothaj.client.impl.event.player.RotationEvent;
import team.gothaj.client.impl.event.player.SlowDownEvent;
import team.gothaj.client.impl.event.render.MouseOverEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.module.client.Debug;
import team.gothaj.client.impl.module.movement.NoSlow;
import team.gothaj.client.impl.module.world.Breaker;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;
import team.gothaj.eventbus.Priority;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.Setter;

public class KillAura extends Module {
	public final ModeProperty targetMode = new ModeProperty("Target", "Priority", "Priority", "Switch");
	public final NumberProperty switchDelay = new NumberProperty("Switch Delay", 600, 1, 2000, 1)
			.setVisible(() -> targetMode.is("Switch"));
	public final ModeProperty sortMode = new ModeProperty("Sorting", "Distance", "Distance", "Health", "HurtTime",
			"Fov");
	public static final ModeProperty rotations = new ModeProperty("Rotations", "None", "None", "Normal",
			"Randomized Pattern", "Plain Pattern", "Test", "Polar", "Modern", "Library", "BHV");
	private final BooleanProperty rotationLockView = new BooleanProperty("Rotation Lock View", false)
			.setVisible(() -> !rotations.is("None"));

	private final RangedNumberProperty yawSpeed = new RangedNumberProperty("Yaw Speed", 120, 180, 1, 180, 1);
	private final RangedNumberProperty pitchSpeed = new RangedNumberProperty("Pitch Speed", 120, 180, 1, 180, 1);

	public final static ModeProperty autoBlock = new ModeProperty("Auto Block", "None", "None", "Fake", "Vanilla",
			"Watchdog", "Watchdog2", "Watchdog3", "Polar 1.20").addChangeListener((property, oldValue, newValue) -> switch (
					newValue == null ? "" : newValue.trim().toLowerCase()) {
				case "hypixel", "watchdog" -> "Watchdog";
				case "watchdog2", "flux" -> "Watchdog2";
				case "watchdog3" -> "Watchdog3";
				case "blink", "adaptive" -> "None";
				default -> newValue;
			});

	private final BooleanProperty forceBlockAnimation = new BooleanProperty("Force Block Animation", true)
			.setVisible(() -> isWatchdogAutoBlockMode());
	private static final int BYPASS_CYCLE_TICKS_A = 4;
	private static final int BYPASS_CYCLE_TICKS_B = 3;
	public static final NumberProperty interactRange = new NumberProperty("Interact Range", 3f, 3f, 6f, 0.05F);
	public static final NumberProperty range = new NumberProperty("Range", 3, 3, 6, 0.05F);
	public final ModeProperty clickMode = new ModeProperty("Click Mode", "Normal", "Normal", "Butterfly",
			"Double Click", "Smart", "Recorded", "Jitter", "Drag").addChangeListener((property, oldValue, newValue) -> {
				if (!oldValue.equalsIgnoreCase(newValue) && newValue.equalsIgnoreCase("Recorded")) {
					final NotificationManager notificationManager = GothajClient.getInstance().getNotificationManager();
					notificationManager.add("KillAura", "Pres middle click with enabled KillAura to start recording!",
							Notification.Type.INFO);
					notificationManager.add("KillAura", "Press middle click again when you are done to stop recording!",
							Notification.Type.INFO);
				}
				return newValue;
			});
	private final BooleanProperty perfectHit = new BooleanProperty("Perfect Hit", false)
			.setVisible(() -> !clickMode.is("Recorded"));
	private final BooleanProperty perfectHitSmart = new BooleanProperty("Perfect Hit Smart", false)
			.setVisible(() -> !clickMode.is("Recorded"));
	private final BooleanProperty perfectHitDisableOnDamage = new BooleanProperty("Perfect Hit Disable On Damage", false)
			.setVisible(() -> perfectHit.getProperty() && !clickMode.is("Recorded"));
	private final NumberProperty perfectHitMinHurtTime = new NumberProperty("Perfect Hit Min Hurt Time", 2, 0, 10, 1)
			.setVisible(() -> perfectHit.getProperty() && !clickMode.is("Recorded"));
	private final RangedNumberProperty attackCPS = new RangedNumberProperty("Attack CPS", 10, 15, 0, 20, 1)
			.setVisible(() -> !clickMode.is("Recorded"));
	private final RangedNumberProperty reduceCPS = new RangedNumberProperty("Reduce CPS", 5, 10, 1, 20, 1)
			.setVisible(() -> !clickMode.is("Recorded"));
	private final RangedNumberProperty reduceCPSHurtTime = new RangedNumberProperty("Reduce CPS Hurt Time", 1, 10, 1,
			10, 1).setVisible(() -> !clickMode.is("Recorded"));

	private final BooleanProperty rayTracing = new BooleanProperty("Ray Cast", true);
	private final BooleanProperty useRotEvent = new BooleanProperty("Use Rotation Event", true);
	public final BooleanProperty swingInInteractRange = new BooleanProperty("Swing In Interact Range", true);
	public final BooleanProperty blockInInteractRange = new BooleanProperty("Block In Interact Range", true);
	public final BooleanProperty checkWalls = new BooleanProperty("Check Walls", true);
	public final BooleanProperty forceSprint = new BooleanProperty("ForceSprint", false);
	private final RotationUtil.PatternState patternState = new RotationUtil.PatternState();
	private final RotationUtil.PatternState testPatternState = new RotationUtil.PatternState();
	private final PatternProperty pattern = new PatternProperty("Randomized Pattern", "")
			.setVisible(() -> rotations.is("Randomized Pattern"));
	private final PatternProperty testPattern = new PatternProperty("Plain Pattern", "")
			.setVisible(() -> rotations.is("Plain Pattern"));
	public final BooleanProperty rayTraceRots = new BooleanProperty("Raytrace Rotations When Available", false)
			.setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));
	private final RangedNumberProperty patternAimHeight = new RangedNumberProperty("Pattern Aim Height", 0.6f, 0.8f,
			0.1f, 1.0f, 0.05f).setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));
	private final NumberProperty patternYawRange = new NumberProperty("Pattern Yaw Range", 15, 1, 30, 0.5f)
			.setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));
	private final NumberProperty patternPitchRange = new NumberProperty("Pattern Pitch Range", 20, 1, 30, 0.5f)
			.setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));
	private final NumberProperty patternSpeed = new NumberProperty("Pattern Speed", 1.0f, 0.1f, 8.0f, 0.1f)
			.setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));
	private final NumberProperty patternNoiseScale = new NumberProperty("Pattern Noise", 0.15f, 0.0f, 0.5f, 0.01f)
			.setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));
	private final NumberProperty patternSmoothness = new NumberProperty("Pattern Smoothness", 20, 1, 50, 1)
			.setVisible(() -> rotations.is("Randomized Pattern") || rotations.is("Plain Pattern"));

	private final BooleanProperty polarJitter = new BooleanProperty("Jitter", true)
			.setVisible(() -> rotations.is("Polar"));

	private final NumberProperty polarReactionTime = new NumberProperty("Reaction Time", 6, 0, 50, 1)
			.setVisible(() -> rotations.is("Polar"));

	private final NumberProperty polarYawSmoothnessOnTarget = new NumberProperty("Yaw Smoothness On Target", 0.18f, 0,
			1, 0.01f).setVisible(() -> rotations.is("Polar"));
	private final NumberProperty polarYawSmoothnessOffTarget = new NumberProperty("Yaw Smoothness Off Target", 0.25f, 0,
			1, 0.01f).setVisible(() -> rotations.is("Polar"));
	private final NumberProperty bestHitVecChance = new NumberProperty("Polar Best Hit Vec Chance", 12, 0,
			100, 1).setVisible(() -> rotations.is("Polar"));
	private final BooleanProperty bestHitVecDistanceCheck = new BooleanProperty("Polar Best Hit Vec Distance Check", false).setVisible(() -> rotations.is("Polar"));
	private final NumberProperty bestHitVecDistance = new NumberProperty("Polar Best Hit Vec Distance", 2.7f, 0.0f, 4.0f, 0.1f).setVisible(() -> rotations.is("Polar") && bestHitVecDistanceCheck.getProperty());
	private final BooleanProperty bestHitVecFirstHitOnly = new BooleanProperty("Polar Best Hit Vec First Hit Only", true).setVisible(() -> rotations.is("Polar"));
	private final BooleanProperty bestHitVecResetOnHurtTime = new BooleanProperty("Polar Best Hit Vec Reset On Hurt Time", true).setVisible(() -> rotations.is("Polar") && bestHitVecFirstHitOnly.getProperty());

	private final NumberProperty polarPitchSmoothnessOnTarget = new NumberProperty("Pitch Smoothness On Target", 0.18f,
			0, 1, 0.01f).setVisible(() -> rotations.is("Polar"));
	private final NumberProperty polarPitchSmoothnessOffTarget = new NumberProperty("Pitch Smoothness Off Target",
			0.25f, 0, 1, 0.01f).setVisible(() -> rotations.is("Polar"));

	private final NumberProperty polarNoiseSpeed = new NumberProperty("Noise Speed", 4, 0.5f, 15, 0.5f)
			.setVisible(() -> rotations.is("Polar"));

	private final NumberProperty polarYawNoiseRange = new NumberProperty("Yaw Noise Range", 0.3f, 0, 1, 0.01f)
			.setVisible(() -> rotations.is("Polar"));
	private final NumberProperty polarPitchNoiseRange = new NumberProperty("Pitch Noise Range", 0.6f, 0, 1, 0.01f)
			.setVisible(() -> rotations.is("Polar"));

	private final LibraryClient libraryClient = new LibraryClient();

	private final TimerUtil rotationRandomizationTimer = new TimerUtil();
	public final TimerUtil switchTimer = new TimerUtil();
	private final TimerUtil cpsTimer = new TimerUtil();
	private final TimerUtil recordTimer = new TimerUtil();
	private double currentYawSpeed = 0f, currentPitchSpeed = 0f;
	private boolean isBlocking = false, cpsDelayElapsed, wasTick;
	public static EntityLivingBase target;
	private int autoBlockTicks = 0;
	private boolean watchdogAlternateState = false;
	private int watchdogTargetTicks = 0;
	private double clickDelay;
	private boolean isBlinking = false;
	private final ClickHandler clickHandler = new ClickHandler();
	private final AiRotationUtil rotationUtil = new AiRotationUtil();
	private final ThreadLocalRandom random = ThreadLocalRandom.current();
	private final FastNoiseLite behaviorNoise;
	private final double sessionSkill;
	private double currentFocus = 0.6;
	private int behaviorCycle = 0;
	private long lastHit = 0L;
	private boolean reset = false;
	public boolean isAttacking = false;
	public ArrayList<TargetPosition> targetPositions = new ArrayList<>();

	private boolean isBedWars() {
		if (mc.theWorld == null)
			return false;
		Scoreboard scoreboard = mc.theWorld.getScoreboard();
		ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
		if (objective == null)
			return false;
		String title = objective.getDisplayName();
		return title != null && title.toUpperCase().contains("BED WARS");
	}

	private final TimerUtil noSlowNotifTimer = new TimerUtil();
	private final TimerUtil polarYChangeTimer = new TimerUtil();

	private int polarNoise = 0;

	public KillAura() {
		super("KillAura", "Attacks entities around u.", ModuleCategory.COMBAT);
		this.registerProperties(targetMode, switchDelay, sortMode, clickMode, perfectHit, perfectHitSmart, perfectHitDisableOnDamage, perfectHitMinHurtTime,
				attackCPS, reduceCPS, reduceCPSHurtTime, autoBlock, forceBlockAnimation, interactRange, range,
				rayTracing, swingInInteractRange, blockInInteractRange, checkWalls, forceSprint, rotations, yawSpeed, pitchSpeed,
				rotationLockView, useRotEvent, pattern, testPattern, rayTraceRots, patternAimHeight, patternSpeed,
				patternYawRange, patternPitchRange, patternNoiseScale, patternSmoothness, polarReactionTime,
				polarYawSmoothnessOnTarget, polarYawSmoothnessOffTarget, bestHitVecChance, bestHitVecDistanceCheck, bestHitVecFirstHitOnly, bestHitVecResetOnHurtTime, polarPitchSmoothnessOnTarget,
				polarPitchSmoothnessOffTarget, polarNoiseSpeed, polarYawNoiseRange, polarPitchNoiseRange);
		
		this.behaviorNoise = new FastNoiseLite();
		this.behaviorNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		this.behaviorNoise.SetFrequency(0.06f);
		this.sessionSkill = 0.35 + random.nextDouble() * 0.4;
	}

	@Override
	public String getSuffix() {
		return targetMode.getProperty();
	}

	public void reset() {
		if (mc.thePlayer == null)
			return;
		target = null;
		if (clickHandler.isRecording())
			clickHandler.stopRecording();
		unBlockSword(true);
		unBlink();
		autoBlockTicks = 0;
		watchdogAlternateState = false;
		watchdogTargetTicks = 0;
		patternState.patternIndex = random.nextInt(1000);
		patternState.patternSeed = System.currentTimeMillis();
		patternState.lastTarget = null;
		if (!pattern.getSmoothedPattern().isEmpty()) {
			pattern.setPatternPhaseOffset(new Random(System.nanoTime()).nextInt(pattern.getSmoothedPattern().size()));
		}
		clickHandler.reset();
		RotationUtil.lastTarget = null;

		if (!reset && (!getCastedModule(Scaffold.class).isToggled()
				&& !(getCastedModule(Breaker.class).isToggled() && Breaker.targetBlockPos != null))) {
			RotationComponent.setRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
					MathUtil.random(15, 45), MathUtil.random(15, 45), true);
			reset = true;
		}

		polarNoise = (int) (Math.random() * 99999);
		targetPositions.clear();
	}

	@Override
	public void onEnable() {
		if (target == null) {
			if (rotations.is("Library")) {
				libraryClient.resetRotationData();
			}
		}
		reset();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		targetTicks = 0;
		if (target != null) {
			if (rotations.is("Library")) {
				libraryClient.resetRotationData();
			}
		}
		reset();
		if (clickHandler.isRecording()) {
			clickHandler.stopRecording();
		}
		if (didBlockPolar) {
			didBlockPolar = false;
			PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
			mc.gameSettings.keyBindUseItem.pressed = false;
		}
		super.onDisable();
	}

	@Listen
	public void onSlowDown(SlowDownEvent event) {
		if (forceSprint.getProperty()) {
			event.setShouldSprint(true);
		}
		if (isWatchdogAutoBlockMode()) {
			if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
					&& target != null) {
				event.setForceSlowDown(false);
				event.setSlowDown(1.0F);
				event.setShouldSprint(true);
			}
		}
	}

	/**
	 * ForceSprint: re-assert the sprinting status every tick, after
	 * EntityPlayerSP#onLivingUpdate has had its say and before the sprint state is
	 * compared against serverSprintState, so both the client flag and the
	 * START/STOP_SPRINTING packets report sprinting.
	 */
	@Listen(priority = Priority.CRITICAL)
	public void onForceSprint(MotionEvent event) {
		if (!forceSprint.getProperty() || mc.thePlayer == null) {
			return;
		}

		if (!mc.thePlayer.isSprinting()) {
			mc.thePlayer.setSprinting(true);
		}

		// setSprinting(true) seeds this with 600; keep it topped up so the countdown in
		// onLivingUpdate can never expire and flip the status back off.
		mc.thePlayer.sprintingTicksLeft = 600;
	}

	/**
	 * ForceSprint: vanilla clears the sprint on a knockback-dealing hit. Runs at LOW
	 * priority so it has the final word over other AttackMotionEvent listeners
	 * (KeepSprint's Normal mode among them).
	 */
	@Listen(priority = Priority.LOW)
	public void onForceSprintAttack(AttackMotionEvent event) {
		if (!forceSprint.getProperty()) {
			return;
		}

		event.setSprint(true);
	}

	@Listen
	public void onMiddleClick(MiddleClickEvent event) {
		if (clickMode.is("Recorded")) {
			if (!clickHandler.isRecording()) {
				clickHandler.startRecording();
				Logger.clientChat("recording...");
				recordTimer.reset();
			} else {
				if (recordTimer.hasTimeReached(150)) {
					clickHandler.stopRecording();
					Logger.clientChat("stopping...");
				}
			}
		}
	}

	@Listen
	public void onRotation(RotationEvent event) {
		cpsDelay();
		if (target == null || !useRotEvent.getProperty() || rotations.is("Polar"))
			return;
		calculateRotations(true, true);
	}

	@Listen
	public void onMotion(PostMotionEvent event) {
		if (rotationLockView.getProperty() && target != null && RotationComponent.rotations != null) {
			mc.thePlayer.rotationYaw = RotationComponent.rotations[0];
			mc.thePlayer.rotationPitch = RotationComponent.rotations[1];
		}
	}

	@Listen
	public void onLegitClick(MouseClickEvent event) {
		wasTick = true;
		tickAction();
		NoSlow noSlow = getCastedModule(NoSlow.class);
		if (noSlow.isToggled() && isBedWars()) {
			noSlow.setToggled(false);
			GothajClient.getInstance().getNotificationManager().add("NoSlow", "Auto-disabled on BedWars!",
					Notification.Type.INFO);
		}
	}

	@Listen
	public void onUnchangedTimer(UnchangedTimerEvent event) {
		if (wasTick) {
			wasTick = false;
			return;
		}

		tickAction();
	}

	public void tickAction() {
		libraryClient.getGui(rotations);
		isAttacking = false;
		ignoreTicks--;
		if (ignoreTicks <= 0)
			ignoreTicks = 0;
		if (clickMode.is("Recorded")) {
			if (clickHandler.isRecording()) {
				while (mc.gameSettings.keyBindAttack.isPressed()) {
					clickHandler.handleRealClick();
				}
			}
		}
		if (RotationComponent.rotations == null)
			return;
		target = EntityUtil.getTarget(interactRange.getProperty());
		if (target == null || (getCastedModule(Scaffold.class).isToggled()
				|| (getCastedModule(Breaker.class).isToggled() && Breaker.targetBlockPos != null))) {
			targetTicks = 0;
			if (didBlockPolar) {
				didBlockPolar = false;
				PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
				mc.gameSettings.keyBindUseItem.pressed = false;
			}
			reset();
			return;
		} else {
			targetTicks++;
		}
		if (mc.thePlayer.hurtTime == 1 && bestHitVecResetOnHurtTime.getProperty())
			targetTicks = 0;

		// autoprojectile, extinguish, annoyer when occupying rotationcomponent
		if (getCastedModule(AutoProjectile.class).isRotating() || getCastedModule(Extinguish.class).isRotating()
				|| getCastedModule(Annoyer.class).isRotating()) {
			reset();
			return;
		}

		calculateRotations(true, (!useRotEvent.getProperty() || rotations.is("Polar")));
		handleAB();
		attack();
	}

	private int rolled = 0;

	public boolean shouldBlink() {
		return mc.thePlayer.ticksExisted % 8 >= 3;
	}

	private void calculateRotations(boolean... setRots) {
		if (RotationComponent.rotations == null)
			return;
		float targetYaw = RotationComponent.rotations[0];
		float targetPitch = RotationComponent.rotations[1];

		if (setRots[0]) {
			behaviorCycle++;
			patternState.lastMovementTime = System.nanoTime();
			if (target != patternState.lastTarget || mc.thePlayer.getHealth() <= 5 || target.getHealth() <= 3) {
				patternState.lastTarget = target;
				patternState.currentPattern = mc.thePlayer.getHealth() <= 3 ? AimPattern.HESITATE : AimPattern.NORMAL;
				patternState.transitionTicks = 6 + random.nextInt(8);
				patternState.transitionProgress = 0f;
				patternState.patternIndex = random.nextInt(1000);
				if (!pattern.getSmoothedPattern().isEmpty()) {
					pattern.setPatternPhaseOffset(random.nextInt(pattern.getSmoothedPattern().size()));
				}
				patternState.accumulatedPath = 0f;
				patternState.directDistance = 0f;
				patternState.aimStartPos = new Vec3(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 0);
			}

			double minYaw = this.yawSpeed.getLeft();
			double maxYaw = this.yawSpeed.getRight();
			double minPitch = this.pitchSpeed.getLeft();
			double maxPitch = this.pitchSpeed.getRight();

			double focusNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.003f, 10.0f) + 1.0) * 0.5;
			currentFocus += (focusNoise - 0.5) * 0.015;
			currentFocus = Math.clamp(currentFocus, 0.2, 0.9);
			double intervalBase = 140 + behaviorNoise.GetNoise(behaviorCycle * 0.002f, 20.0f) * 80;
			long randomizationInterval = (long) (intervalBase * (1.1 - currentFocus * 0.2));
			if (this.rotationRandomizationTimer.hasTimeReached(randomizationInterval)) {
				double betaExp1 = 1.15 + sessionSkill * 0.35 + currentFocus * 0.2;
				double betaExp2 = 1.2 + sessionSkill * 0.3 + currentFocus * 0.15;
				double beta1 = Math.pow(random.nextDouble(), betaExp1);
				double beta2 = Math.pow(random.nextDouble(), betaExp2);
				this.currentYawSpeed = minYaw + beta1 * (maxYaw - minYaw);
				this.currentPitchSpeed = minPitch + beta2 * (maxPitch - minPitch);
				this.rotationRandomizationTimer.reset();
			}

			if (target != null) {

				if (rotations.is("BHV")) {
					Vec3 bhv = RotationUtil.getBestHitVec(target);
					float[] rots = RotationUtil.getRotations(bhv);
					targetYaw = rots[0];
					targetPitch = rots[1];
				}

				if (rotations.is("Normal")) {
					final Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
							mc.thePlayer.posZ);
					Vec3 targetVis = new Vec3(target.posX, target.getEntityBoundingBox().minY + target.height,
							target.posZ);
					boolean currentAimVisible = mc.theWorld.rayTraceBlocks(eyes, targetVis) == null;
					double visibleY = -1;
					AxisAlignedBB entityBox = target.getEntityBoundingBox();
					if (!currentAimVisible) {
						for (double y2 = entityBox.minY; y2 <= entityBox.maxY; y2 += 0.008) {
							Vec3 targetVecY = new Vec3(target.posX, y2, target.posZ);
							if (mc.theWorld.rayTraceBlocks(eyes, targetVecY) == null) {
								visibleY = y2 - target.posY;
								break;
							}
						}
					}
					double adjustedAimHeight = 0.8;
					if (visibleY != -1)
						adjustedAimHeight = (visibleY / target.height);
					if (visibleY != -1) {
						double targetAimY = target.posY + (target.height * adjustedAimHeight);
						float[] rots = RotationUtil.getRotationFromPosition(target.posX, targetAimY, target.posZ);
						targetYaw = rots[0];
						targetPitch = rots[1];
					} else {
						float[] rots = RotationUtil.getRotations(target, true);
						targetYaw = rots[0];
						targetPitch = rots[1];
					}
				}

				if (rotations.is("Randomized Pattern")) {
					float distanceToTarget = mc.thePlayer.getDistanceToEntity(target);
					float distanceScale = Math.min(1.0f, distanceToTarget / 3.5f);
					patternState.patternYawRange = patternYawRange.getProperty() * (0.3f + distanceScale * 0.7f);
					patternState.patternPitchRange = patternPitchRange.getProperty() * (0.3f + distanceScale * 0.7f);
					patternState.patternSpeed = patternSpeed.getProperty();
					pattern.setNoiseScale(patternNoiseScale.getProperty());
					pattern.setSmoothness(patternSmoothness.getProperty().intValue());
					int ticksSinceSwitch = patternState.patternIndex - patternState.lastPatternSwitchIndex;
					double switchNoise = (behaviorNoise.GetNoise(behaviorCycle * 0.008f, 30.0f) + 1.0) * 0.5;
					double switchBase = ticksSinceSwitch * (0.0012 + sessionSkill * 0.0008);
					float switchProbability = (float) Math.min(0.18, switchBase * (1.0 + switchNoise * 0.4));
					if (ticksSinceSwitch > 25 && random.nextDouble() < switchProbability) {
						double patternRoll = (behaviorNoise.GetNoise(behaviorCycle * 0.01f, 40.0f) + 1.0) * 0.5;
						double normalBias = 0.55 + sessionSkill * 0.05;
						AimPattern newPattern = patternRoll < normalBias ? AimPattern.NORMAL
								: (patternRoll < (normalBias + 0.25) ? AimPattern.HESITATE : AimPattern.OVERSHOOT);
						if (newPattern != patternState.currentPattern || random.nextDouble() < 0.3) {
							patternState.currentPattern = newPattern;
							pattern.setCurrentPattern(patternState.currentPattern);
							patternState.lastPatternSwitchIndex = patternState.patternIndex;
						}
					}
					patternState.targetStepDeg = 0.3f + (40f - patternSmoothness.getProperty()) * 0.04f;
					float[] rots = RotationUtil.patternRotations(target, pattern, patternState,
							patternAimHeight.getLeft(), patternAimHeight.getRight(), rayTraceRots.getProperty(),
							lastHit, false);
					if (patternState.transitionTicks > 0) {
						patternState.transitionProgress += 1f / (8 + patternState.transitionTicks);
						patternState.transitionProgress = Math.min(1f, patternState.transitionProgress);
						patternState.transitionTicks--;
						float easeOut = 1f - (float) Math.pow(1 - patternState.transitionProgress, 2.5);
						float yawDelta = RotationUtil.normalizeAngleDelta(rots[0] - targetYaw);
						rots[0] = targetYaw + yawDelta * easeOut;
						rots[1] = targetPitch + (rots[1] - targetPitch) * easeOut;
					}
					targetYaw = rots[0];
					targetPitch = rots[1];
				}

				if (rotations.is("Plain Pattern")) {
					testPatternState.patternYawRange = patternYawRange.getProperty();
					testPatternState.patternPitchRange = patternPitchRange.getProperty();
					testPatternState.patternSpeed = patternSpeed.getProperty();
					testPattern.setNoiseScale(patternNoiseScale.getProperty());
					testPattern.setSmoothness(patternSmoothness.getProperty().intValue());
					float[] rots = RotationUtil.patternRotations(target, testPattern, testPatternState,
							patternAimHeight.getLeft(), patternAimHeight.getRight(), rayTraceRots.getProperty(),
							lastHit, true);
					targetYaw = rots[0];
					targetPitch = rots[1];
				}

				if (rotations.is("Test")) {
					float[] rots = RotationUtil.AntiMLRotationHandler.getAntiMLRotation(target);
					targetYaw = rots[0];
					targetPitch = rots[1];
				}

				if (rotations.is("Polar")) {

					FastNoiseLite noise1 = new FastNoiseLite();
					noise1.SetNoiseType(FastNoiseLite.NoiseType.ValueCubic);
					noise1.SetFrequency(0.04f);

					double x = target.posX;
					double z = target.posZ;

					double playerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();

					double yChange = EntityUtil.getDistanceToEntityBoxFromPosition(mc.thePlayer.posX, target.posY,
							mc.thePlayer.posZ, target) > 2.5 ? 0.42
									: EntityUtil.getDistanceToEntityBoxFromPosition(mc.thePlayer.posX, target.posY,
											mc.thePlayer.posZ, target) > 2 ? 0.84 : 1.2;

					double y = playerY - yChange;

					if (playerY > target.posY + target.getEyeHeight() + (yChange - 0.13)) {
						y = target.posY + target.getEyeHeight() - 0.5;
					} else if (y < target.posY) {
						y = target.posY;
					}

					double yawMultiplier = polarYawNoiseRange.getProperty();
					double pitchMultiplier = polarPitchNoiseRange.getProperty();

					int ticksExisted = (int) (polarNoise + mc.thePlayer.ticksExisted * polarNoiseSpeed.getProperty());

					x += noise1.GetNoise(ticksExisted + 54811, ticksExisted + 651250) * yawMultiplier;
					z += noise1.GetNoise(ticksExisted + 54811, ticksExisted + 835250) * yawMultiplier;
					y += noise1.GetNoise(ticksExisted + 87441, ticksExisted + 275241) * pitchMultiplier;

					targetPositions.add(new TargetPosition(new double[] { (float) x, (float) y, (float) z },
							(int) (mc.thePlayer.ticksExisted + polarReactionTime.getProperty() + 1)));

					double lastX = 0, lastY = 0, lastZ = 0;
					for (TargetPosition r : targetPositions) {
						r.delay--;

						if (r.delay == mc.thePlayer.ticksExisted) {
							lastX = r.getPosition()[0];
							lastY = r.getPosition()[1];
							lastZ = r.getPosition()[2];
						}
					}
					if (lastX != 0 && lastY != 0 && lastZ != 0) {
						float[] rots = RotationUtil.getRotationFromPosition(lastX, lastY, lastZ);
						if (rolled < bestHitVecChance.getProperty()) {
							if (!(mc.thePlayer.getDistanceToEntity(target) < bestHitVecDistance.getProperty().doubleValue() && bestHitVecDistanceCheck.getProperty())
									&& !(targetTicks < 4 && bestHitVecFirstHitOnly.getProperty())) {
								rots = RotationUtil.getRotations(RotationUtil.getBestHitVec(target));
							}
						}
						float yawSmooth = (mc.objectMouseOver.entityHit != null
								? (1 - polarYawSmoothnessOnTarget.getProperty())
								: (1 - polarYawSmoothnessOffTarget.getProperty()));
						float pitchSmooth = (mc.objectMouseOver.entityHit != null
								? (1 - polarPitchSmoothnessOnTarget.getProperty())
								: (1 - polarPitchSmoothnessOffTarget.getProperty()));

						float yawDelta = RotationUtil.smoothYaw(rots[0], RotationComponent.lastRotations[0], yawSmooth);
						float pitchDelta = RotationUtil.smoothPitch(rots[1], RotationComponent.lastRotations[1],
								pitchSmooth);

						if (polarJitter.getProperty()) {
							yawDelta += (Math.random() - Math.random());
							pitchDelta += (Math.random() - Math.random());
						}
						rots[0] = RotationComponent.lastRotations[0] + yawDelta;
						rots[1] = RotationComponent.lastRotations[1] + pitchDelta;

						targetYaw = rots[0];
						targetPitch = rots[1];
					}
				}

				if (rotations.is("Modern")) {
					float[] rots = RotationUtil.getRotations(rotationUtil.getAimTarget(mc.thePlayer, target));
					targetYaw = rots[0];
					targetPitch = rots[1];
				}
				if (rotations.is("Library")) {
					float[] rots = libraryClient.getRotation(target);
					targetYaw = rots[0];
					targetPitch = rots[1];
				}
			}
		}
		if (setRots[1])

		{
			if (!rotations.is("None")) {
				if (getCastedModule(Debug.class).isToggled()) {
					RotationDebugger.recordRotation(RotationComponent.rotations[0], RotationComponent.rotations[1]);
				}

				RotationComponent.setRotations(targetYaw, targetPitch, currentYawSpeed, currentPitchSpeed, true);
			}
		}
	}

	private void sendNoSlowPackets() {
		int randomSlot = random.nextInt(9);
		while (randomSlot == mc.thePlayer.inventory.currentItem) {
			randomSlot = random.nextInt(9);
		}
		PacketUtil.send(new C09PacketHeldItemChange(randomSlot));
		PacketUtil.send(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
	}

	private boolean didBlockPolar = false;

	private void handleAB() {
		switch (autoBlock.getProperty()) {
		case "Polar 1.20" -> {
			didBlockPolar = true;
			PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
		}
		}
	}

	private boolean isWatchdogAutoBlockMode() {
		return autoBlock.is("Watchdog") || autoBlock.is("Watchdog2") || autoBlock.is("Watchdog3");
	}

	private boolean shouldHoldWatchdogAutoBlock() {
		return isToggled() && target != null && mc.thePlayer != null && mc.thePlayer.getHeldItem() != null
				&& mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
	}

	private void handleWatchdog2AutoBlock(boolean inInteractRange, boolean inHitRange) {
		if (!inInteractRange || !shouldHoldWatchdogAutoBlock()) {
			if (autoBlockTicks > 0 && mc.thePlayer != null) {
				PacketUtil.send(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
			}
			if (mc.thePlayer != null && (isBlocking || mc.thePlayer.isUsingItem())) {
				PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
				mc.thePlayer.stopUsingItem();
				isBlocking = false;
			}
			unBlink();
			autoBlockTicks = 0;
			watchdogTargetTicks = 0;
			return;
		}

		final BlinkComponent blinkComponent = GothajClient.getInstance().getComponentManager()
				.getAndCast(BlinkComponent.class);

		switch (autoBlockTicks) {
		case 0 -> {
			int randomSlot = random.nextInt(9);
			while (randomSlot == mc.thePlayer.inventory.currentItem) {
				randomSlot = random.nextInt(9);
			}

			blinkComponent.startBlinkingOutbound();
			isBlinking = true;
			PacketUtil.send(new C09PacketHeldItemChange(randomSlot));

			if (isBlocking || mc.thePlayer.isUsingItem()) {
				PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
				mc.thePlayer.setSprinting(false);
				mc.thePlayer.stopUsingItem();
				isBlocking = false;
			}

			watchdogTargetTicks = watchdogAlternateState ? BYPASS_CYCLE_TICKS_A : BYPASS_CYCLE_TICKS_B;
			watchdogAlternateState = !watchdogAlternateState;
			autoBlockTicks = 1;
		}
		case 1 -> {
			PacketUtil.send(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
			autoBlockTicks = 2;
		}
		default -> autoBlockTicks++;
		}

		if (autoBlockTicks >= watchdogTargetTicks) {
			if (inHitRange) {
				watchdog2Attack();
			} else if (target != null && inInteractRange) {
				PacketUtil.send(new C0APacketAnimation());
				mc.thePlayer.swingItem();
				isAttacking = true;
			}

			if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
				mc.playerController.syncCurrentPlayItem();
				PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
				isBlocking = true;
			}

			blinkComponent.stopBlinkingOutbound();
			isBlinking = false;
			autoBlockTicks = 0;
		}
	}

	private void watchdog2Attack() {
		if (target == null)
			return;

		PacketUtil.send(new C0APacketAnimation());
		mc.thePlayer.swingItem();
		PacketUtil.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
		PacketUtil.send(new C02PacketUseEntity(target, new Vec3(0.0D, 0.0D, 0.0D)));
		PacketUtil.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
		mc.thePlayer.attackTargetEntityWithCurrentItem(target);
		isAttacking = true;
	}

	/**
	 * Watchdog3 — 7-CPS variant. A 3-tick cycle: attack while the server sees us
	 * unblocked, block on the next tick behind a fresh blink window, release on the
	 * one after. The release lands in the blink queue and is flushed at the start of
	 * the following block tick, so the server reads
	 * [release, attack, block] in that order every cycle.
	 *
	 * Cycle (autoBlockTicks): 0 warm-up → 1 attack → 2 block+blink → 3 release → 1 …
	 * One attack per 3 ticks ≈ 6.7 CPS.
	 */
	private void handleWatchdog3AutoBlock(boolean inInteractRange, boolean inHitRange) {
		if (!inInteractRange || !shouldHoldWatchdogAutoBlock()) {
			if (isBlocking || mc.thePlayer.isUsingItem()) {
				PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
				mc.thePlayer.stopUsingItem();
				isBlocking = false;
			}
			unBlink();
			autoBlockTicks = 0;
			return;
		}

		final BlinkComponent blinkComponent = GothajClient.getInstance().getComponentManager()
				.getAndCast(BlinkComponent.class);

		switch (autoBlockTicks) {
		case 0 -> autoBlockTicks = 1;

		case 1 -> {
			// Attack while the server-side state is unblocked.
			if (inHitRange) {
				watchdog3Attack();
			} else if (target != null && swingInInteractRange.getProperty()) {
				PacketUtil.send(new C0APacketAnimation());
				mc.thePlayer.swingItem();
				isAttacking = true;
			}
			isBlocking = false;
			autoBlockTicks = 2;
		}

		case 2 -> {
			if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
				mc.playerController.syncCurrentPlayItem();
				PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
				isBlocking = true;
			}

			// Flush whatever the previous cycle queued (the release), then reopen the
			// window so this cycle's release is held the same way.
			blinkComponent.stopBlinkingOutbound();
			blinkComponent.startBlinkingOutbound();
			isBlinking = true;
			autoBlockTicks = 3;
		}

		case 3 -> {
			if (isBlocking) {
				PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
			}
			isBlocking = false;
			autoBlockTicks = 1;
		}

		default -> autoBlockTicks = 1;
		}
	}

	private void watchdog3Attack() {
		if (target == null)
			return;

		PacketUtil.send(new C0APacketAnimation());
		mc.thePlayer.swingItem();
		mc.playerController.attackEntityEvent(mc.thePlayer, target);
		PacketUtil.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
		RotationUtil.AntiMLRotationHandler.notifyAttackEvent();
		lastHit = System.currentTimeMillis();
		isAttacking = true;
	}

	private void attack() {
		boolean inInteractRange = inInteractRange();
		boolean inHitRange = inHitRange();

		if (inHitRange) {
			rolled = random.nextInt(0, 100);
		}

		if (autoBlock.is("Watchdog2")) {
			handleWatchdog2AutoBlock(inInteractRange, inHitRange);
			return;
		}

		if (autoBlock.is("Watchdog3")) {
			handleWatchdog3AutoBlock(inInteractRange, inHitRange);
			return;
		}

		if (autoBlock.is("Watchdog") && inInteractRange) {
			boolean rayCast = RayCastUtil.rayTrace(range.getProperty(), RotationComponent.rotations) == target;
			final BlinkComponent blinkComponent = GothajClient.getInstance().getComponentManager()
					.getAndCast(BlinkComponent.class);

			switch (autoBlockTicks) {
			case 0 -> {
				int randomSlot = random.nextInt(9);
				while (randomSlot == mc.thePlayer.inventory.currentItem) {
					randomSlot = random.nextInt(9);
				}

				blinkComponent.startBlinkingOutbound();
				isBlinking = true;
				PacketUtil.send(new C09PacketHeldItemChange(randomSlot));

				if (isBlocking) {
					PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
					isBlocking = false;
				}

				watchdogTargetTicks = watchdogAlternateState ? BYPASS_CYCLE_TICKS_A : BYPASS_CYCLE_TICKS_B;
				watchdogAlternateState = !watchdogAlternateState;

				autoBlockTicks = 1;
			}

			case 1 -> {
				PacketUtil.send(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
				autoBlockTicks = 2;
			}

			default -> autoBlockTicks++;
			}

			if (autoBlockTicks >= watchdogTargetTicks) {
				if (inHitRange) {
					mc.thePlayer.swingItem();
					isAttacking = true;

					if (rayCast) {
						mc.playerController.attackEntityEvent(mc.thePlayer, target);

						PacketUtil.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
					}
				}

				if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
					PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
					isBlocking = true;
				}

				blinkComponent.stopBlinkingOutbound();
				isBlinking = false;

				autoBlockTicks = 0;
			}

			return;
		}

		if (inInteractRange) {
			if (perfectHit.getProperty()) {
				if (canPerfectHit() && inHitRange && (!rayTracing.getProperty()
						|| RayCastUtil.rayTrace(range.getProperty(), RotationComponent.rotations) == target)) {
					cpsDelayElapsed = true;
				}
			}
			if (cpsDelayElapsed) {
				if (!isBlocking)
					sendAttack(inHitRange, inInteractRange);
			}

			if (blockInInteractRange.getProperty() || inHitRange) {
				if (autoBlock.is("Vanilla")) {
					blockSword(true, true);
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
				}
			}
			cpsDelayElapsed = false;
			clickDelay = randomizeCPS();
		} else {
			unBlink();
		}
	}

	private void sendAttack(boolean inHitRange, boolean inInteractRange) {
		if (target == null)
			return;
		boolean rayCast = RayCastUtil.rayCast(RotationComponent.rotations, range.getProperty(), 0).entityHit != null;

		if (inInteractRange && !rayCast) {
			pattern.recordHit(false);
			RotationUtil.updateGlobalSkill(patternState, false);
		}

		if ((!inHitRange || !rayCast) && inInteractRange && swingInInteractRange.getProperty()) {
			isAttacking = true;
			clickHandler.registerClick();
			clickHandler.recordMiss();
			pattern.recordHit(false);
			RotationUtil.updateGlobalSkill(patternState, false);
			mc.thePlayer.swingItem();

		}

		boolean canHitNow = inHitRange && (rayCast || !rayTracing.getProperty());

		if (!canHitNow)
			return;

		Velocity velocity = getCastedModule(Velocity.class);
		if (velocity != null && velocity.shouldBlockKillAuraAttackThisTick(target))
			return;

		clickHandler.registerClick();
		clickHandler.recordHit();
		mc.thePlayer.swingItem();
		mc.playerController.attackEntityEvent(mc.thePlayer, target);
		RotationUtil.AntiMLRotationHandler.notifyAttackEvent();
		lastHit = System.currentTimeMillis();

		if (rotations.is("Randomized Pattern")) {
			pattern.recordHit(true);
			RotationUtil.updateGlobalSkill(patternState, true);
			applyAimReward(RotationComponent.rotations);
		}
	}

	private int ignoreTicks = 0;

	private boolean canPerfectHit() {
		if (target == null)
			return false;

		if (perfectHitSmart.getProperty()) {
			if (target.hurtTime <= perfectHitMinHurtTime.getProperty() && ignoreTicks <= 0) {
				ignoreTicks = 4;
				return true;
			}
		} else {
			if (target.hurtTime <= perfectHitMinHurtTime.getProperty())
				return true;
		}

		return mc.thePlayer.hurtTime != 0 && perfectHitDisableOnDamage.getProperty();
	}

	public boolean canBlockVisually() {
		boolean swordOrAxe = mc.thePlayer.getHeldItem() != null
				&& (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
						|| mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe);
		if (isWatchdogAutoBlockMode()) {
			return isToggled() && target != null && swordOrAxe && (forceBlockAnimation.getProperty() || isBlocking);
		}
		return isToggled() && target != null && !autoBlock.is("None") && swordOrAxe;
	}

	public boolean isWatchdogBlocking() {
		return isWatchdogAutoBlockMode() && isBlocking;
	}

	public void hypixelBlockSword() {
		boolean isSword = mc.thePlayer.getHeldItem() != null
				&& (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword);
		if (isSword) {
			PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
			isBlocking = true;
		}
	}

	private void hypixelUnBlink() {
		GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class).stopBlinkingOutbound();
		isBlinking = false;
	}

	public void blockSword(boolean sendPlacement, boolean interact) {
		if (!isBlocking && mc.thePlayer.inventory.getCurrentItem() != null
				&& mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSword) {
			MovingObjectPosition mop = mc.objectMouseOver;
			if (mop != null) {
				if (interact) {
					PacketUtil.send(new C02PacketUseEntity(target, new Vec3(mop.hitVec.xCoord - target.posX,
							mop.hitVec.yCoord - target.posY, mop.hitVec.zCoord - target.posZ)));
					PacketUtil.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
				}
				if (sendPlacement) {
					PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
				} else {
					KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
				}
				isBlocking = true;
			}
		}
	}

	public void unBlockSword(boolean instantRelease) {
		if (isBlocking && mc.thePlayer.inventory.getCurrentItem() != null
				&& mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSword) {
			if (getCastedModule(NoSlow.class).isToggled() && isWatchdogAutoBlockMode()) {
				sendNoSlowPackets();
			}
			if (instantRelease)
				PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
			else
				mc.gameSettings.keyBindUseItem.pressed = false;
			isBlocking = false;
		}
	}

	private void unBlink() {
		BlinkComponent blinkComp = GothajClient.getInstance().getComponentManager()
				.getAndCast(BlinkComponent.class);
		if (isBlinking || blinkComp.isBlinkingOutbound()) {
			blinkComp.stopBlinkingOutbound();
			isBlinking = false;
		}
	}

	private boolean inHitRange() {
		return EntityUtil.isInReach(target, range.getProperty()) != null;
	}

	private boolean inInteractRange() {
		return EntityUtil.getDistanceToEntityBox(target) <= interactRange.getProperty();
	}

	private void cpsDelay() {
		if (cpsTimer.hasTimeReached((long) clickDelay, true))
			cpsDelayElapsed = true;
	}

	private double randomizeCPS() {
		if (clickHandler.isRecording())
			return 120;

		double min = mc.thePlayer.hurtTime >= reduceCPSHurtTime.getLeft()
				&& mc.thePlayer.hurtTime <= reduceCPSHurtTime.getRight() && mc.objectMouseOver.entityHit != null
						? reduceCPS.getLeft()
						: attackCPS.getLeft();
		double max = mc.thePlayer.hurtTime >= reduceCPSHurtTime.getLeft()
				&& mc.thePlayer.hurtTime <= reduceCPSHurtTime.getRight() && mc.objectMouseOver.entityHit != null
						? reduceCPS.getRight()
						: attackCPS.getRight();

		float totalDelta = Math.abs(RotationComponent.rotations[0] - RotationComponent.lastRotations[0])
				+ Math.abs(RotationComponent.rotations[1] - RotationComponent.lastRotations[1]);
		if (totalDelta > (13 + behaviorNoise.GetNoise(behaviorCycle * 0.004f, 90.0f) * 4)) {
			max = Math.min(max, 9.5);
			min = Math.min(min, 6.5);
		}

		return clickHandler.getDelay(clickMode.getProperty(), min, max, mc.thePlayer.hurtTime > 0,
				mc.thePlayer.hurtTime);
	}

	private int targetTicks = 0;

	private void applyAimReward(float[] rots) {
		if (target == null)
			return;
		RotationUtil.PatternState state = this.patternState;
		double reward = 0.0;
		if (RayCastUtil.canHitCustom(rots, target, interactRange.getProperty())) {
			reward += 0.45;
			float dist = mc.thePlayer.getDistanceToEntity(target);
			if (dist >= 2.8f && dist <= 3.2f)
				reward += 0.05;
		} else {
			MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1.0f),
					target.getPositionVector().addVector(0, target.height * 0.5, 0));
			if (trace == null) {
				float[] directRots = RotationUtil.getRotations(target, false);
				float yawError = Math.abs(RotationUtil.normalizeAngleDelta(rots[0] - directRots[0]));
				float pitchError = Math.abs(rots[1] - directRots[1]);
				if (yawError + pitchError < 15.0f)
					reward += 0.15 * (1.0 - (yawError + pitchError) / 15.0f);
			}
		}
		if (state.lastTargetPos != null && state.secondLastTargetPos != null) {
			Vec3 targetVel = target.getPositionVector().subtract(state.lastTargetPos);
			if (targetVel.lengthVector() > 0.08) {
				reward += 0.15 * Math.max(0.0, Entity.getVectorForRotation(rots[1], rots[0])
						.dotProduct(target.getPositionVector().subtract(mc.thePlayer.getPositionVector()).normalize()));
				if (state.predictedTargetPos != null
						&& state.predictedTargetPos.distanceTo(target.getPositionVector()) < 0.5)
					reward += 0.10;
			}
		}
		if (state.lastPatternYaw != null && state.patternIndex > 10) {
			float totalDeltaR = Math.abs(RotationUtil.normalizeAngleDelta(rots[0] - state.lastPatternYaw))
					+ Math.abs(rots[1] - state.lastPatternPitch);
			float expectedDelta = 2.0f + (6.0f - mc.thePlayer.getDistanceToEntity(target)) * 1.2f;
			float smoothnessScore = (totalDeltaR < expectedDelta * 0.3f) ? totalDeltaR / (expectedDelta * 0.3f)
					: (totalDeltaR > expectedDelta * 2.5f
							? Math.max(0.0f, 1.0f - (totalDeltaR - expectedDelta * 2.5f) / expectedDelta)
							: 1.0f);
			reward += 0.20 * smoothnessScore;
		}
		double expectedAccuracy = (1.0 - state.sessionFatigue * 0.3) * (0.5 + state.microFocusLevel * 0.4);
		reward += 0.10 * Math.max(0.0, 1.0 - Math.abs((reward / 0.8) - expectedAccuracy));
		if (state.patternIndex > 50) {
			float variance = calculateRecentVariance(state);
			if (variance < 0.5f)
				reward -= 0.15 * (1.0f - variance / 0.5f);
		}
		RotationUtil.updateProfileReward(state, Math.clamp(reward, 0.0, 1.0));
	}

	private float calculateRecentVariance(RotationUtil.PatternState state) {
		return (state.accumulatedPath > 0 && state.patternIndex > 20)
				? Math.min(1.0f, (state.accumulatedPath / state.patternIndex) / 0.15f)
				: 0.5f;
	}

	@Listen(priority = Priority.HIGH)
	public void onRender(RenderOverlayEvent event) {
		if (!GothajClient.getInstance().getModuleManager().find(Debug.class).isToggled())
			return;

		if (!(rotations.is("Plain Pattern") || rotations.is("Randomized Pattern")))
			return;

		int startX = 150, startY = 9, colW = 80, lineH = 11, currentY = startY;
		Fonts.nunito12.drawStringWithShadow("§l§nMachine Learning", startX, currentY, Color.CYAN.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Average: §a%.3f", patternState.runningAvgReward), startX,
				currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Updates: §e%d", patternState.totalUpdates), startX, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Reward: §%s%.3f",
				patternState.lastReward >= 0.7 ? "a" : (patternState.lastReward >= 0.4 ? "e" : "c"),
				patternState.lastReward), startX, currentY, Color.GRAY.getRGB());
		currentY += lineH;
		int distB = 0, strafeB = 0, jumpB = 0, hitB = 0;
		if (target != null && mc.thePlayer != null) {
			distB = RotationUtil.getDistanceBucket(mc.thePlayer.getDistanceToEntity(target));
			strafeB = patternState.lastTargetVelocity != null
					? RotationUtil.getStrafeBucket(patternState.lastTargetVelocity)
					: 0;
			jumpB = (patternState.lastTargetVelocity != null && patternState.lastTargetVelocity.yCoord > 0.2
					&& !target.onGround) ? 1 : 0;
			hitB = (System.currentTimeMillis() - lastHit < 500) ? 1 : 0;
		}
		Fonts.nunito12.drawStringWithShadow(
				String.format("Context: §b%d §7(Dist:%d Strafe:%d)", patternState.lastContextIndex, distB, strafeB),
				startX, currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(
				String.format("Jump:%d Hit:%d Profile:§d%d", jumpB, hitB, patternState.lastProfileIndex), startX,
				currentY, Color.GRAY.getRGB());

		int col2X = startX + colW;
		currentY = startY;
		Fonts.nunito12.drawStringWithShadow("§l§nHuman", col2X, currentY, Color.ORANGE.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Fatigue: §%s%.1f%%",
				patternState.sessionFatigue * 100 > 70 ? "c" : (patternState.sessionFatigue * 100 > 40 ? "e" : "a"),
				patternState.sessionFatigue * 100), col2X, currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Focus: §%s%.1f%%",
				patternState.microFocusLevel * 100 > 60 ? "a" : (patternState.microFocusLevel * 100 > 35 ? "e" : "c"),
				patternState.microFocusLevel * 100), col2X, currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Skill: §b%.1f%%", patternState.baseReactionSkill * 100),
				col2X, currentY, Color.GRAY.getRGB());
		currentY += lineH;
		double hr = patternState.totalEngagements > 0
				? (double) patternState.totalSuccessfulHits / patternState.totalEngagements
				: 0.5;
		Fonts.nunito12.drawStringWithShadow(
				String.format("Hit Rate: §%s%.1f%%", hr > 0.65 ? "a" : (hr > 0.45 ? "e" : "c"), hr * 100), col2X,
				currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Engagements: §d%d", patternState.totalEngagements), col2X,
				currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Concentration: §d%d", patternState.concentrationCycle),
				col2X, currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Reaction: §e%d ticks", patternState.reactionDelayTicks),
				col2X, currentY, Color.GRAY.getRGB());

		int col3X = col2X + colW;
		currentY = startY;
		Fonts.nunito12.drawStringWithShadow("§l§nProfile #" + patternState.lastProfileIndex, col3X, currentY,
				Color.MAGENTA.getRGB());
		currentY += lineH;
		RotationUtil.PatternState.AimProfile prof = patternState.profiles[patternState.lastProfileIndex];
		Fonts.nunito12.drawStringWithShadow(String.format("Speed: §a%.2fx", prof.speedMul), col3X, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Yaw Range: §b%.2fx", prof.yawRangeMul), col3X, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Pitch Range: §b%.2fx", prof.pitchRangeMul), col3X, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Jitter: §e%.2fx", prof.jitterMul), col3X, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Overshoot: §c%.2fx", prof.overshootMul), col3X, currentY,
				Color.GRAY.getRGB());

		int col4X = col3X + colW;
		currentY = startY;
		Fonts.nunito12.drawStringWithShadow("§l§nPattern", col4X, currentY, Color.GREEN.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Index: §d%d", patternState.patternIndex), col4X, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Type: §b%s", patternState.currentPattern.name()), col4X,
				currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Height: §a%.2f", patternState.brownMotionHeight), col4X,
				currentY, Color.GRAY.getRGB());
		currentY += lineH;
		float sm = patternState.patternIndex > 0 ? patternState.accumulatedPath / patternState.patternIndex : 0;
		Fonts.nunito12.drawStringWithShadow(
				String.format("Smoothness: §%s%.3f", sm < 0.08 ? "c" : (sm > 0.15 ? "c" : "a"), sm), col4X, currentY,
				Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Path: §e%.1f", patternState.accumulatedPath), col4X,
				currentY, Color.GRAY.getRGB());
		currentY += lineH;
		Fonts.nunito12.drawStringWithShadow(String.format("Speed: §e%.1f", patternState.patternSpeed), col4X, currentY,
				Color.GRAY.getRGB());

		int col5X = col4X + colW;
		currentY = startY;
		Fonts.nunito12.drawStringWithShadow("§l§nTarget", col5X, currentY, Color.YELLOW.getRGB());
		currentY += lineH;
		if (target == null) {
			Fonts.nunito12.drawStringWithShadow("§7No Target", col5X, currentY, Color.GRAY.getRGB());
		} else {
			Fonts.nunito12.drawStringWithShadow(
					String.format("Distance: §a%.2f", mc.thePlayer.getDistanceToEntity(target)), col5X, currentY,
					Color.GRAY.getRGB());
			currentY += lineH;
			Fonts.nunito12.drawStringWithShadow(String.format("Jump Track: §%s%s", patternState.jumpFix ? "a" : "7",
					patternState.jumpFix ? "Yes" : "No"), col5X, currentY, Color.GRAY.getRGB());
			currentY += lineH;
			Fonts.nunito12.drawStringWithShadow(String.format("Prediction: §d%d/15", patternState.predictionConfidence),
					col5X, currentY, Color.GRAY.getRGB());
			currentY += lineH;
			if (patternState.lastTargetVelocity != null) {
				Fonts.nunito12.drawStringWithShadow(
						String.format("Velocity: §e%.3f", patternState.lastTargetVelocity.lengthVector()), col5X,
						currentY, Color.GRAY.getRGB());
				currentY += lineH;
			}
			Fonts.nunito12.drawStringWithShadow(String.format("CPS: §a%.3f", 1000 / clickDelay), col5X, currentY,
					Color.GRAY.getRGB());
			currentY += lineH;
			if (patternState.lastPatternYaw != null && RotationComponent.rotations != null) {
				float td = Math.abs(
						RotationUtil.normalizeAngleDelta(RotationComponent.rotations[0] - patternState.lastPatternYaw))
						+ Math.abs(RotationComponent.rotations[1] - patternState.lastPatternPitch);
				Fonts.nunito12.drawStringWithShadow(String.format("Angle Delta: §%s%.1f°", td > 10 ? "c" : "a", td),
						col5X, currentY, Color.GRAY.getRGB());
			}
		}
	}

	private class TargetPosition {
		@Getter
		@Setter
		double[] position;
		int delay;

		public TargetPosition(double[] ds, int delay) {
			this.position = ds;
			this.delay = delay;
		}
	}
}
