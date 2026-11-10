package team.gothaj.client.impl.module.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.player.SimulatedPlayer;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.player.AttackEntityEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MoreKB extends Module {

	private final ModeProperty mode = new ModeProperty("Mode", "Smart", "Smart", "Sprint Tap", "WTap", "STap", "Sneak",
			"Basic", "Packet Legit", "Combo");
	
	private final NumberProperty maxHurtTime = new NumberProperty("Max Hurt Time", 10, 0, 10, 1).setVisible(() -> mode.is("Basic"));

	private final NumberProperty delayAfterHit = new NumberProperty("Delay After Hit", 30, 0, 150, 1)
			.setVisible(() -> !mode.is("Basic"));

	private final NumberProperty stopDuration = new NumberProperty("Stop Duration", 40, 5, 200, 1)
			.setVisible(() -> !mode.is("Basic"));

	private final BooleanProperty randomize = new BooleanProperty("Randomize", true).setVisible(() -> !mode.is("Basic"));

	private final NumberProperty randomRange = new NumberProperty("Random Range", 15, 0, 50, 1)
			.setVisible(randomize::getProperty).setVisible(() -> !mode.is("Basic"));

	private final BooleanProperty onlyMoving = new BooleanProperty("Only Moving", true)
			.setVisible(() -> !mode.is("Basic"));

	private final BooleanProperty prediction = new BooleanProperty("Prediction", true)
			.setVisible(() -> !mode.is("Basic"));

	private final NumberProperty predictionTicks = new NumberProperty("Prediction Ticks", 3, 1, 6, 1)
			.setVisible(prediction::getProperty).setVisible(() -> !mode.is("Basic"));

	private final BooleanProperty distanceCheck = new BooleanProperty("Distance Check", true)
			.setVisible(() -> !mode.is("Basic"));

	private final NumberProperty minDistance = new NumberProperty("Min Distance", 2.5f, 1.0f, 5.0f, 0.1f)
			.setVisible(distanceCheck::getProperty).setVisible(() -> !mode.is("Basic"));

	private final BooleanProperty debug = new BooleanProperty("Debug", false).setVisible(() -> !mode.is("Basic"));

	private long lastHitTime = 0L;
	private boolean pendingReset = false;
	private long resetStartTime = 0L;
	private int comboCount = 0;
	private long lastRandomDelay = 0L;
	private long lastRandomDuration = 0L;
	private String currentSmartMode = "WTap";

	private final List<SimulationProcess> simulationProcesses = new ArrayList<>();

	public MoreKB() {
		super("MoreKB", "Deals more knockback when attacking!", ModuleCategory.COMBAT);
		registerProperties(mode, maxHurtTime, delayAfterHit, stopDuration, randomize, randomRange, onlyMoving, prediction,
				predictionTicks, distanceCheck, minDistance, debug);
	}

	@Override
	public String getSuffix() {
		return mode.is("Smart") ? "Smart/" + currentSmartMode : mode.getProperty();
	}

	@Override
	public void onDisable() {
		pendingReset = false;
		resetStartTime = 0L;
		comboCount = 0;
		simulationProcesses.clear();
		super.onDisable();
	}

	@Listen
	public void onAttack(LivingUpdateEvent event) {
		if (mode.is("Basic")) {
			EntityLivingBase hitEntity = detectHit();
			if (hitEntity != null && hitEntity.hurtTime == 10 && mc.thePlayer.hurtTime <= maxHurtTime.getProperty()) {
				mc.thePlayer.reSprint = 2;
			}
		}
	}

	@Listen
	public void onMoveInput(MoveInputEvent event) {
		if (mc.thePlayer == null || mc.theWorld == null)
			return;

		if (prediction.getProperty()) {
			updatePrediction();
		}

		EntityLivingBase hitEntity = detectHit();

		if (onlyMoving.getProperty() && !mc.gameSettings.keyBindForward.isKeyDown()) {
			return;
		}

		if (distanceCheck.getProperty() && hitEntity != null) {
			double distance = mc.thePlayer.getDistanceToEntity(hitEntity);
			if (distance < minDistance.getProperty().doubleValue()) {
				return;
			}
		}

		if (prediction.getProperty() && hitEntity != null && !simulationProcesses.isEmpty()) {
			Vec3 predictedPos = simulationProcesses.getLast().position;
			double predictedDistance = predictedPos.distanceTo(hitEntity.getPositionVector());

			if (predictedDistance < 2.0) {
				return;
			}
		}

		if (hitEntity != null) {
			lastHitTime = System.currentTimeMillis();
			pendingReset = true;
			comboCount++;

			if (randomize.getProperty()) {
				int range = randomRange.getProperty().intValue();
				lastRandomDelay = ThreadLocalRandom.current().nextLong(-range, range + 1);
				lastRandomDuration = ThreadLocalRandom.current().nextLong(-range / 2, range / 2 + 1);
			} else {
				lastRandomDelay = 0;
				lastRandomDuration = 0;
			}
			if (mode.is("Smart")) {
				currentSmartMode = selectSmartMode(hitEntity);
			}

			if (debug.getProperty()) {
				double dist = mc.thePlayer.getDistanceToEntity(hitEntity);
				Logger.debugChat(this,
						String.format(
								"§aHit #%d §7| Mode: §f%s §7| Delay: §e%dms §7| Duration: §e%dms §7| Dist: §b%.1f",
								comboCount, mode.is("Smart") ? "Smart/" + currentSmartMode : mode.getProperty(),
								delayAfterHit.getProperty().longValue() + lastRandomDelay,
								stopDuration.getProperty().longValue() + lastRandomDuration, dist));
			}
		}

		long now = System.currentTimeMillis();
		long delayMs = delayAfterHit.getProperty().longValue() + lastRandomDelay;
		long durationMs = stopDuration.getProperty().longValue() + lastRandomDuration;

		delayMs = Math.max(0, delayMs);
		durationMs = Math.max(5, durationMs);

		String activeMode = mode.is("Smart") ? currentSmartMode : mode.getProperty();

		switch (activeMode) {
		case "Sprint Tap" -> {
			if (hitEntity != null && hitEntity.hurtTime == 10) {
				mc.thePlayer.reSprint = 2;
			}
		}

		case "WTap" -> handleWTap(event, now, delayMs, durationMs);
		case "STap" -> handleSTap(event, now, delayMs, durationMs);
		case "Sneak" -> handleSneak(event, now, delayMs, durationMs);
		case "Combo" -> handleCombo(event, now, delayMs, durationMs, hitEntity);
		}
	}

	private void updatePrediction() {
		simulationProcesses.clear();

		SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
		sim.rotationYaw = RotationComponent.isActive() ? RotationComponent.rotations[0] : mc.thePlayer.rotationYaw;

		int ticks = predictionTicks.getProperty().intValue();
		SimulatedPlayer.enterSpeculativeMovement();
		try {
			for (int i = 0; i < ticks; i++) {
				sim.tick();
				simulationProcesses.add(new SimulationProcess(sim.getPos(), sim.onGround));
			}
		} finally {
			SimulatedPlayer.leaveSpeculativeMovement();
		}
	}

	private EntityLivingBase detectHit() {
		EntityLivingBase hitEntity = null;

		if (getCastedModule(KillAura.class).isToggled() && KillAura.target != null && KillAura.target.hurtTime == 10) {
			hitEntity = KillAura.target;
		} else if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase e) {
			if (e.hurtTime == 10) {
				hitEntity = e;
			}
		}

		return hitEntity;
	}

	private String selectSmartMode(EntityLivingBase target) {
		double distance = mc.thePlayer.getDistanceToEntity(target);
		boolean targetMovingAway = isTargetMovingAway(target);
		boolean inAir = !mc.thePlayer.onGround;

		if (inAir) {
			return "Sprint Tap";
		}

		if (targetMovingAway) {
			return "WTap";
		}

		if (comboCount % 3 == 0) {
			return "Sneak";
		} else if (comboCount % 2 == 0) {
			return "STap";
		}

		return "WTap";
	}

	private boolean isTargetMovingAway(EntityLivingBase target) {
		double dx = target.posX - target.lastTickPosX;
		double dz = target.posZ - target.lastTickPosZ;

		double toTargetX = target.posX - mc.thePlayer.posX;
		double toTargetZ = target.posZ - mc.thePlayer.posZ;

		return (dx * toTargetX + dz * toTargetZ) > 0;
	}

	private void handleWTap(MoveInputEvent event, long now, long delayMs, long durationMs) {
		if (!pendingReset)
			return;

		if (resetStartTime == 0L && now - lastHitTime >= delayMs) {
			resetStartTime = now;
			event.setKeyForward(false);
			if (debug.getProperty()) {
				Logger.debugChat(this, "§eWTap §7- W key released");
			}
		}

		if (resetStartTime != 0L && now - resetStartTime >= durationMs) {
			if (debug.getProperty()) {
				Logger.debugChat(this, "§aWTap complete §7- " + (now - lastHitTime) + "ms total");
			}
			pendingReset = false;
			resetStartTime = 0L;
		}
	}

	private void handleSTap(MoveInputEvent event, long now, long delayMs, long durationMs) {
		if (!pendingReset)
			return;

		if (resetStartTime == 0L && now - lastHitTime >= delayMs) {
			resetStartTime = now;
			event.setKeyBack(true);
			event.setKeyForward(false);
			if (debug.getProperty()) {
				Logger.debugChat(this, "§eSTap §7- S key pressed, W released");
			}
		}

		if (resetStartTime != 0L && now - resetStartTime >= durationMs) {
			event.setKeyBack(false);
			if (debug.getProperty()) {
				Logger.debugChat(this, "§aSTap complete §7- " + (now - lastHitTime) + "ms total");
			}
			pendingReset = false;
			resetStartTime = 0L;
		}
	}

	private void handleSneak(MoveInputEvent event, long now, long delayMs, long durationMs) {
		if (!pendingReset)
			return;

		if (resetStartTime == 0L && now - lastHitTime >= delayMs) {
			resetStartTime = now;
			event.setKeySneak(true);
			mc.thePlayer.setSprinting(false);
			if (debug.getProperty()) {
				Logger.debugChat(this, "§eSneak §7- Sneaking, sprint stopped");
			}
		}

		if (resetStartTime != 0L && now - resetStartTime >= durationMs) {
			event.setKeySneak(false);
			if (debug.getProperty()) {
				Logger.debugChat(this, "§aSneak complete §7- " + (now - lastHitTime) + "ms total");
			}
			pendingReset = false;
			resetStartTime = 0L;
		}
	}

	private void handleCombo(MoveInputEvent event, long now, long delayMs, long durationMs, EntityLivingBase target) {
		if (!pendingReset)
			return;

		int technique = comboCount % 4;
		String[] techniqueNames = { "WTap", "SprintTap", "Sneak" };

		if (resetStartTime == 0L && now - lastHitTime >= delayMs) {
			resetStartTime = now;

			switch (technique) {
			case 0 -> event.setKeyForward(false);
			case 2 -> mc.thePlayer.reSprint = 2;
			case 3 -> {
				event.setKeySneak(true);
				mc.thePlayer.setSprinting(false);
			}
			}

			if (debug.getProperty()) {
				Logger.debugChat(this, "§eCombo §7- Using §f" + techniqueNames[technique]);
			}
		}

		if (resetStartTime != 0L && now - resetStartTime >= durationMs) {
			switch (technique) {
			case 1 -> event.setKeyBack(false);
			case 3 -> event.setKeySneak(false);
			}

			if (debug.getProperty()) {
				Logger.debugChat(this, "§aCombo complete §7- " + techniqueNames[technique] + " §7- "
						+ (now - lastHitTime) + "ms total");
			}
			pendingReset = false;
			resetStartTime = 0L;
		}
	}

	private record SimulationProcess(Vec3 position, boolean onGround) {
	}
}
