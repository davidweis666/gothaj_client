package team.gothaj.client.impl.module.movement;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.block.material.Material;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3i;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.impl.event.game.TimeDelayEvent;
import team.gothaj.client.impl.event.game.UnchangedTimerEvent;
import team.gothaj.client.impl.event.input.InputEvent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.input.PlaceEvent;
import team.gothaj.client.impl.event.player.RotationEvent;
import team.gothaj.client.impl.event.player.SlowDownEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author Kawase
 * @since 28.09.2025
 */
public class ScaffoldRecorder extends Module {

	public ArrayList<Rotation> rotations = new ArrayList<Rotation>();
	public ArrayList<Movement> movements = new ArrayList<Movement>();
	public ArrayList<Placement> clicks = new ArrayList<Placement>();

	int rotationTicks, movementTicks, clickTicks;

	public BooleanProperty record = new BooleanProperty("Record", false);
	public NumberProperty timer = new NumberProperty("Timer", 0.1f, 0.01f, 1f, 0.01f);

	public ScaffoldRecorder() {
		super("ScaffoldRecorder", "Records how you bridge and does that", ModuleCategory.MOVEMENT);
		this.registerProperties(record, timer);
	}

	@Override
	public void onEnable() {
		if (record.getProperty()) {
			rotations.clear();
			clicks.clear();
			movements.clear();
		}
		rotationTicks = 0;
		clickTicks = 0;
		movementTicks = 0;

		super.onEnable();

	}

	@Override
	public void onDisable() {
		mc.timer.timerSpeed = 1f;

		super.onDisable();
	}

	@Listen
	public void onMouseClick(MouseClickEvent event) {
		mc.timer.timerSpeed = timer.getProperty();

		if (record.getProperty()) {
			rotations.add(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch));
		} else {
			mc.timer.timerSpeed = 1.0f;
			if (rotationTicks >= rotations.size() - 1) {
				rotationTicks = 0;
			}

			mc.thePlayer.rotationYaw = rotations.get(rotationTicks).yaw;
			mc.thePlayer.rotationPitch = rotations.get(rotationTicks).pitch;
			rotationTicks++;

		}
		if (record.getProperty()) {
			clicks.add(new Placement(null, null));
		} else {
			mc.timer.timerSpeed = 1.0f;
			if (clickTicks >= clicks.size() - 1) {
				clickTicks = 0;
			}
			mc.entityRenderer.getMouseOver(1.0F);

			BlockPos blockPos = clicks.get(clickTicks).blockPos;
			EnumFacing facing = clicks.get(clickTicks).facing;

			if (blockPos != null && facing != null) {
				if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(),
						blockPos.add(new Vec3i(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)), mc.objectMouseOver.sideHit,
						mc.objectMouseOver.hitVec)) {
					mc.thePlayer.swingItem();
				}

			}
			clickTicks++;

		}

	}

	@Listen
	public void onPlace(PlaceEvent event) {
		if (record.getProperty()) {
			clicks.removeLast();
			clicks.add(new Placement(event.getBlockPos(), event.getFacing()));
		}
	}

	@Listen
	public void onMoveInput(MoveInputEvent event) {
		mc.timer.timerSpeed = timer.getProperty();
		if (movementTicks >= movements.size() - 1) {
			movementTicks = 0;
		}

		if (record.getProperty()) {
			movements.add(new Movement(event.isKeyForward(), event.isKeyBack(), event.isKeyLeft(), event.isKeyRight(),
					event.isJump(), event.isSneak()));
		} else {
			mc.timer.timerSpeed = 1.0f;
			event.setKeyForward(movements.get(movementTicks).forward);
			event.setKeyBack(movements.get(movementTicks).backward);
			event.setKeyLeft(movements.get(movementTicks).left);
			event.setKeyRight(movements.get(movementTicks).right);
			event.setKeyJump(movements.get(movementTicks).jump);
			event.setKeySneak(movements.get(movementTicks).sneak);

			movementTicks++;

		}
	}

	private class Movement {
		public boolean forward, backward, left, right, jump, sneak;

		public Movement(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak) {
			this.forward = forward;
			this.backward = backward;
			this.left = left;
			this.right = right;
			this.jump = jump;
			this.sneak = sneak;
		}
	}

	private class Placement {
		public BlockPos blockPos;
		public EnumFacing facing;

		public Placement(BlockPos pos, EnumFacing facing) {
			this.blockPos = pos;
			this.facing = facing;
		}
	}

	private class Rotation {
		public float yaw, pitch;

		public Rotation(float yaw, float pitch) {
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}
}