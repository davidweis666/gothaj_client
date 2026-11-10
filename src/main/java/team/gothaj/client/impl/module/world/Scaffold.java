package team.gothaj.client.impl.module.world;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSnow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import systems.vajzii.irc.main.IrcManager;
import systems.vajzii.irc.players.IrcRank;
import systems.vajzii.irc.util.IRCUtil;
import team.gothaj.client.api.component.ComponentManager;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.progress.Progress;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.property.properties.RangedNumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.ClickHandler;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.math.FastNoiseLite;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.component.ProgressComponent;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.misc.ProgressEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.*;
import team.gothaj.client.impl.event.player.item.SpoofClientItemEvent;
import team.gothaj.client.impl.event.player.move.JumpEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.event.player.move.PostMoveEvent;
import team.gothaj.client.impl.event.player.move.PreMoveEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.client.impl.event.render.shader.ShaderOverlayEvent;
import team.gothaj.client.impl.module.combat.AntiFireball;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.impl.module.visual.widget.BlockCountWidget;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

public class Scaffold extends Module {

	private static final Set<String> servers = Set.of("mineblaze.net", "gommehd.net", "gamster.org", "pika.host", "mineberry.org", "coralmc.it", "mineblaze.ru");
	private static final double[] CLUTCH_PLACE_OFFSETS = { 0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375,
			0.40625, 0.46875, 0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875 };

	public int posY, itemBefore, sneakPlaced, placed, offGroundTicks;
	private float scaffoldYaw;
	private float scaffoldPitch;
	private float watchdog2TargetYaw;
	private float watchdog2TargetPitch;
	private float watchdog2SpeedNoise;
	private int watchdog2NoiseUpdateTimer;

	private BlockPos blockPos;
	private EnumFacing facing;
	private Vec3 tellyHitVec;
	private Vec3 watchdog2HitVec;

	public static int spoofSlot = -1;

	private int towerGroundTicks, towerStage;
	private boolean tellyTowering;
	private boolean tellyForwardSnapped;
	private int tellyVelocityTicks;
	private boolean towering = false;
	private boolean requestTower = false;

	private final ModeProperty mode = new ModeProperty("Mode", "Normal", "Normal", "Telly", "Retarded");
	private final ModeProperty rotations = new ModeProperty("Rotations", "Intave", "Intave", "God Bridge", "Custom",
			"Polar", "Watchdog 2").setVisible(() -> mode.is("Normal") || mode.is("Telly"));

	private final BooleanProperty polarJump = new BooleanProperty("Polar Jump", true)
			.setVisible(() -> rotations.is("Polar"));
	private final BooleanProperty polarJumpProgressBar = new BooleanProperty("Polar Jump Progress Bar", true)
			.setVisible(() -> rotations.is("Polar") && polarJump.getProperty());
	private final BooleanProperty polarStrongRotations = new BooleanProperty("Polar Strong Rotations", false)
			.setVisible(() -> rotations.is("Polar"));
	private final BooleanProperty polarFastWalk = new BooleanProperty("Polar Fast Walk", true)
			.setVisible(() -> rotations.is("Polar"));
	private final BooleanProperty polarTellyTower = new BooleanProperty("Polar Telly Tower", false)
			.setVisible(() -> rotations.is("Polar"));

	private final NumberProperty customYaw = new NumberProperty("Yaw", 180, -180, 180, 1)
			.setVisible(() -> mode.is("Normal") && rotations.is("Custom"));
	private final NumberProperty customPitch = new NumberProperty("Pitch", 82.5f, -90.0f, 90.0f, 0.5f)
			.setVisible(() -> mode.is("Normal") && rotations.is("Custom"));

	private final BooleanProperty sideways = new BooleanProperty("Sideways", false)
			.setVisible(() -> !rotations.is("Polar"))
			.setVisible(() -> !mode.is("Telly") && !rotations.is("God Bridge"));

	private final ModeProperty sprint = new ModeProperty("Sprint", "Always", "Always", "Off", "Legit");
	private final ModeProperty tower = new ModeProperty("Tower", "None", "None", "NCP", "Timer", "Intave", "Watchdog",
			"Polar");

	private final BooleanProperty sneak = new BooleanProperty("Sneak", false)
			.setVisible(() -> mode.is("Normal") && !rotations.is("Polar"));
	private final NumberProperty sneakDelay = new NumberProperty("Sneak Delay", 4, 0, 30, 1)
			.setVisible(() -> mode.is("Normal") && sneak.getProperty() && !rotations.is("Polar"));

	private final BooleanProperty safeWalk = new BooleanProperty("Safe Walk", true)
			.setVisible(() -> mode.is("Normal") && !rotations.is("Polar"));

	private final BooleanProperty rayTrace = new BooleanProperty("Ray Trace", true);

	private final BooleanProperty moveFix = new BooleanProperty("Move Fix", true);
	private final BooleanProperty keepY = new BooleanProperty("Keep Y", true).setVisible(() -> !rotations.is("Polar"));
	private final BooleanProperty slotSpoof = new BooleanProperty("Spoof Slot", true);
	private final BooleanProperty swing = new BooleanProperty("Swing", false);
	private final BooleanProperty jump = new BooleanProperty("Jump", false)
			.setVisible(() -> mode.is("Normal") && !rotations.is("Polar"));

	private final BooleanProperty dragClick = new BooleanProperty("Drag Click", false);
	private final RangedNumberProperty dragClickCPS = new RangedNumberProperty("Drag Click CPS", 20, 20, 1, 40, 1);

	private final BooleanProperty renderCount = new BooleanProperty("Render Count", false);
	private final BooleanProperty thirdPerson = new BooleanProperty("Third Person", false);

	private final NumberProperty watchdogRotationSpeed = new NumberProperty("Watchdog Rotation Speed", 30, 10, 180, 10)
			.setVisible(() -> mode.is("Normal") && (rotations.is("Intave") || rotations.is("Watchdog")));
	private final RangedNumberProperty watchdog2RotationSpeed = new RangedNumberProperty("Watchdog 2 Rotation Speed", 110, 120,
			40, 130, 1).setVisible(() -> (mode.is("Normal") || mode.is("Telly")) && rotations.is("Watchdog 2"));
	private final BooleanProperty watchdog2Noise = new BooleanProperty("Watchdog 2 Noise", true)
			.setVisible(() -> (mode.is("Normal") || mode.is("Telly")) && rotations.is("Watchdog 2"));
	private final NumberProperty tellyFirstYawSpeed = new NumberProperty("Telly First Yaw Speed", 30, 1, 130, 1)
			.setVisible(() -> mode.is("Telly"));
	private final NumberProperty tellyAirYawSpeed = new NumberProperty("Telly Air Yaw Speed", 39, 1, 40, 1)
			.setVisible(() -> mode.is("Telly"));
	private final NumberProperty smoothSpeed = new NumberProperty("Smooth Speed", 30, 10, 180, 10)
			.setVisible(() -> !mode.is("Telly"));

	private final TimerUtil polarTimer = new TimerUtil();
	private final TimerUtil addBlockTimer = new TimerUtil();

