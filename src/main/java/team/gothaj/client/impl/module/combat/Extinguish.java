package team.gothaj.client.impl.module.combat;

import lombok.Getter;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

public class Extinguish extends Module {

	private final BooleanProperty instantRotation = new BooleanProperty("Instant Rotation", true);

	@Getter
	private boolean rotating;

	public Extinguish() {
		super("Extinguish", "Automatically extinguishes you", ModuleCategory.COMBAT);
		registerProperties(instantRotation);
	}

	@Listen
	public void onTick(MotionEvent event) {
		if (RotationUtil.isAngleClose(RotationComponent.rotations[1], 90, 40) && mc.thePlayer.isInWater() && mc.thePlayer.getHeldItem() != null
				&& mc.thePlayer.getHeldItem().getItem() == Items.bucket) {
			mc.rightClickMouse();
			rotating = false;
			return;
		}

		if (!mc.thePlayer.isBurning()) {
			rotating = false;
			return;
		}

		int waterBucketSlot = getWaterBucketSlot();
		if (waterBucketSlot == -1) {
			rotating = false;
			return;
		}

		rotating = true;

		mc.thePlayer.inventory.currentItem = waterBucketSlot - 36;

		if (RotationComponent.rotations[1] == 90f) {
			mc.rightClickMouse();
		}

		if (!instantRotation.getProperty()) {
			float[] rots = new float[] { RotationComponent.rotations[0], 90 };

			float yawDelta = RotationUtil.smoothYaw(rots[0], RotationComponent.lastRotations[0], 0.5f);
			float pitchDelta = RotationUtil.smoothPitch(rots[1], RotationComponent.lastRotations[1], 0.5f);

			yawDelta += (Math.random() - Math.random());
			pitchDelta += (Math.random() - Math.random());

			rots[0] = RotationComponent.lastRotations[0] + yawDelta;
			rots[1] = RotationComponent.lastRotations[1] + pitchDelta;

			RotationComponent.setRotations(rots[0], rots[1], 30 + (Math.random() * 5), true);
		} else {
			RotationComponent.setRotations(RotationComponent.rotations[0], 90, 180, true);
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
	}

	public int getWaterBucketSlot() {
		for (int i = 36; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				if (stack != null && stack.getItem() != null && stack.getItem() == Items.water_bucket) {
					return i;
				}
			}
		}
		return -1;
	}
}
