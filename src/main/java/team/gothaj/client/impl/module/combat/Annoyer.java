package team.gothaj.client.impl.module.combat;

import lombok.Getter;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.player.RayCastUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.eventbus.Listen;

public class Annoyer extends Module {

	private final BooleanProperty instantRotation = new BooleanProperty("Instant Rotation", true);

	private final BooleanProperty lava = new BooleanProperty("Place Lava", true);

	private final BooleanProperty blocks = new BooleanProperty("Place Blocks", false);
	private final NumberProperty blockPlaceDelay = new NumberProperty("Block Place Delay", 3000, 500, 10000, 100)
			.setVisible(() -> blocks.getProperty());

	private BlockPos lavaPosition;

	public TimerUtil blockTimer = new TimerUtil();

	private boolean isLava;
	@Getter
	private boolean rotating;

	public Annoyer() {
		super("Annoyer", "Makes players hate you", ModuleCategory.COMBAT);
		registerProperties(instantRotation, lava, blocks, blockPlaceDelay);
	}

	@Listen
	public void onTick(MouseClickEvent event) {
		if (lava.getProperty())
			lava();
		if (blocks.getProperty())
			blocks();
	}

	public void lava() {
		EntityLivingBase target = EntityUtil.getTarget(3);
		if (target == null) {
			rotating = false;
			isLava = false;
			return;
		}

		/*if (target.isInLava() && lavaPosition != null) {
			float rots[] = RotationUtil.getRotationFromPosition(lavaPosition.getX(), lavaPosition.getY(),
					lavaPosition.getZ());

			if (instantRotation.getProperty()) {
				RotationComponent.setRotations(rots[0], rots[1], 180, true);
			} else {
				float yawDelta = RotationUtil.smoothYaw(rots[0], RotationComponent.lastRotations[0], 0.5f);
				float pitchDelta = RotationUtil.smoothPitch(rots[1], RotationComponent.lastRotations[1], 0.5f);

				yawDelta += (Math.random() - Math.random());
				pitchDelta += (Math.random() - Math.random());

				rots[0] = RotationComponent.lastRotations[0] + yawDelta;
				rots[1] = RotationComponent.lastRotations[1] + pitchDelta;

				RotationComponent.setRotations(rots[0], rots[1], 30 + (Math.random() * 5), true);
			}

			if (RotationUtil.isAngleClose(RotationComponent.rotations[0], rots[0], 2)
					&& RotationUtil.isAngleClose(RotationComponent.rotations[1], rots[1], 2)) {

				mc.rightClickMouse();

				lavaPosition = null;

				rotating = false;
				isLava = false;
			}
		}*/

		if (EntityUtil.getDistanceToEntityBox(target) < 1.5) {
			rotating = false;
			isLava = false;
			return;
		}

		int lavaSlot = getLavaSlot();

		if (lavaSlot == -1) {
			rotating = false;
			isLava = false;
			return;
		}

		if (target.isBurning() || target.isInWater()) {
			rotating = false;
			isLava = false;
			return;
		}
		rotating = true;
		isLava = true;

		float rots[] = RotationUtil.getDirectionToBlock(target.posX, target.posY - 1, target.posZ, EnumFacing.UP);

		if (instantRotation.getProperty()) {
			RotationComponent.setRotations(rots[0], rots[1], 180, true);
		} else {
			float yawDelta = RotationUtil.smoothYaw(rots[0], RotationComponent.lastRotations[0], 0.5f);
			float pitchDelta = RotationUtil.smoothPitch(rots[1], RotationComponent.lastRotations[1], 0.5f);

			yawDelta += (Math.random() - Math.random());
			pitchDelta += (Math.random() - Math.random());

			rots[0] = RotationComponent.lastRotations[0] + yawDelta;
			rots[1] = RotationComponent.lastRotations[1] + pitchDelta;

			RotationComponent.setRotations(rots[0], rots[1], 30 + (Math.random() * 5), true);
		}
		
		if (RotationUtil.isAngleClose(mc.thePlayer.lastReportedYaw, rots[0], 2)
				&& RotationUtil.isAngleClose(mc.thePlayer.lastReportedPitch, rots[1], 1)) {
			mc.thePlayer.inventory.currentItem = lavaSlot - 36;

			mc.rightClickMouse();
			
			rotating = false;
			isLava = false;

			lavaPosition = new BlockPos(target.posX, target.posY - 1, target.posZ);
		}
	}

	public void blocks() {
		if (isLava) {
			return;
		}

		EntityLivingBase target = EntityUtil.getTarget(4.5f);
		if (target == null) {
			rotating = false;
			return;
		}

		if (EntityUtil.getDistanceToEntityBox(target) < 1.5) {
			rotating = false;
			return;
		}

		int blockSlot = InventoryUtil.getBlockSlot();

		if (blockSlot == -1) {
			rotating = false;
			return;
		}

		if (target.isInWater()) {
			rotating = false;
			return;
		}

		if (!blockTimer.hasTimeReached(blockPlaceDelay.getProperty().longValue())) {
			rotating = false;
			return;
		}

		rotating = true;

		mc.thePlayer.inventory.currentItem = blockSlot;

		float rots[] = RotationUtil.getRotationFromPosition(target.posX, target.posY - 1, target.posZ);
		
		if (instantRotation.getProperty()) {
			RotationComponent.setRotations(rots[0], rots[1], 180, true);
		} else {
			RotationComponent.setRotations(rots[0], rots[1], 30 + (Math.random() * 5), true);
		}
		
		if (RotationUtil.isAngleClose(mc.thePlayer.lastReportedYaw, rots[0], 4)
				&& RotationUtil.isAngleClose(mc.thePlayer.lastReportedPitch, rots[1], 4)) {

			mc.rightClickMouse();
			rotating = false;
			blockTimer.reset();
		}
	}

	@Override
	public void onEnable() {
		super.onEnable();
	}

	@Override
	public void onDisable() {
		super.onDisable();

		rotating = false;
		isLava = false;
	}

	public int getLavaSlot() {
		for (int i = 36; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				if (stack != null && stack.getItem() != null && stack.getItem() == Items.lava_bucket) {
					return i;
				}
			}
		}
		return -1;
	}
}
