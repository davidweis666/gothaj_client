package team.gothaj.client.impl.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemPotion;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Action;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.api.util.player.RayCastUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.network.PacketEvent.Direction;
import team.gothaj.client.impl.event.player.AttackEntityEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.NormalUpdateEvent;
import team.gothaj.client.impl.event.player.PostMotionEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.module.movement.Flight;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;
import team.gothaj.eventbus.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Velocity extends Module {
	private final ModeProperty mode = new ModeProperty("Mode", "Vanilla", "Vanilla", "Jump", "Intave", "Grim",
			"Watchdog", "Delay", "MMC", "Polar 1.20", "BackTrack");

	private final NumberProperty minHurtTime = new NumberProperty("Min HurtTime", 1, 1, 10, 1)
			.setVisible(() -> mode.is("BackTrack"));
	private final NumberProperty minTargetHurtTime = new NumberProperty("Min Target HurtTime", 1, 1, 10, 1)
			.setVisible(() -> mode.is("BackTrack"));
	
	private final NumberProperty lagDelay = new NumberProperty("Delay", 100, 0, 2000, 1)
			.setVisible(() -> mode.is("Delay"));

	private final BooleanProperty jumpReset = new BooleanProperty("Jump Reset", true)
			.setVisible(() -> mode.is("Delay"));
	private final BooleanProperty lowTimer = new BooleanProperty("Low Timer", true).setVisible(() -> mode.is("Jump"));

	private final NumberProperty jumpChance = new NumberProperty("Jump Chance", 100, 1, 100, 1)
			.setVisible(() -> mode.is("Jump") || mode.is("Delay"));

	private final BooleanProperty stackVelocity = new BooleanProperty("Stack Velocity", true)
			.setVisible(() -> mode.is("Delay"));
	private final BooleanProperty ignoreWhenEating = new BooleanProperty("Ignore When Eating", true);

	private final NumberProperty releaseAfter = new NumberProperty("Release After", 10, 0, 100, 1)
			.setVisible(() -> mode.is("Delay") && stackVelocity.getProperty());

	private final NumberProperty horizontal = new NumberProperty("Horizontal", 0, -100, 100, 1)
			.setVisible(() -> mode.is("Vanilla"));
	private final NumberProperty vertical = new NumberProperty("Vertical", 0, -100, 100, 1)
			.setVisible(() -> mode.is("Vanilla"));
	private final BooleanProperty intaveJump = new BooleanProperty("Intave Jump", true)
			.setVisible(() -> mode.getProperty().equalsIgnoreCase("Intave"));
	private final BooleanProperty intaveStrong = new BooleanProperty("Intave Strong", true)
			.setVisible(() -> mode.getProperty().equalsIgnoreCase("Intave"));

	private final BooleanProperty wdAttackReduce = new BooleanProperty("Attack Reduce", true)
			.setVisible(() -> mode.is("Watchdog"));
	private final BooleanProperty wdBufferCorrect = new BooleanProperty("Buffer Correct", true)
			.setVisible(() -> mode.is("Watchdog"));
	private final BooleanProperty wdRotationJumpReset = new BooleanProperty("Rotation Jump Reset", true)
			.setVisible(() -> mode.is("Watchdog"));
	private final BooleanProperty wdAirPush = new BooleanProperty("Air Push", true).setVisible(() -> mode.is("Watchdog"));
	private final NumberProperty wdBufferTick = new NumberProperty("Buffer Tick", 4, 0, 10, 1)
			.setVisible(() -> mode.is("Watchdog"));
	private final BooleanProperty wdOnlyPlayer = new BooleanProperty("Only Attack Player", true)
			.setVisible(() -> mode.is("Watchdog"));
	private final BooleanProperty wdRiskMode = new BooleanProperty("Risk Mode", false)
			.setVisible(() -> mode.is("Watchdog") && wdAirPush.getProperty());

	private final ConcurrentLinkedQueue<Packet<?>> wdServerPackets = new ConcurrentLinkedQueue<>();
	private boolean wdForward, wdBuffer, wdShouldStrict, wdReceiveVelocity, wdHandleReset;
	private boolean wdSendingAttack, wdWasSprinting;
	private int wdHitCount, wdBufferTickCounter = -1, wdTicksSinceVelocity = -1;
	private float wdOppositeYaw, wdOppositePitch;
	private int wdLastKillAuraAttackTick = -1000;
	private final long[] wdInputPressTimes = new long[4];
	private final boolean[] wdLastInputStates = new boolean[4];

	public boolean blockVelocity;
	public int ticks, updates;
	private int hitTick = 0;

	public ConcurrentLinkedQueue<Short> transactions = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PacketUtil.DelayedPacket> velocityPackets = new ConcurrentLinkedQueue<>();
	public List<Packet<?>> packets = new ArrayList<>();
	public final TimerUtil lagTimer = new TimerUtil();
	private boolean shouldJump;
	private boolean releasing = false;
	private int jumpCoumt = 0;

	public Velocity() {
		super("Velocity", "Makes you not take kb.", ModuleCategory.COMBAT);
		registerProperties(mode, minHurtTime, minTargetHurtTime, lagDelay, jumpReset, jumpChance, ignoreWhenEating, stackVelocity, releaseAfter,
				horizontal, vertical, intaveJump, intaveStrong, wdAttackReduce, wdBufferCorrect,
				wdRotationJumpReset, wdAirPush, wdBufferTick, wdOnlyPlayer, wdRiskMode);
	}

	@Override
	public String getSuffix() {
		return mode.getProperty();
	}

	@Override
	public void onEnable() {
		ticks = 0;
		updates = 0;
		packets.clear();
		transactions.clear();
		blockVelocity = false;
		shouldJump = false;
		releasing = false;
		wdReset();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		for (PacketUtil.DelayedPacket packet : velocityPackets) {
			if (lagTimer.hasTimeReached(packet.delay(), lagDelay.getProperty().longValue(), true)) {
				PacketUtil.receiveNoEvent(packet.packet());
				velocityPackets.remove(packet);
				if (jumpReset.getProperty())
					shouldJump = true;
			}
		}
		wdReset();
		super.onDisable();
	}

	private void wdReset() {
		wdRestoreForcedKeys();
		wdForward = false;
		wdBuffer = false;
		wdShouldStrict = false;
		wdReceiveVelocity = false;
		wdHandleReset = false;
		wdSendingAttack = false;
		wdWasSprinting = false;
		wdHitCount = 0;
		wdBufferTickCounter = -1;
		wdTicksSinceVelocity = -1;
		wdOppositeYaw = mc.thePlayer != null ? mc.thePlayer.rotationYaw : 0.0F;
		wdOppositePitch = mc.thePlayer != null ? mc.thePlayer.rotationPitch : 0.0F;
		for (int i = 0; i < wdInputPressTimes.length; i++) {
			wdInputPressTimes[i] = 0L;
			wdLastInputStates[i] = false;
		}
		wdServerPackets.clear();
	}

	private int wdComputeReduceTicks(int motionX, int motionZ) {
		int result = (int) Math.round(0.000643153527 * Math.hypot(motionX, motionZ) + 2.9419087136);
		return MathHelper.clamp(result, 1, 10);
	}

	private void handleWatchdogPacket(PacketEvent event) {
		if (event.isCancelled() || event.getDirection() != PacketEvent.Direction.RECEIVE)
			return;

		Packet<?> packet = event.getPacket();

		if (packet instanceof S12PacketEntityVelocity velocity && velocity.getEntityID() == mc.thePlayer.getEntityId()
				&& !getCastedModule(Flight.class).isToggled()) {
			double velocityX = velocity.getMotionX() / 8000.0D;
			double velocityY = velocity.getMotionY() / 8000.0D;
			double velocityZ = velocity.getMotionZ() / 8000.0D;
			wdHandleIncomingMotion(velocityX, velocityY, velocityZ, velocity.getMotionX(), velocity.getMotionZ());
		} else if (packet instanceof S27PacketExplosion explosion && !getCastedModule(Flight.class).isToggled()) {
			double velocityX = explosion.func_149149_c();
			double velocityY = explosion.func_149144_d();
			double velocityZ = explosion.func_149147_e();
			wdHandleIncomingMotion(velocityX, velocityY, velocityZ,
					(int) Math.round(velocityX * 8000.0D),
					(int) Math.round(velocityZ * 8000.0D));
		}

		if (wdBuffer && !wdIsBufferBlacklist(packet)) {
			wdServerPackets.add(packet);
			event.setCancelled(true);
		}
	}

	private void wdHandleIncomingMotion(double velocityX, double velocityY, double velocityZ, int scaledMotionX,
			int scaledMotionZ) {
		double strength = Math.hypot(velocityX, velocityZ);

		if (velocityY <= 0.0D || strength < 0.1D || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater()) {
			return;
		}

		wdBeginIncomingMotion(velocityX, velocityZ, scaledMotionX, scaledMotionZ);
	}

	private void wdBeginIncomingMotion(double velocityX, double velocityZ, int scaledMotionX, int scaledMotionZ) {
		wdHitCount = wdComputeReduceTicks(scaledMotionX, scaledMotionZ);
		wdStoreOppositeRotation(velocityX, velocityZ);

		if (!wdBuffer) {
			KillAura killAura = getCastedModule(KillAura.class);
			boolean hasTarget = wdHasAttackReduceTarget(killAura)
					|| (wdAirPush.getProperty() && wdGetFarthestLivingEntity() != null);

			if (wdBufferCorrect.getProperty() && wdBufferTick.getProperty().intValue() != 0) {
				if (hasTarget) {
					if (wdIsSprinting()) {
						wdStartReceiveWindow(killAura);
					} else {
						wdBuffer = true;
						wdBufferTickCounter = 0;
					}
				} else {
					wdStartReceiveWindow(killAura);
				}
			} else {
				wdStartReceiveWindow(killAura);
			}
		}
	}

	private void tickWatchdog() {
		boolean sprinting = wdIsSprinting();

		if (wdTicksSinceVelocity >= 0)
			wdTicksSinceVelocity++;
		if (wdBufferTickCounter >= 0)
			wdBufferTickCounter++;

		if (wdTicksSinceVelocity >= 10) {
			wdReceiveVelocity = false;
			wdShouldStrict = false;
			wdTicksSinceVelocity = -1;
		}

		if (wdTicksSinceVelocity >= wdHitCount) {
			wdReceiveVelocity = false;
			wdShouldStrict = false;
		}

		wdHandleJumpReset();

		if (wdBuffer || wdHandleReset)
			wdShouldStrict = true;

		KillAura killAura = getCastedModule(KillAura.class);
		EntityLivingBase attackTarget = wdGetAttackReduceTarget(killAura);
		EntityLivingBase airPushTarget = wdAirPush.getProperty() ? wdGetFarthestLivingEntity() : null;

		if ((attackTarget != null || airPushTarget != null) && wdReceiveVelocity)
			wdShouldStrict = true;

		if (wdReceiveVelocity && sprinting) {
			if (attackTarget != null && wdAttackReduce.getProperty()) {
				wdTryAttackReduce(killAura, attackTarget);
			} else if (airPushTarget != null && wdAirPush.getProperty()) {
				wdSendFullPacketReduce(airPushTarget);
			}
		}

		wdWasSprinting = mc.thePlayer.isSprinting();
	}

	private void wdPostMotion() {
		if (!wdBuffer)
			return;

		KillAura killAura = getCastedModule(KillAura.class);

		if (wdBufferTickCounter >= wdBufferTick.getProperty().intValue() || wdIsSprinting()
				|| mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.moveSlowdown) || mc.thePlayer.onGround
				|| (!wdHasAttackReduceTarget(killAura)
						&& (!wdAirPush.getProperty() || wdGetFarthestLivingEntity() == null))) {
			wdFlushServerPackets();
			wdStartReceiveWindow(killAura);
			wdBuffer = false;
			wdBufferTickCounter = -1;
		}
	}

	private void wdStartReceiveWindow(KillAura killAura) {
		wdReceiveVelocity = true;
		wdTicksSinceVelocity = 0;
		if (mc.thePlayer.onGround && !getCastedModule(Scaffold.class).isToggled()
				&& wdRotationJumpReset.getProperty()) {
			wdApplyOppositeRotation(killAura);
			wdShouldStrict = true;
		}
	}

	private void wdStoreOppositeRotation(double velocityX, double velocityZ) {
		double targetX = mc.thePlayer.posX - velocityX;
		double targetZ = mc.thePlayer.posZ - velocityZ;
		wdOppositeYaw = (float) (Math.toDegrees(Math.atan2(targetZ - mc.thePlayer.posZ,
				targetX - mc.thePlayer.posX)) - 90.0D);
		wdOppositePitch = RotationComponent.rotations != null ? RotationComponent.rotations[1] : mc.thePlayer.rotationPitch;
	}

	private void wdApplyOppositeRotation(KillAura killAura) {
		EntityLivingBase closest = wdGetClosestLivingEntity(killAura != null ? KillAura.range.getProperty() : 3.0F);
		float targetPitch = closest != null ? RotationUtil.getRotations(closest, false)[1] : wdOppositePitch;
		RotationComponent.rotations = new float[] { wdOppositeYaw, targetPitch };
		RotationComponent.lastRotations = new float[] { wdOppositeYaw, targetPitch };
		RotationComponent.targetRotations = new float[] { wdOppositeYaw, targetPitch };
		RotationComponent.setRotations(wdOppositeYaw, targetPitch, 180, 180, true, false);
	}

	private void wdHandleJumpReset() {
		if (!wdRotationJumpReset.getProperty())
			return;

		if (mc.currentScreen != null) {
			wdRestoreForcedKeys();
			wdHandleReset = false;
			return;
		}

		if (wdTicksSinceVelocity >= 0 && !getCastedModule(Scaffold.class).isToggled()) {
			wdHandleReset = true;
			if (wdTicksSinceVelocity <= 2 && mc.thePlayer.onGround)
				mc.gameSettings.keyBindJump.pressed = true;
			if (wdTicksSinceVelocity <= 3) {
				mc.gameSettings.keyBindForward.pressed = true;
				wdForward = true;
			}
		}

		if (wdTicksSinceVelocity >= 4 && wdTicksSinceVelocity <= 9) {
			mc.gameSettings.keyBindJump.pressed = org.lwjgl.input.Keyboard
					.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
			if (wdForward) {
				mc.gameSettings.keyBindForward.pressed = org.lwjgl.input.Keyboard
						.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
				wdForward = false;
			}
			wdHandleReset = false;
		}
	}

	private boolean wdIsSprinting() {
		return mc.thePlayer.isSprinting() || wdWasSprinting;
	}

	private void wdRestoreForcedKeys() {
		mc.gameSettings.keyBindJump.pressed = org.lwjgl.input.Keyboard
				.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
		if (wdForward) {
			mc.gameSettings.keyBindForward.pressed = org.lwjgl.input.Keyboard
					.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
			wdForward = false;
		}
	}

	private void wdResolveModernInputs(MoveInputEvent event) {
		boolean[] states = { event.isKeyForward(), event.isKeyBack(), event.isKeyLeft(), event.isKeyRight() };
		long now = System.currentTimeMillis();

		for (int i = 0; i < states.length; i++) {
			if (states[i] && !wdLastInputStates[i]) {
				wdInputPressTimes[i] = now;
			}
			wdLastInputStates[i] = states[i];
		}

		if (event.isKeyForward() && event.isKeyBack()) {
			if (wdInputPressTimes[0] >= wdInputPressTimes[1]) {
				event.setKeyBack(false);
			} else {
				event.setKeyForward(false);
			}
		}

		if (event.isKeyLeft() && event.isKeyRight()) {
			if (wdInputPressTimes[2] >= wdInputPressTimes[3]) {
				event.setKeyRight(false);
			} else {
				event.setKeyLeft(false);
			}
		}
	}

	private boolean wdIsStillBlocking(KillAura killAura) {
		if (mc.thePlayer == null)
			return false;

		if (killAura != null && killAura.isWatchdogBlocking())
			return true;

		if (mc.thePlayer.isBlocking())
			return true;

		return mc.thePlayer.getHeldItem() != null
				&& mc.thePlayer.getHeldItem().getItem().getItemUseAction(mc.thePlayer.getHeldItem()) == EnumAction.BLOCK
				&& mc.gameSettings.keyBindUseItem.pressed;
	}

	private void wdTryAttackReduce(KillAura killAura, EntityLivingBase target) {
		if (wdIsStillBlocking(killAura))
			return;
		if (killAura != null && killAura.isAttacking)
			return;
		if (mc.thePlayer.ticksExisted == wdLastKillAuraAttackTick)
			return;
		if (RotationComponent.rotations == null)
			return;
		if (RayCastUtil.rayTrace(KillAura.range.getProperty(), RotationComponent.rotations) != target)
			return;

		wdSendFullPacketReduce(target);
	}

	private void wdSendFullPacketReduce(EntityLivingBase target) {
		if (wdIsStillBlocking(getCastedModule(KillAura.class)))
			return;

		wdSendingAttack = true;
		try {
			PacketUtil.send(new C0APacketAnimation());
			PacketUtil.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
		} finally {
			wdSendingAttack = false;
		}

		mc.thePlayer.motionX *= 0.6D;
		mc.thePlayer.motionZ *= 0.6D;
		mc.thePlayer.setSprinting(false);
	}

	private EntityLivingBase wdGetAttackReduceTarget(KillAura killAura) {
		float range = KillAura.range.getProperty();

		if (killAura != null && killAura.isToggled()) {
			EntityLivingBase target = KillAura.target;
			return wdIsAttackReduceTarget(target, range) && RotationComponent.rotations != null
					&& RayCastUtil.canHitCustom(RotationComponent.rotations, target, range) ? target : null;
		}

		if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase living
				&& wdIsAttackReduceTarget(living, range)) {
			return living;
		}

		return null;
	}

	private boolean wdHasAttackReduceTarget(KillAura killAura) {
		float range = KillAura.range.getProperty();

		if (killAura != null && killAura.isToggled()) {
			return wdIsAttackReduceTarget(KillAura.target, range);
		}

		return mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase living
				&& wdIsAttackReduceTarget(living, range);
	}

	private boolean wdIsAttackReduceTarget(EntityLivingBase living, double range) {
		return living != null && living != mc.thePlayer && wdIsValidTarget(living)
				&& EntityUtil.getDistanceToEntityBox(living) <= range;
	}

	private EntityLivingBase wdGetClosestLivingEntity(double range) {
		EntityLivingBase target = null;
		double closestDistance = range;

		if (mc.theWorld == null)
			return null;

		for (Entity entity : mc.theWorld.getLoadedEntityList()) {
			if (!(entity instanceof EntityLivingBase living) || living == mc.thePlayer
					|| living instanceof EntityArmorStand || !wdIsValidTarget(living)) {
				continue;
			}

			double distance = EntityUtil.getDistanceToEntityBox(living);
			if (distance <= closestDistance) {
				closestDistance = distance;
				target = living;
			}
		}

		return target;
	}

	private EntityLivingBase wdGetFarthestLivingEntity() {
		EntityLivingBase target = null;
		double farthestDistance = 0.0D;

		if (mc.theWorld == null)
			return null;

		for (Entity entity : mc.theWorld.getLoadedEntityList()) {
			if (!(entity instanceof EntityLivingBase living) || living == mc.thePlayer
					|| living instanceof EntityArmorStand || !wdIsValidTarget(living)) {
				continue;
			}

			double distance = EntityUtil.getDistanceToEntityBox(living);
			if (distance < 11.0D && !wdRiskMode.getProperty())
				continue;

			if (distance > farthestDistance) {
				farthestDistance = distance;
				target = living;
			}
		}

		return target;
	}

	private boolean wdIsValidTarget(EntityLivingBase living) {
		if (wdOnlyPlayer.getProperty())
			return living instanceof EntityPlayer;

		return living instanceof EntityPlayer || living instanceof EntityMob || living instanceof EntityVillager;
	}

	private boolean wdIsBufferBlacklist(Packet<?> packet) {
		return packet instanceof S06PacketUpdateHealth || packet instanceof S08PacketPlayerPosLook
				|| packet instanceof S29PacketSoundEffect || packet instanceof S02PacketChat
				|| packet instanceof S42PacketCombatEvent || packet instanceof S2EPacketCloseWindow
				|| packet instanceof S0BPacketAnimation animation && animation.getEntityID() != mc.thePlayer.getEntityId()
				|| packet instanceof S45PacketTitle || packet instanceof S3EPacketTeams;
	}

	private void wdFlushServerPackets() {
		while (!wdServerPackets.isEmpty()) {
			PacketUtil.receiveNoEvent(wdServerPackets.poll());
		}
	}

	public boolean shouldBlockKillAuraAttackThisTick(EntityLivingBase target) {
		KillAura killAura = getCastedModule(KillAura.class);
		return mode.is("Watchdog") && wdAttackReduce.getProperty() && wdReceiveVelocity
				&& (wdIsStillBlocking(killAura)
						|| target != null && target == wdGetAttackReduceTarget(killAura));
	}

	@Listen
	public void onWorldChange(WorldChangeEvent event) {
		ticks = 0;
		packets.clear();
		blockVelocity = false;

		if (mode.getProperty().equalsIgnoreCase("Grim")) {
			updates = 8;
		}

		if (mode.is("Watchdog")) {
			wdReset();
		}
	}

	@Listen
	public void onPacket(PacketEvent event) {
		if (mode.is("Watchdog") && event.getDirection() == PacketEvent.Direction.SEND
				&& event.getPacket() instanceof C02PacketUseEntity useEntity
				&& useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
			if (wdReceiveVelocity && wdIsStillBlocking(getCastedModule(KillAura.class))) {
				event.setCancelled(true);
				return;
			}

			if (!wdSendingAttack) {
				wdLastKillAuraAttackTick = mc.thePlayer.ticksExisted;
			}
		}

		if (!mode.is("Watchdog") && ignoreWhenEating.getProperty() && mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse().getItem()
				.getItemUseAction(mc.thePlayer.getItemInUse()) == EnumAction.EAT)
			return;
		if (!mode.is("Watchdog") && ignoreWhenEating.getProperty() && mc.thePlayer.getHeldItem() != null
				&& mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion)
			return;

		switch (mode.getProperty()) {
		case "BackTrack":
			if (event.getDirection() == Direction.RECEIVE && !event.isCancelled()) {
				EntityLivingBase target = EntityUtil.getTarget(3);
				if (mc.thePlayer.hurtTime >= minHurtTime.getProperty() && target != null && target.hurtTime >= minTargetHurtTime.getProperty()) {
					if (blockIncomingPacket(event.getPacket())) {
						GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class).offerInboundPacket(event.getPacket());
						event.setCancelled(true);
					}
				}else {
					
				}
			}
			break;
		case "Polar 1.20":
			AxisAlignedBB col = mc.thePlayer.getEntityBoundingBox();
			if (event.getPacket() instanceof S12PacketEntityVelocity p
					&& p.getEntityID() == mc.thePlayer.getEntityId()) {
				for (Entity e : Minecraft.getMinecraft().theWorld.getLoadedEntityList()) {
					if (e.getEntityId() != mc.thePlayer.getEntityId() && col.intersectsWith(e.getEntityBoundingBox())) {
						event.setCancelled(true);
						mc.thePlayer.motionX = 0;
						mc.thePlayer.motionZ = 0;

					}
				}
			}
			break;
		case "Vanilla":
			if (event.getPacket() instanceof S12PacketEntityVelocity p
					&& p.getEntityID() == mc.thePlayer.getEntityId()) {
				final int h = this.horizontal.getProperty().intValue();
				final int v = this.vertical.getProperty().intValue();
				p.setMotionX((p.getMotionX() / 100) * h);
				p.setMotionY((p.getMotionY() / 100) * v);
				p.setMotionZ((p.getMotionZ() / 100) * h);
				if (h == 0 && v == 0) {
					event.setCancelled(true);
				}
			}
			break;

		case "MMC":
			if (event.getPacket() instanceof S12PacketEntityVelocity p
					&& p.getEntityID() == mc.thePlayer.getEntityId()) {
				if (mc.thePlayer.onGround) {
					mc.thePlayer.setSprinting(true);
					mc.thePlayer.jump();
					mc.timer.timerSpeed = 0.7f;
				} else {
					mc.timer.timerSpeed = 0.8f;
				}
				GothajClient.getInstance().getActionManager().queue(new Action(MovementUtil::boost, 2));
				GothajClient.getInstance().getActionManager().queue(new Action(() -> mc.timer.timerSpeed = 1.0f, 2));
				GothajClient.getInstance().getActionManager().queue(new Action(() -> {
					mc.thePlayer.motionX *= 0.981;
					mc.thePlayer.motionZ *= 0.981;
				}, 1));
			}
			break;

		case "Watchdog":
			handleWatchdogPacket(event);
			break;

		case "Delay":
			if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
				if (event.getPacket() instanceof S12PacketEntityVelocity packet) {
					if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
						velocityPackets.add(new PacketUtil.DelayedPacket(packet, System.currentTimeMillis()));
						event.setCancelled(true);
					}
				} else if (event.getPacket() instanceof S27PacketExplosion packet) {
					velocityPackets.add(new PacketUtil.DelayedPacket(packet, System.currentTimeMillis()));
					event.setCancelled(true);
				}
			}
			break;

		case "Grim":
			if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
				if (event.getPacket() instanceof S08PacketPlayerPosLook packet) {
					if ((packet.x == mc.thePlayer.posX && packet.y == mc.thePlayer.posY
							&& packet.z == mc.thePlayer.posZ)
							|| (packet.x == mc.thePlayer.lastReportedPosX && packet.y == mc.thePlayer.lastReportedPosY
									&& packet.z == mc.thePlayer.lastReportedPosZ)) {
						return;
					}
					updates = 8;
				}

				if (updates > 0) {
					transactions.clear();
					blockVelocity = false;
					return;
				}

				if (event.getPacket() instanceof S12PacketEntityVelocity
						&& ((S12PacketEntityVelocity) event.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
					event.setCancelled(true);
					blockVelocity = true;
				} else if (event.getPacket() instanceof S32PacketConfirmTransaction packet) {
					if (!blockVelocity)
						return;
					transactions.add(packet.getActionNumber());
					event.setCancelled(true);
				}
			} else {
				if (updates > 0) {
					transactions.clear();
					blockVelocity = false;
					return;
				}
				if (event.getPacket() instanceof C0FPacketConfirmTransaction packet) {
					if (!blockVelocity || transactions.isEmpty())
						return;
					if (transactions.remove(((C0FPacketConfirmTransaction) packet).getUid())) {
						event.setCancelled(true);
					}
				}
			}
			break;
		}
	}

	@Listen
	public void onMoveButton(MoveInputEvent event) {
		switch (mode.getProperty().toLowerCase()) {
		case "watchdog":
			if (wdHandleReset || wdBuffer || wdShouldStrict) {
				event.setKeyForward(true);
			}
			wdResolveModernInputs(event);
			break;

		case "delay":
		case "jump":
			if (mode.is("Jump")) {
				if (mc.thePlayer.hurtTime > 0 && !mc.thePlayer.isInWeb) {
					Entity attacker = mc.thePlayer.getLastAttacker();
					if (attacker != null && attacker != mc.thePlayer && attacker instanceof EntityOtherPlayerMP) {
						if (Math.random() <= (jumpChance.getProperty() / 100)) {
							event.setKeyForward(true);
							Logger.debugChat(this, "forward key");
							if (mc.thePlayer.onGround && mc.thePlayer.hurtTime > 8) {
								Logger.debugChat(this, "jump key");
								event.setKeyJump(true);
							}
						}
					}
				}
			} else if (mode.is("Delay")) {
				if (shouldJump && Math.random() <= (jumpChance.getProperty() / 100)) {
					Entity attacker = mc.thePlayer.getLastAttacker();
					if (attacker != null && attacker != mc.thePlayer) {
						event.setKeyForward(true);
						if (mc.thePlayer.onGround && mc.thePlayer.hurtTime > 8) {
							event.setKeyJump(true);
							shouldJump = false;
						}
					}
				}
			}
			break;

		case "intave":
			if (mc.objectMouseOver != null && mc.thePlayer.hurtTime > 0 && !mc.thePlayer.isBurning()
					&& !mc.thePlayer.isInWeb && mc.objectMouseOver.entityHit != null) {
				event.setKeyForward(true);
				if (mc.thePlayer.hurtTime > 8) {
					if (intaveJump.getProperty())
						event.setKeyJump(true);
				}
			}
			break;
		}
	}

	@Listen
	public void onPostMotion(PostMotionEvent event) {
		if (mode.is("Watchdog")) {
			wdPostMotion();
		}
	}

	@Listen(priority = Priority.HIGH)
	public void onMouseClick(MouseClickEvent event) {
		if (mode.is("Watchdog")) {
			tickWatchdog();
		}
	}

	@Listen
	public void onAttack(AttackEntityEvent event) {
		if (mc.objectMouseOver == null)
			return;

		switch (mode.getProperty().toLowerCase()) {
		case "intave":
			if (mc.objectMouseOver.entityHit != null) {
				if (mc.thePlayer.hurtTime > 0 && blockVelocity
						&& (intaveStrong.getProperty() || mc.thePlayer.isSprinting())) {
					mc.thePlayer.setSprinting(false);
					mc.thePlayer.motionX *= 0.6;
					mc.thePlayer.motionZ *= 0.37650;
					blockVelocity = false;
				}
			}
			break;
		}
	}

	@Listen
	public void onNormalUpdate(NormalUpdateEvent event) {
	}

	@Listen
	public void onUpdate(LivingUpdateEvent event) {
		switch (mode.getProperty()) {
		case "Polar":
			break;
		case "Intave":
			blockVelocity = true;
			break;

		case "Delay":
			if (stackVelocity.getProperty()) {
				if (velocityPackets.size() >= releaseAfter.getProperty() && !releasing) {
					releasing = true;
					lagTimer.reset();
				}
				if (releasing) {
					for (PacketUtil.DelayedPacket packet : velocityPackets) {
						if (lagTimer.hasTimeReached(packet.delay(), lagDelay.getProperty().longValue(), true)) {
							PacketUtil.receiveNoEvent(packet.packet());
							velocityPackets.remove(packet);
							if (jumpReset.getProperty())
								shouldJump = true;
							break;
						}
					}
					if (velocityPackets.isEmpty()) {
						releasing = false;
					}
				}
			} else {
				for (PacketUtil.DelayedPacket packet : velocityPackets) {
					if (lagTimer.hasTimeReached(packet.delay(), lagDelay.getProperty().longValue(), true)) {
						PacketUtil.receiveNoEvent(packet.packet());
						velocityPackets.remove(packet);
						if (jumpReset.getProperty())
							shouldJump = true;
					}
				}
			}
			break;

		case "Grim":
			updates--;
			if (packets.isEmpty() && blockVelocity) {
				blockVelocity = false;
			}
			break;

		}
	}

	private boolean blockIncomingPacket(Packet packet) {
		return (packet instanceof S32PacketConfirmTransaction || packet instanceof S27PacketExplosion
				|| packet instanceof S12PacketEntityVelocity || packet instanceof S00PacketKeepAlive
				|| packet instanceof S03PacketTimeUpdate || packet instanceof S14PacketEntity
				|| packet instanceof S19PacketEntityHeadLook || packet instanceof S18PacketEntityTeleport
				|| packet instanceof S0FPacketSpawnMob || packet instanceof S08PacketPlayerPosLook
				|| (packet instanceof S01PacketPong));
	}
}
