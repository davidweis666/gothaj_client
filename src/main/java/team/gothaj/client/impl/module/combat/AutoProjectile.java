package team.gothaj.client.impl.module.combat;

import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemFishingRod;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.player.NormalUpdateEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

public class AutoProjectile extends Module {
	public final BooleanProperty onlyKillAura = new BooleanProperty("Only KillAura", true);
	public final NumberProperty maxHurtTime = new NumberProperty("Max Hurt Time", 10, 0, 10, 1);
	public final NumberProperty range = new NumberProperty("Range", 5.0f, 4.0f, 8.0f, 0.1f);
	public final BooleanProperty legitSwitch = new BooleanProperty("Legit Switch", true);
	public final BooleanProperty rotate = new BooleanProperty("Rotate", false);
	public final BooleanProperty instantRotation = new BooleanProperty("Instant Rotation", false)
			.setVisible(rotate::getProperty);
	public final NumberProperty rotationPrediction = new NumberProperty("Rotation Prediction", 4, 1, 8, 1)
			.setVisible(rotate::getProperty);

	public TimerUtil delayTimer = new TimerUtil();

	@Getter
	private boolean rotating;

	public AutoProjectile() {
		super("AutoProjectile", "", ModuleCategory.COMBAT);
		registerProperties(onlyKillAura, maxHurtTime, range, legitSwitch, rotate, instantRotation, rotationPrediction);
	}

	@Listen
	public void onLegitClick(MouseClickEvent event) {
		KillAura ka = getCastedModule(KillAura.class);
		if (onlyKillAura.getProperty() && !ka.isToggled()) {
			rotating = false;
			return;
		}

		Scaffold scaff = getCastedModule(Scaffold.class);
		if (scaff.isToggled()) {
			rotating = false;
			return;
		}
		
		EntityLivingBase target = EntityUtil.getTarget(range.getProperty());
		if (target == null) {
			rotating = false;
			return;
		}

		double distance = EntityUtil.getDistanceToEntityBox(target);

		if (distance <= 3.5 || distance > range.getProperty()) {
			rotating = false;
			return;
		}

		int projectileSlot = InventoryUtil.getProjectileSlot();
		if (projectileSlot == -1) {
			rotating = false;
			return;
		}

		rotating = true;

		double deltaX = (target.posX - target.lastTickPosX) * rotationPrediction.getProperty();
		double deltaZ = (target.posZ - target.lastTickPosZ) * rotationPrediction.getProperty();
		double targetPosX = target.posX + deltaX;
		double targetPosZ = target.posZ + deltaZ;
		
		double playerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();

		double targetPosY = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - 1;

		if (playerY > target.posY + target.getEyeHeight() + 1) {
			targetPosY = target.posY + target.getEyeHeight() - 1;
		} else if (mc.thePlayer.posY < target.posY && playerY > target.posY) {
			targetPosY = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
		} else if (playerY < target.posY) {
			targetPosY = target.posY + target.getEyeHeight() - 0.5;
		}
		
		float rots[] = RotationUtil.getRotationFromPosition(targetPosX, targetPosY, targetPosZ);

		if (rotate.getProperty()) {
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
		}

		if (target.hurtTime > maxHurtTime.getProperty()) {
			return;
		}

		if (!rotate.getProperty() || (RotationUtil.isAngleClose(mc.thePlayer.lastReportedYaw, rots[0], 4)
				&& RotationUtil.isAngleClose(mc.thePlayer.lastReportedPitch, rots[1], 4))) {
			if (delayTimer.hasTimeReached((long) (mc.thePlayer.inventoryContainer.getSlot(36 + projectileSlot)
					.getStack().getItem() instanceof ItemFishingRod ? 300 : 550), false)) {

				int oldSlot = mc.thePlayer.inventory.currentItem;

				mc.thePlayer.inventory.currentItem = projectileSlot;

				mc.rightClickMouse();
				delayTimer.reset();

				rotating = false;

				if (!legitSwitch.getProperty() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemFishingRod))
					mc.thePlayer.inventory.currentItem = oldSlot;
			}
		}
	}
}