	private final TimerUtil dragClickDelayTimer = new TimerUtil();
	private int dragClicks;

	public Scaffold() {
		super("Scaffold", "scaffold", ModuleCategory.WORLD);
		registerProperties(mode, rotations, polarJump, polarJumpProgressBar, polarStrongRotations, polarFastWalk,
				polarTellyTower, customYaw, customPitch, sideways, sprint, tower, keepY, sneak, sneakDelay, safeWalk,
				rayTrace, moveFix, slotSpoof, swing, jump, dragClick, dragClickCPS, renderCount, thirdPerson,
				watchdogRotationSpeed, watchdog2RotationSpeed, watchdog2Noise, tellyFirstYawSpeed, tellyAirYawSpeed,
				smoothSpeed);
	}

	@Override
	public String getSuffix() {
		return mode.getProperty();
	}

	public boolean shouldSuppressFirstPersonEquipAnimation() {
		return isToggled() && !swing.getProperty();
	}

	@Override
	public void onDisable() {
		if (mc.thePlayer == null)
			return;

		dragClicks = 0;
		tellyForwardSnapped = false;
		tellyHitVec = null;
		tellyVelocityTicks = 0;

		mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());

		mc.thePlayer.inventory.currentItem = itemBefore;
		RotationComponent.setRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, smoothSpeed.getProperty(),
				smoothSpeed.getProperty() / 2, shouldMoveFix());
		mc.timer.timerSpeed = 1f;

		mc.thePlayer.omniSprint = false;

		if (slotSpoof.getProperty())
			mc.thePlayer.inventory.currentItem = spoofSlot;

		if (thirdPerson.getProperty() && mc.gameSettings.thirdPersonView == 1)
			mc.gameSettings.thirdPersonView = 0;

		mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);

		super.onDisable();
	}

	@Override
	public void onEnable() {
		placed = 3;
		sneakPlaced = 0;
		towerGroundTicks = 0;
		towerStage = 0;
		tellyTowering = false;
		tellyForwardSnapped = false;
		tellyHitVec = null;
		tellyVelocityTicks = 0;

		if (mc.thePlayer == null)
			return;

		itemBefore = mc.thePlayer.inventory.currentItem;
		posY = (int) (mc.thePlayer.posY - 1);

		if (slotSpoof.getProperty())
			spoofSlot = mc.thePlayer.inventory.currentItem;

		if (mode.is("Normal"))
			scaffoldYaw = mc.thePlayer.rotationYaw + 180;

		watchdog2TargetYaw = MathHelper.wrapAngleTo180_float((float) MovementUtil.getDirectionKeybinds() - 180.0F);
		watchdog2TargetPitch = 81.5F;
		watchdog2HitVec = null;

		if (mode.is("Retarded"))
			scaffoldYaw = mc.thePlayer.rotationYaw + 180;

		scaffoldPitch = 84f;

		polarTimer.reset();

		if (thirdPerson.getProperty() && mc.gameSettings.thirdPersonView == 0)
			mc.gameSettings.thirdPersonView = 1;

		sprint();
		super.onEnable();
	}

	@Listen
	public void onRenderDimension(RenderDimensionEvent event) {
		if (!hasBlocks())
			return;
		if (blockPos != null) {
			RenderUtil.renderBlock(blockPos, ColorUtil.setAlpha(Hud.getColor(5), 200), true, false);
		}
	}

	@Listen
	public void onSpoofItem(SpoofClientItemEvent event) {
		if (itemBefore != -1) {
			event.setItem(mc.thePlayer.inventory.getStackInSlot(itemBefore));
		}
	}


	@Listen
	public void onPacket(PacketEvent event) {
		if (event.getDirection() != PacketEvent.Direction.RECEIVE || mc.thePlayer == null) {
			return;
		}

		if (event.getPacket() instanceof S12PacketEntityVelocity packet
				&& packet.getEntityID() == mc.thePlayer.getEntityId()
				&& (packet.getMotionX() != 0 || packet.getMotionZ() != 0)) {
			tellyVelocityTicks = 8;
		}
	}

	@Listen
	public void onRotation(RotationEvent event) {
		if (!hasBlocks() || AntiFireball.target != null) {
			return;
		}

		if (tellyVelocityTicks > 0) {
			tellyVelocityTicks--;
		}

		if (dragClickDelayTimer
				.hasTimeReached((long) (1000/MathUtil.random(dragClickCPS.getLeft(), dragClickCPS.getRight())), true)) {
			dragClicks++;
		}

		processBlockData(mc.thePlayer.posX, mc.thePlayer.posZ);
		rotations();

		if (polarStrongRotations.getProperty() && mc.thePlayer.onGround)
			place();
	}

	public boolean isOnSupportedServer() {
		if (IRCUtil.isDevOrHigher())
			return true;
		if (mc.currentServerData == null)
			return false;
		var ip = mc.currentServerData.serverIP;
		for (var s : servers) {
			if (ip.equals(s) || ip.endsWith("." + s)) {
				return true;
			}
		}

		if (mc.thePlayer.ticksExisted % 30 == 0)
			Logger.addChat("Unsupported server!");
		return false;
	}

	@Listen
	public void onClick(MouseClickEvent event) {
		if (rotations.is("Polar") && isOnSupportedServer()) {
			jump.setProperty(false);
			keepY.setProperty(false);
			sneak.setProperty(false);
		}

		if (slotSpoof.getProperty())
			spoofSlot = mc.thePlayer.inventory.currentItem;

		if (!hasBlocks()) {
			RotationComponent.setRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
					smoothSpeed.getProperty(), smoothSpeed.getProperty() / 2, shouldMoveFix());
			return;
		}

		if (AntiFireball.target != null)
			return;

		place();

		if (dragClick.getProperty()) {
			if (blockPos != null) {
				if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null
						&& mc.objectMouseOver.getBlockPos().equalsBlockPos(blockPos)) {
					while (dragClicks > 0) {
						PacketUtil.sendSilent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
						dragClicks--;
					}
				}
			}
		}
	}

	@Listen
	public void onMotion(MotionEvent event) {
		tower();
	}

	@Listen
	public void onShader(ShaderOverlayEvent event) {
		if (renderCount.getProperty()) {
			GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
				if (w.getLabel().equalsIgnoreCase("Block Count")) {
					w.onShader(event);
				}
			});
		}
	}

	@Listen
	public void onRender2D(RenderOverlayEvent event) {
		if (renderCount.getProperty()) {
			GothajClient.getInstance().getWidgetManager().getItems().forEach(w -> {
				if (w.getLabel().equalsIgnoreCase("Block Count")) {
					if (w instanceof BlockCountWidget e) {
						int renderSlot = slotSpoof.getProperty() ? InventoryUtil.getBlockSlot()
								: mc.thePlayer.inventory.currentItem;

						if (renderSlot < 0 || renderSlot >= mc.thePlayer.inventory.mainInventory.length
								|| mc.thePlayer.inventory.mainInventory[renderSlot] == null) {
							renderSlot = InventoryUtil.getBlockSlot();
						}

						e.setSpoofedSlot(renderSlot);
					}
					w.onRender(event.getScaledResolution());
				}
			});
		}
	}

	@Listen
	public void onPostMotion(PostMotionEvent event) {
		if (slotSpoof.getProperty() && spoofSlot != -1)
			mc.thePlayer.inventory.currentItem = spoofSlot;
	}

	@Listen
	public void onJump(JumpEvent event) {
		if (tower.is("Intave")) {
			if (mc.gameSettings.keyBindJump.pressed) {
				event.setJumpMotion(0.41F);
			}
		}
	}

	@Listen
	public void onProgress(ProgressEvent event) {
		if (MovementUtil.isMoving() && !MovementUtil.isGoingDiagonally()) {
			if (!mc.gameSettings.keyBindJump.isPressed()) {
				if (mode.is("Normal")) {
					if (rotations.is("Polar") && isOnSupportedServer()) {
						if (polarJumpProgressBar.getProperty() && polarJump.getProperty())
							event.setProgress(new Progress("Jump", (placed + 1) / 11f));
					}
				}
			}
		}
	}

	@Listen
	public void spoofSneak(SneakSpoofEvent event) {
		if (MovementUtil.isMoving()) {
			if (!mc.gameSettings.keyBindJump.isPressed()) {
				if (mode.is("Normal")) {
					if (rotations.is("Polar") && isOnSupportedServer()) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@Listen
	public void onMoveInput(MoveInputEvent event) {
		sprint();

		if (mode.is("Retarded") && mc.thePlayer.onGround) {
			event.setKeyJump(true);
			return;
		}

		if (shouldTellyAutoJump()) {
			event.setKeyJump(true);
		}
		
		if (MovementUtil.isLookingDiagonally()) {
			placed = rotations.is("Polar") ? 9 : 6;
		}
		if (!mc.thePlayer.onGround && Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
			placed = 0;
		}

		if (rotations.is("Polar") && isOnSupportedServer()) {
			
			if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
				boolean shouldSneak = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
						mc.thePlayer.getEntityBoundingBox()
								.addCoord(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
								.expand(-0.11, 0, -0.11))
						.isEmpty();

				event.setKeySneak(shouldSneak);
				event.setSneak(shouldSneak);
				if (polarFastWalk.getProperty()) {
					event.setSlowdown(mc.thePlayer.onGround ? Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 0.3 : 1 : 0.3);
				}
			} else {
				event.setKeySneak(false);
				event.setSneak(false);
			}
			if (placed >= 10 && polarJump.getProperty()) {
				event.setKeyJump(true);
				placed = 0;
			}
		}
		if (rotations.is("God Bridge")) {
			if (placed >= 7) {
				event.setKeyJump(true);
				placed = 0;
			}
		}

		if (sneak.getProperty() && !mode.is("Telly")) {
			if (mc.thePlayer.onGround) {
				boolean overVoid = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
						mc.thePlayer.getEntityBoundingBox()
								.addCoord(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
								.expand(-0.175, 0, -0.175))
						.isEmpty();

				event.setKeySneak(false);
				event.setSneak(false);
				if (sneakPlaced >= sneakDelay.getProperty()) {
					event.setKeySneak(true);
					event.setSneak(true);
					sneakPlaced = 0;
				}
			}
		}

		if (safeWalk.getProperty() && mc.thePlayer.onGround) {
			if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
					mc.thePlayer.getEntityBoundingBox()
							.addCoord(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
							.expand(-0.175, 0, -0.175))
					.isEmpty()) {
				event.setSneak(true);
				event.setKeySneak(true);
			}
		}

		if (mode.is("Normal") && (rotations.is("God Bridge"))) {

			double movingYaw = Math
					.round((mc.thePlayer.rotationYaw - (MovementUtil.isLookingDiagonally() ? 180 : 135)) / 45f) * 45f;
			movingYaw = MathHelper.wrapAngleTo180_double(movingYaw);

			boolean isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.18) != Math
					.floor(mc.thePlayer.posX)
					|| Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.18) != Math
							.floor(mc.thePlayer.posZ);

			if (mc.gameSettings.keyBindJump.pressed) {
				return;
			}
			if (MovementUtil.isLookingDiagonally()) {
				if (polarTimer.hasTimeReached(250)) {
					event.setKeyLeft(false);
					event.setKeySneak(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT));
				}
			} else {
				if (!isOnRightSide && event.isKeyBack()) {
					event.setKeyLeft(true);
					event.setKeySneak(true);
					polarTimer.reset();
				} else {
					if (polarTimer.hasTimeReached(250)) {
						event.setKeyLeft(false);
						event.setKeySneak(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT));
					}
				}
			}
		}
	}

	private boolean shouldMoveFix() {
		return moveFix.getProperty();
	}

	private boolean shouldTellyAutoJump() {
		return mode.is("Telly") && mc.thePlayer.onGround && MovementUtil.isMoving() && hasBlocks();
	}

	private boolean shouldUpdatePlacementY() {
		boolean jumpKeyHeld = mc.gameSettings.keyBindJump.isKeyDown() || mc.gameSettings.keyBindJump.pressed
				|| shouldTellyAutoJump();
		boolean autoJumpMode = !mode.is("Telly") && (jump.getProperty());
		boolean tellyAscending = mode.is("Telly") && (!mc.thePlayer.onGround || mc.thePlayer.motionY > 0.0D);
		boolean isFalling = !mc.thePlayer.onGround && mc.thePlayer.motionY < 0;
		return !keepY.getProperty() || jumpKeyHeld || autoJumpMode || tellyAscending || isFalling;
	}

	public void tower() {
		if (!mc.thePlayer.onGround) {
			offGroundTicks++;
		} else {
			offGroundTicks = 0;
			tellyForwardSnapped = false;
			tellyHitVec = null;
		}

		towering = false;
		if (GameSettings.isKeyDown(mc.gameSettings.keyBindJump)) {
			if (mc.thePlayer.onGround) {
				requestTower = true;
			}
		} else
			requestTower = false;

		if (requestTower && !tower.is("None")) {
			towering = true;

			switch (tower.getProperty().toLowerCase()) {
			case "ncp":
				if (mc.thePlayer.posY % 1 <= 0.00153598 && mc.gameSettings.keyBindJump.pressed) {
					mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
					mc.thePlayer.motionY = 0.41998;
				} else if (mc.thePlayer.posY % 1 < 0.1
						&& (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.pressed)) {
					mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
				}
				break;
			case "Polar":

				break;
			case "timer":
				if (mc.gameSettings.keyBindJump.pressed) {
					mc.timer.timerSpeed = 1.25f;
				}
				break;
			case "watchdog":
				if (mc.gameSettings.keyBindJump.isKeyDown() && !MovementUtil.isMoving()) {
					if (mc.thePlayer.onGround) {
						towerGroundTicks++;
						towerStage++;
						tellyTowering = false;
					} else {
						towerGroundTicks = 0;
					}

					if (towerStage >= 2 && !mc.thePlayer.onGround && mc.thePlayer.motionY <= 0.0
							&& mc.thePlayer.motionY >= -0.09) {
						mc.thePlayer.motionY = -0.38;
					}
				} else {
					towerStage = 0;
					towerGroundTicks = 0;
					tellyTowering = false;
				}
				break;
			}
		}
	}

	public void sprint() {
		switch (sprint.getProperty()) {
		case "Legit":
			if (mode.is("Telly")) {
				boolean snapForward = shouldTellySnapForwardRotation();
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), snapForward);
				mc.thePlayer.omniSprint = false;
				break;
			}

			if (MovementUtil.isMoving() && mc.gameSettings.keyBindForward.pressed
					&& Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)
							- MathHelper.wrapAngleTo180_float(RotationComponent.rotations[0])) < 66.5) {
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
				mc.thePlayer.omniSprint = true;
			} else {
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
				mc.thePlayer.omniSprint = false;
			}
			break;
		case "Off":
			KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
			mc.thePlayer.omniSprint = false;
			break;
		case "Always":
			if (mode.is("Telly")) {
				boolean snapForward = shouldTellySnapForwardRotation();
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), snapForward);
				mc.thePlayer.omniSprint = false;
				break;
			}

			if (MovementUtil.isMoving()) {
				KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
			} else {
				mc.thePlayer.omniSprint = false;
			}
			break;
		}
	}

	public void rotations() {
		boolean instantTurn = false;
		watchdog2HitVec = null;
		boolean isWatchdogTower = tower.is("Watchdog") && mc.gameSettings.keyBindJump.isKeyDown()
				&& !MovementUtil.isMoving();
		if (mode.is("Retarded")) {
			if (blockPos != null && facing != null) {
				scaffoldPitch = RotationUtil.getYawBasedPitch(blockPos, facing, scaffoldYaw, scaffoldPitch, 84);

				if (!RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, true)) {
					if (!RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, false)) {
						scaffoldYaw = RotationUtil.getDirectionToBlock(blockPos.getX(), blockPos.getY(),
								blockPos.getZ(), facing)[0];
					}
				}
			}
			if (mc.thePlayer.onGround)
				scaffoldYaw = (float) MovementUtil.getDirectionKeybinds();

			RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, 180, 180, shouldMoveFix());
			return;
		}
		if (mode.is("Normal")) {
			if (MovementUtil.isMoving() && mc.thePlayer.hurtTime == 0) {
				scaffoldYaw = (float) Math.toDegrees(MovementUtil.getDirectionKeybinds(mc.thePlayer.rotationYaw
						- ((sideways.getProperty() && !MovementUtil.isLookingDiagonally()) ? 135 : 180)));
				scaffoldPitch = 76f;
			} else {
				scaffoldYaw = RotationComponent.rotations[0];
			}

			if (isWatchdogTower) {
				scaffoldPitch = 90f;
			}

			switch (rotations.getProperty()) {
			case "Polar":
				if (!isOnSupportedServer())
					return;

				if (polarTellyTower.getProperty() && mc.gameSettings.keyBindJump.pressed) {
					if (blockPos != null && facing != null) {
						scaffoldPitch = RotationUtil.getYawBasedPitch(blockPos, facing, scaffoldYaw, scaffoldPitch, 84);

						if (!RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, true)) {
							if (!RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, false)) {
								scaffoldYaw = RotationUtil.getDirectionToBlock(blockPos.getX(), blockPos.getY(),
										blockPos.getZ(), facing)[0];
							}
						}
					}
					if (mc.thePlayer.onGround)
						scaffoldYaw = (float) MovementUtil.getDirectionKeybinds();

					instantTurn = true;
				} else {

					scaffoldPitch = MovementUtil.isLookingDiagonally() ? 79f : 82f;
					if (!MovementUtil.isMoving())
						scaffoldPitch = 90f;

					float pitchLimit = MovementUtil.isLookingDiagonally() ? 85f : 79.999f;
					if (!MovementUtil.isMoving())
						pitchLimit = 90f;

					if (blockPos != null && facing != null) {
						if ((mc.thePlayer.fallDistance > 0 || mc.gameSettings.keyBindJump.pressed
								|| mc.thePlayer.hurtTime > 0)
								&& !RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, false)) {
							float targetYaw = RotationUtil.getDirectionToBlock(blockPos.getX(), blockPos.getY(),
									blockPos.getZ(), facing)[0];

							int ticks = 0;
							while (ticks <= 100 && !instantTurn) {
								scaffoldYaw = RotationUtil.updateRotation(scaffoldYaw, targetYaw,
										(float) (1.8f + Math.random() / 10));

								scaffoldPitch = RotationUtil.getYawBasedPitch(blockPos, facing, scaffoldYaw,
										scaffoldPitch, pitchLimit);

								if (RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, true)) {
									instantTurn = true;
								}
								ticks++;
							}
						}
					}
				}
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, !instantTurn ? 30 : 180,
						!instantTurn ? 30 : 180, shouldMoveFix());
				break;
			case "Watchdog":
			case "Intave":
				instantTurn = applyDefaultBruteforceRotations(isWatchdogTower);
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch,
						!instantTurn ? watchdogRotationSpeed.getProperty() : 180,
						!instantTurn ? watchdogRotationSpeed.getProperty() / 2 : 180, shouldMoveFix());
				break;
			case "Watchdog 2":
				applyWatchdog2Rotations();
				float watchdog2RotationSpeed = getWatchdog2RotationSpeed();
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, watchdog2RotationSpeed,
						watchdog2RotationSpeed, shouldMoveFix());
				break;

			case "Custom":
				scaffoldYaw = mc.thePlayer.rotationYaw + customYaw.getProperty();
				scaffoldPitch = customPitch.getProperty();
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, smoothSpeed.getProperty(),
						smoothSpeed.getProperty(), shouldMoveFix());
				break;

			case "God Bridge":
				float baseYaw = mc.thePlayer.rotationYaw - (MovementUtil.isLookingDiagonally() ? 180 : 135);
				scaffoldYaw = Math.round(baseYaw / 45f) * 45f;
				scaffoldPitch = 75.6f;

				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, smoothSpeed.getProperty(),
						smoothSpeed.getProperty(), shouldMoveFix());
				break;
			}
		} else if (mode.is("Telly")) {
			if (shouldTellySnapForwardRotation()) {
				scaffoldYaw = MathHelper.wrapAngleTo180_float((float) MovementUtil.getDirectionKeybinds());
				if (rayTrace.getProperty()) {
					scaffoldPitch = 85.0F;
				}
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, 180, 180, true);
				tellyForwardSnapped = true;
			} else if (rotations.is("Watchdog 2")) {
				applyWatchdog2Rotations();
				final float watchdog2RotationSpeed = getWatchdog2RotationSpeed();
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, watchdog2RotationSpeed,
						watchdog2RotationSpeed, true);
			} else {
				applyTellyAirRotations();
				final float tellyYawSpeed = offGroundTicks <= 1 ? tellyFirstYawSpeed.getProperty()
						: tellyAirYawSpeed.getProperty();
				RotationComponent.setRotations(scaffoldYaw, scaffoldPitch, tellyYawSpeed, 180, true);
			}
		}
	}

	private boolean shouldTellySnapForwardRotation() {
		if (isNoRaycastTellyKnockback()) {
			return false;
		}

		float forward = mc.thePlayer.movementInput.realMoveForward;
		float strafe = mc.thePlayer.movementInput.realMoveStrafe;
		return forward * forward + strafe * strafe >= 1.0E-4F
				&& (mc.thePlayer.onGround || (!tellyForwardSnapped && offGroundTicks <= 1));
	}

	private void applyTellyAirRotations() {
		float yaw180 = MathHelper.wrapAngleTo180_float((float) MovementUtil.getDirectionKeybinds() + 180.0F);
		float targetYaw = isNoRaycastTellyKnockback() ? getBackwardsYaw() : yaw180;
		scaffoldYaw = targetYaw;
		tellyHitVec = null;

		if (blockPos != null && facing != null) {
			if (rayTrace.getProperty()) {
				applyTellyRaycastHitYawRotations(targetYaw);
			} else {
				findTellyNoRaycastHitVec(targetYaw);
				if (isNoRaycastTellyKnockback() && tellyHitVec != null
						&& Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - scaffoldYaw)) < 90.0F) {
					scaffoldYaw = targetYaw;
				}
				if (tellyHitVec == null) {
					scaffoldPitch = RotationUtil.getYawBasedPitch(blockPos, facing, targetYaw, scaffoldPitch, 84);
				}
			}
		}
	}

	private boolean isNoRaycastTellyKnockback() {
		return mode.is("Telly") && !rayTrace.getProperty() && (mc.thePlayer.hurtTime > 0 || tellyVelocityTicks > 0);
	}

	private float getBackwardsYaw() {
		float currentYaw = isNoRaycastTellyKnockback() ? getMotionYaw() : getMovementYaw();
		float eventYaw = RotationComponent.rotations != null ? RotationComponent.rotations[0] : mc.thePlayer.rotationYaw;
		return quantizeBackwardsAngle(wrapBackwardsAngleDiff(currentYaw - 180.0F, eventYaw));
	}

	private float getMotionYaw() {
		if (mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ < 1.0E-4D) {
			return getMovementYaw();
		}

		return MathHelper.wrapAngleTo180_float(
				(float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
	}

	private float getMovementYaw() {
		float yaw = mc.thePlayer.rotationYaw;
		float forward = 0.0F;
		float strafe = 0.0F;

		if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
			forward += 1.0F;
		}
		if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
			forward -= 1.0F;
		}
		if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
			strafe += 1.0F;
		}
		if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
			strafe -= 1.0F;
		}

		if (forward < 0.0F) {
			yaw += 180.0F;
		}
		if (strafe != 0.0F) {
			float multiplier = forward == 0.0F ? 1.0F : 0.5F * Math.signum(forward);
			yaw += -90.0F * multiplier * Math.signum(strafe);
		}

		return MathHelper.wrapAngleTo180_float(yaw);
	}

	private float wrapBackwardsAngleDiff(float angle, float target) {
		return target + MathHelper.wrapAngleTo180_float(angle - target);
	}

	private float quantizeBackwardsAngle(float angle) {
		return (float) ((double) angle - (double) angle % (double) 0.0096F);
	}

	private void applyTellyRaycastHitYawRotations(float fallbackYaw) {
		float[] rot = getTellyDirectionToBlock(blockPos, facing);
		scaffoldPitch = rot[1];

		float currentYaw = RotationComponent.rotations != null ? RotationComponent.rotations[0] : fallbackYaw;
		if (isTellyRaycastHit(currentYaw, scaffoldPitch) && !isTellyNearBlockEdge()) {
			scaffoldYaw = currentYaw;
		} else {
			scaffoldYaw = rot[0];
		}
	}

	private float[] getTellyDirectionToBlock(BlockPos blockPos, EnumFacing facing) {
		Vec3i direction = facing.getDirectionVec();
		double targetX = blockPos.getX() + 0.5D + direction.getX() * 0.25D;
		double targetY = blockPos.getY() + 0.5D + direction.getY() * 0.25D;
		double targetZ = blockPos.getZ() + 0.5D + direction.getZ() * 0.25D;
		double deltaX = targetX - mc.thePlayer.posX;
		double deltaY = targetY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
		double deltaZ = targetZ - mc.thePlayer.posZ;
		double horizontalDist = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
		float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
		float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
		return new float[] { yaw, pitch };
	}

	private boolean isTellyRaycastHit(float yaw, float pitch) {
		MovingObjectPosition mop = mc.thePlayer.rayTraceCustom(mc.playerController.getBlockReachDistance(), yaw,
				pitch);
		return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& mop.getBlockPos().equals(blockPos) && !isPosSolid(blockPos.offset(facing));
	}

	private boolean isTellyNearBlockEdge() {
		double fracX = mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
		double fracZ = mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
		return fracX < 0.22D || fracX > 0.78D || fracZ < 0.22D || fracZ > 0.78D;
	}

	private void findTellyNoRaycastHitVec(float baseYaw) {
		double[] xs = CLUTCH_PLACE_OFFSETS;
		double[] ys = CLUTCH_PLACE_OFFSETS;
		double[] zs = CLUTCH_PLACE_OFFSETS;

		switch (facing) {
		case NORTH:
			zs = new double[] { 0.0 };
			break;
		case EAST:
			xs = new double[] { 1.0 };
			break;
		case SOUTH:
			zs = new double[] { 1.0 };
			break;
		case WEST:
			xs = new double[] { 0.0 };
			break;
		case DOWN:
			ys = new double[] { 0.0 };
			break;
		case UP:
			ys = new double[] { 1.0 };
			break;
		}

		float bestYaw = -180.0F;
		float bestPitch = 0.0F;
		float bestDiff = 0.0F;
		Vec3 bestHitVec = null;

		for (double dx : xs) {
			for (double dy : ys) {
				for (double dz : zs) {
					double relX = blockPos.getX() + dx - mc.thePlayer.posX;
					double relY = blockPos.getY() + dy - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
					double relZ = blockPos.getZ() + dz - mc.thePlayer.posZ;
					float[] rot = getRotationsTo(relX, relY, relZ);
					MovingObjectPosition mop = mc.thePlayer.rayTraceCustom(mc.playerController.getBlockReachDistance(),
							rot[0], rot[1]);
					if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
							|| !mop.getBlockPos().equals(blockPos) || mop.sideHit != facing) {
						continue;
					}

					float totalDiff = Math.abs(MathHelper.wrapAngleTo180_float(rot[0] - baseYaw))
							+ Math.abs(rot[1] - scaffoldPitch);
					if (bestHitVec == null || totalDiff < bestDiff) {
						bestYaw = rot[0];
						bestPitch = rot[1];
						bestDiff = totalDiff;
						bestHitVec = mop.hitVec;
					}
				}
			}
		}

		if (bestHitVec != null) {
			scaffoldYaw = bestYaw;
			scaffoldPitch = bestPitch;
			tellyHitVec = bestHitVec;
		}
	}

	private float[] getRotationsTo(double relX, double relY, double relZ) {
		double dist = MathHelper.sqrt_double(relX * relX + relZ * relZ);
		float yaw = (float) Math.toDegrees(Math.atan2(relZ, relX)) - 90.0F;
		float pitch = (float) -Math.toDegrees(Math.atan2(relY, dist));
		return new float[] { MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F) };
	}

	private boolean applyDefaultBruteforceRotations(boolean pitchLocked) {
		boolean instantTurn = false;
		float yaw180 = MathHelper.wrapAngleTo180_float((float) MovementUtil.getDirectionKeybinds() + 180.0F);

		if (blockPos != null && facing != null) {
			if (!pitchLocked) {
				scaffoldPitch = RotationUtil.getYawBasedPitch(blockPos, facing, yaw180, scaffoldPitch, 84);
			}

			scaffoldYaw = yaw180;

			if (!RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, true)) {
				float targetYaw = RotationUtil.getDirectionToBlock(blockPos.getX(), blockPos.getY(),
						blockPos.getZ(), facing)[0];

				int ticks = 0;
				while (ticks <= 100 && !instantTurn) {
					scaffoldYaw = RotationUtil.updateRotation(scaffoldYaw, targetYaw,
							(float) (1.8f + Math.random() / 10));
					if (!pitchLocked)
						scaffoldPitch = RotationUtil.getYawBasedPitch(blockPos, facing, scaffoldYaw,
								scaffoldPitch, 84);

					if (RotationUtil.lookingAtBlock(blockPos, scaffoldYaw, scaffoldPitch, facing, true)) {
						instantTurn = true;
					}
					ticks++;
				}
			}
		} else {
			scaffoldYaw = yaw180;
		}

		return instantTurn;
	}

	private void applyWatchdog2Rotations() {
		final float fallbackYaw = MathHelper.wrapAngleTo180_float((float) MovementUtil.getDirectionKeybinds() - 180.0F);

		if (blockPos != null && facing != null) {
			if (!RotationUtil.lookingAtBlock(blockPos, watchdog2TargetYaw, watchdog2TargetPitch, facing, true)) {
				watchdog2TargetPitch = RotationUtil.getYawBasedPitch(blockPos, facing, watchdog2TargetYaw,
						watchdog2TargetPitch, 90);

				if (!MovementUtil.isGoingDiagonally() || offGroundTicks < 5) {
					if (!RotationUtil.lookingAtBlock(blockPos, watchdog2TargetYaw, watchdog2TargetPitch, facing, true)) {
						watchdog2TargetYaw = MathHelper.wrapAngleTo180_float((float) MovementUtil.getDirectionKeybinds()
								- 180.0F);

						if (!RotationUtil.lookingAtBlock(blockPos, watchdog2TargetYaw, watchdog2TargetPitch, facing, false)) {
							watchdog2TargetPitch = RotationUtil.getYawBasedPitch(blockPos, facing, watchdog2TargetYaw,
									watchdog2TargetPitch, 90);
						}
					}
				}
			}

			final Watchdog2Aim aim = resolveWatchdog2Aim(watchdog2TargetYaw, watchdog2TargetPitch);
			if (aim != null) {
				watchdog2TargetYaw = aim.yaw;
				watchdog2TargetPitch = aim.pitch;
				watchdog2HitVec = aim.hitVec;
			}
		} else {
			watchdog2TargetYaw = fallbackYaw;
		}

		scaffoldYaw = watchdog2TargetYaw;
		scaffoldPitch = watchdog2TargetPitch;
	}

	private float getWatchdog2RotationSpeed() {
		if (watchdog2Noise.getProperty()) {
			if (watchdog2NoiseUpdateTimer <= 0) {
				watchdog2SpeedNoise = -2.5F + (float) (Math.random() * 2.5F);
				watchdog2NoiseUpdateTimer = 15 + (int) (Math.random() * 4);
			} else {
				watchdog2NoiseUpdateTimer--;
			}
		} else {
			watchdog2SpeedNoise = 0;
		}

		return (mc.thePlayer.onGround ? 180 : offGroundTicks < 2
				? watchdog2RotationSpeed.getLeft()
				+ (float) (Math.random() * (watchdog2RotationSpeed.getRight() - watchdog2RotationSpeed.getLeft()))
				: 40) + watchdog2SpeedNoise;
	}

	private Watchdog2Aim resolveWatchdog2Aim(float baseYaw, float basePitch) {
		final MovingObjectPosition baseHit = mc.thePlayer.rayTraceCustom(mc.playerController.getBlockReachDistance(),
				baseYaw, basePitch);
		if (isWatchdog2FaceHit(baseHit)) {
			return new Watchdog2Aim(baseYaw, basePitch, baseHit.hitVec);
		}

		double[] xs = CLUTCH_PLACE_OFFSETS;
		double[] ys = CLUTCH_PLACE_OFFSETS;
		double[] zs = CLUTCH_PLACE_OFFSETS;

		switch (facing) {
		case NORTH:
			zs = new double[] { 0.0 };
			break;
		case EAST:
			xs = new double[] { 1.0 };
			break;
		case SOUTH:
			zs = new double[] { 1.0 };
			break;
		case WEST:
			xs = new double[] { 0.0 };
			break;
		case DOWN:
			ys = new double[] { 0.0 };
			break;
		case UP:
			ys = new double[] { 1.0 };
			break;
		}

		Watchdog2Aim best = null;
		float bestDiff = Float.POSITIVE_INFINITY;

		for (double x : xs) {
			for (double y : ys) {
				for (double z : zs) {
					final double relativeX = blockPos.getX() + x - mc.thePlayer.posX;
					final double relativeY = blockPos.getY() + y - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
					final double relativeZ = blockPos.getZ() + z - mc.thePlayer.posZ;
					final float[] candidate = getRotationsTo(relativeX, relativeY, relativeZ);
					final MovingObjectPosition hit = mc.thePlayer.rayTraceCustom(
							mc.playerController.getBlockReachDistance(), candidate[0], candidate[1]);

					if (!isWatchdog2FaceHit(hit))
						continue;

					final float difference = Math.abs(MathHelper.wrapAngleTo180_float(candidate[0] - baseYaw))
							+ Math.abs(candidate[1] - basePitch);
					if (difference < bestDiff) {
						bestDiff = difference;
						best = new Watchdog2Aim(candidate[0], candidate[1], hit.hitVec);
					}
				}
			}
		}

		return best;
	}

	private boolean isWatchdog2FaceHit(MovingObjectPosition hit) {
		return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
				&& hit.getBlockPos() != null && hit.getBlockPos().equalsBlockPos(blockPos) && hit.sideHit == facing;
	}

	public boolean place() {
		if (blockPos == null || facing == null) {
			return false;
		}

		boolean isClutching = !mc.thePlayer.onGround && mc.thePlayer.motionY < -0.1;

		if ((rotations.is("God Bridge")) && mode.is("Normal") && !mc.thePlayer.isJumping
				&& mc.gameSettings.keyBindSneak.pressed && !isClutching) {
			return false;
		}

		mc.thePlayer.inventory.currentItem = InventoryUtil.getBlockSlot();

		if (rotations.is("Watchdog 2") && watchdog2HitVec != null) {
			final boolean clicked = mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
					mc.thePlayer.getHeldItem(), blockPos, facing, watchdog2HitVec);
			if (!clicked)
				return false;

			placed++;
			sneakPlaced++;

			if (swing.getProperty()) {
				mc.thePlayer.swingItem();
			} else {
				mc.getNetHandler().getNetworkManager().sendPacketSilentEvent(new C0APacketAnimation());
				mc.getItemRenderer().forceEquippedProgress();
			}

			Logger.debugChat(this, "SUCCESS: Placed at " + blockPos + " facing " + facing);
			return true;
		}

		if (mode.is("Telly") && !rayTrace.getProperty()) {
			return placeTellyNoRaycast();
		}

		if (mc.objectMouseOver == null) {
			return false;
		}

		if (mc.objectMouseOver.getBlockPos() == null || !mc.objectMouseOver.getBlockPos().equalsBlockPos(blockPos)) {
			return false;
		}

		if (mc.objectMouseOver.sideHit == EnumFacing.UP && facing != EnumFacing.UP) {
			return false;
		}

		boolean clicked = mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(),
				mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec);
		if (!clicked)
			return false;

		placed++;

		sneakPlaced++;

		if (swing.getProperty()) {
			mc.thePlayer.swingItem();
		} else {
			mc.getNetHandler().getNetworkManager().sendPacketSilentEvent(new C0APacketAnimation());
			mc.getItemRenderer().forceEquippedProgress();
		}

		Logger.debugChat(this, "SUCCESS: Placed at "
				+ (mc.objectMouseOver != null && rayTrace.getProperty() ? mc.objectMouseOver.getBlockPos() : blockPos)
				+ " facing " + facing);
		return true;
	}

	private boolean placeTellyNoRaycast() {
		if (tellyHitVec == null) {
			return false;
		}

		if (!isTellyNoRaycastRotationReady()) {
			return false;
		}

		boolean clicked = mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(),
				blockPos, facing, tellyHitVec);
		if (!clicked)
			return false;

		placed++;
		sneakPlaced++;

		if (swing.getProperty()) {
			mc.thePlayer.swingItem();
		} else {
			mc.getNetHandler().getNetworkManager().sendPacketSilentEvent(new C0APacketAnimation());
			mc.getItemRenderer().forceEquippedProgress();
		}

		Logger.debugChat(this, "SUCCESS: Placed at " + blockPos + " facing " + facing);
		return true;
	}

	private boolean isTellyNoRaycastRotationReady() {
		return RotationComponent.rotations != null
				&& Math.abs(MathHelper.wrapAngleTo180_float(scaffoldYaw - RotationComponent.rotations[0])) <= 1.0F;
	}

	public void processBlockData(double posX, double posZ) {
		boolean shouldUpdateY;

		if (mode.is("Telly")) {
			shouldUpdateY = shouldUpdatePlacementY();
		} else {
			if (keepY.getProperty() || towering) {
				shouldUpdateY = mc.gameSettings.keyBindJump.pressed;
			} else {
				shouldUpdateY = mc.thePlayer.onGround || mc.thePlayer.motionY <= 0;
			}
		}

		if (shouldUpdateY) {
			posY = (int) (mc.thePlayer.posY - 1);
		}

		if (isNoRaycastTellyKnockback()) {
			ClutchBlockData clutchData = getClutchBlockData();
			if (clutchData != null) {
				blockPos = clutchData.blockPos;
				facing = clutchData.facing;
			} else {
				blockPos = null;
				facing = null;
			}
			return;
		}

		int currentY = posY;

		if (!mode.is("Telly") && !mc.gameSettings.keyBindJump.pressed && !mc.thePlayer.onGround) {
			BlockPos abovePos = new BlockPos(mc.thePlayer.posX, posY + 1, mc.thePlayer.posZ);
			if (isValidBock(abovePos)) {
				currentY = abovePos.getY();
			}
		}

		blockPos = getBlockPos(posX, currentY, posZ);

		if (blockPos != null) {
			facing = getPlaceSide(posX, currentY, posZ);
		} else {
			facing = null;
		}
	}

	private BlockPos getBlockPos(double posX, double posY, double posZ) {
		BlockPos directBelow = new BlockPos(posX, posY, posZ);
		if (isValidBock(directBelow))
			return directBelow;

		BlockPos playerPos = new BlockPos(posX, posY, posZ);
		ArrayList<Vec3> positions = new ArrayList<>();
		HashMap<Vec3, BlockPos> hashMap = new HashMap<>();

		for (double y = playerPos.getY() - (!mc.thePlayer.onGround ? 1 : 0); y <= playerPos.getY(); ++y) {
			for (double x = playerPos.getX() - 7.0D; x <= playerPos.getX() + 7.0D; ++x) {
				for (double z = playerPos.getZ() - 76.0D; z <= playerPos.getZ() + 7.0D; ++z) {
					BlockPos candidate = new BlockPos(x, y, z);
					if (isValidBock(candidate)) {
						Block block = mc.theWorld.getBlockState(candidate).getBlock();

						double ex = MathHelper.clamp(posX, candidate.getX() + 0.01,
								candidate.getX() + block.getBlockBoundsMaxX() - 0.01);
						double ey = MathHelper.clamp(posY, candidate.getY() + 0.01,
								candidate.getY() + block.getBlockBoundsMaxY() - 0.01);
						double ez = MathHelper.clamp(posZ, candidate.getZ() + 0.01,
								candidate.getZ() + block.getBlockBoundsMaxZ() - 0.01);

						Vec3 vec3 = new Vec3(ex, ey, ez);
						positions.add(vec3);
						hashMap.put(vec3, candidate);
					}
				}
			}
		}

		if (positions.isEmpty())
			return null;

		double sortY = keepY.getProperty() && !mc.gameSettings.keyBindJump.pressed ? posY + 1 : mc.thePlayer.posY;

		positions.sort(Comparator.<Vec3>comparingDouble(pos -> {
			double dx = pos.xCoord - posX;
			double dz = pos.zCoord - posZ;
			return dx * dx + dz * dz;
		}).thenComparingDouble(pos -> Math.abs(pos.yCoord - sortY)));

		return hashMap.get(positions.get(0));
	}

	private ClutchBlockData getClutchBlockData() {
		int startY = MathHelper.floor_double(mc.thePlayer.posY);
		BlockPos targetPos = new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), startY - 1,
				MathHelper.floor_double(mc.thePlayer.posZ));

		if (!isReplaceable(targetPos)) {
			return null;
		}

		ArrayList<BlockPos> positions = new ArrayList<>();
		for (int x = -4; x <= 4; x++) {
			for (int y = -4; y <= 0; y++) {
				for (int z = -4; z <= 4; z++) {
					BlockPos pos = targetPos.add(x, y, z);
					if (isReplaceable(pos) || isInteractable(pos)
							|| mc.thePlayer.getDistance(pos.getX() + 0.5D, pos.getY() + 0.5D,
									pos.getZ() + 0.5D) > mc.playerController.getBlockReachDistance()) {
						continue;
					}

					for (EnumFacing side : EnumFacing.VALUES) {
						if (side != EnumFacing.DOWN && isReplaceable(pos.offset(side))) {
							positions.add(pos);
							break;
						}
					}
				}
			}
		}

		if (positions.isEmpty()) {
			return null;
		}

		positions.sort(Comparator.comparingDouble(pos -> pos.distanceSqToCenter(targetPos.getX() + 0.5D,
				targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D)));

		BlockPos blockPos = positions.get(0);
		EnumFacing bestFacing = getClutchBestFacing(blockPos, targetPos);
		return bestFacing == null ? null : new ClutchBlockData(blockPos, bestFacing);
	}

	private EnumFacing getClutchBestFacing(BlockPos blockPos, BlockPos targetPos) {
		double bestDistance = 0.0D;
		EnumFacing bestFacing = null;

		for (EnumFacing side : EnumFacing.VALUES) {
			if (side == EnumFacing.DOWN) {
				continue;
			}

			BlockPos placePos = blockPos.offset(side);
			if (placePos.getY() > targetPos.getY()) {
				continue;
			}

			double distance = placePos.distanceSqToCenter(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D,
					targetPos.getZ() + 0.5D);
			if (bestFacing == null || distance < bestDistance || distance == bestDistance && side == EnumFacing.UP) {
				bestDistance = distance;
				bestFacing = side;
			}
		}

		return bestFacing;
	}

	private EnumFacing getPlaceSide(double posX, double posY, double posZ) {
		ArrayList<Vec3> positions = new ArrayList<>();
		HashMap<Vec3, EnumFacing> hashMap = new HashMap<>();

		BlockPos targetPos = new BlockPos(posX, posY + 1, posZ);

		if (!keepY.getProperty() || ((!mode.is("Telly") && (jump.getProperty())) || mc.gameSettings.keyBindJump.pressed)
				|| towering) {
			if (!isPosSolid(blockPos.add(0, 1, 0)) && !blockPos.add(0, 1, 0).equals(targetPos)
					&& !mc.thePlayer.onGround) {
				BlockPos bp = blockPos.add(0, 1, 0);
				Vec3 vec4 = this.getBestHitFeet(bp, posX, posZ);
				positions.add(vec4);
				hashMap.put(vec4, EnumFacing.UP);
			}
		}

		if (!isPosSolid(blockPos.add(1, 0, 0)) && !blockPos.add(1, 0, 0).equals(targetPos)) {
			BlockPos bp = blockPos.add(1, 0, 0);
			Vec3 vec4 = this.getBestHitFeet(bp, posX, posZ);
			positions.add(vec4);
			hashMap.put(vec4, EnumFacing.EAST);
		}
		if (!isPosSolid(blockPos.add(-1, 0, 0)) && !blockPos.add(-1, 0, 0).equals(targetPos)) {
			BlockPos bp = blockPos.add(-1, 0, 0);
			Vec3 vec4 = this.getBestHitFeet(bp, posX, posZ);
			positions.add(vec4);
			hashMap.put(vec4, EnumFacing.WEST);
		}
		if (!isPosSolid(blockPos.add(0, 0, 1)) && !blockPos.add(0, 0, 1).equals(targetPos)) {
			BlockPos bp = blockPos.add(0, 0, 1);
			Vec3 vec4 = this.getBestHitFeet(bp, posX, posZ);
			positions.add(vec4);
			hashMap.put(vec4, EnumFacing.SOUTH);
		}
		if (!isPosSolid(blockPos.add(0, 0, -1)) && !blockPos.add(0, 0, -1).equals(targetPos)) {
			BlockPos bp = blockPos.add(0, 0, -1);
			Vec3 vec4 = this.getBestHitFeet(bp, posX, posZ);
			positions.add(vec4);
			hashMap.put(vec4, EnumFacing.NORTH);
		}

		if (positions.isEmpty())
			return null;

		double sortY = keepY.getProperty() && !mc.gameSettings.keyBindJump.pressed ? posY + 1 : mc.thePlayer.posY;

		positions.sort(Comparator.comparingDouble(vec3 -> {
			double dx = vec3.xCoord - posX;
			double dz = vec3.zCoord - posZ;
			return (dx * dx + dz * dz) + Math.abs(vec3.yCoord - sortY) * 0.1;
		}));

		return hashMap.get(positions.get(0));
	}

	private Vec3 getBestHitFeet(final BlockPos blockPos, double posX, double posZ) {
		final Block block = mc.theWorld.getBlockState(blockPos).getBlock();

		final double ex = MathHelper.clamp(posX, blockPos.getX() + 0.01,
				blockPos.getX() + block.getBlockBoundsMaxX() - 0.01);
		final double ey = MathHelper.clamp(mc.thePlayer.posY, blockPos.getY() + 0.01,
				blockPos.getY() + block.getBlockBoundsMaxY() - 0.01);
		final double ez = MathHelper.clamp(posZ, blockPos.getZ() + 0.01,
				blockPos.getZ() + block.getBlockBoundsMaxZ() - 0.01);
		return new Vec3(ex, ey, ez);
	}

	public static boolean isPosSolid(BlockPos pos) {
		Block block = Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock();
		return (block.getMaterial().isSolid() || !block.isTranslucent() || !block.isReplaceable(mc.theWorld, pos)
				|| block instanceof BlockLadder || block instanceof BlockCarpet || block instanceof BlockSnow
				|| block instanceof BlockSkull) && !block.getMaterial().isLiquid()
				&& !(block instanceof BlockContainer);
	}

	private boolean isReplaceable(BlockPos pos) {
		Block block = mc.theWorld.getBlockState(pos).getBlock();
		return block.isReplaceable(mc.theWorld, pos) || block instanceof BlockAir || block instanceof BlockLiquid;
	}

	private boolean isInteractable(BlockPos pos) {
		return mc.theWorld.getBlockState(pos).getBlock() instanceof BlockContainer;
	}

	public static boolean isValidBock(BlockPos blockPos) {
		Block block = Minecraft.getMinecraft().theWorld.getBlockState(blockPos).getBlock();
		return !block.isReplaceable(mc.theWorld, blockPos) && !(block instanceof BlockLiquid)
				&& !(block instanceof BlockAir) && !(block instanceof BlockChest) && !(block instanceof BlockFurnace);
	}

	public boolean hasBlocks() {
		if (mc.thePlayer == null)
			return false;
		int item = InventoryUtil.getBlockSlot();
		if (item == -1) {
			RotationComponent.setRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch,
					smoothSpeed.getProperty(), smoothSpeed.getProperty() / 2, shouldMoveFix());
			return false;
		}
		return true;
	}

	private static class Watchdog2Aim {
		private final float yaw;
		private final float pitch;
		private final Vec3 hitVec;

		private Watchdog2Aim(float yaw, float pitch, Vec3 hitVec) {
			this.yaw = yaw;
			this.pitch = pitch;
			this.hitVec = hitVec;
		}
	}

	private static class ClutchBlockData {
		private final BlockPos blockPos;
		private final EnumFacing facing;

		private ClutchBlockData(BlockPos blockPos, EnumFacing facing) {
			this.blockPos = blockPos;
			this.facing = facing;
		}
	}
}
