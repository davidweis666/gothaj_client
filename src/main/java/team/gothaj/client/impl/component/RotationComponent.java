package team.gothaj.client.impl.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import team.gothaj.client.api.component.Component;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.event.player.*;
import team.gothaj.client.impl.event.player.move.JumpCorrectionEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.module.combat.Annoyer;
import team.gothaj.client.impl.module.combat.AutoProjectile;
import team.gothaj.client.impl.module.combat.Extinguish;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.visual.Camera;
import team.gothaj.client.impl.module.world.AutoBedDefense;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * Handles all rotation related logic for the client, including:
 * <ul>
 * <li>Rotation speed calculations</li>
 * <li>Movement correction</li>
 * <li>Rendering of rotations</li>
 * </ul>
 * <p>
 * All rotation handling must go through this component. If rotations are
 * calculated manually outside this component, I'm going to crash tf out.
 * </p>
 *
 * @author Kawase
 * @since 15.08.2025
 */
@Getter
@Setter
public class RotationComponent extends Component implements Utils {
	public static float[] rotations, lastRotations, targetRotations, lastServerRotations;
	private static double yawSpeed, pitchSpeed;
	private static boolean shouldMoveFix;
	private static boolean shouldInputMoveFix;

	@Getter
	@Setter
	public static boolean active;

	public static void setRotations(float yaw, float pitch, double speed, boolean shouldMoveFix) {
		setRotations(yaw, pitch, speed, speed, shouldMoveFix);
	}

	public static void setRotations(float[] rotations, double speed, boolean shouldMoveFix) {
		setRotations(rotations[0], rotations[1], speed, speed, shouldMoveFix);
	}

	public static void setRotations(float yaw, float pitch, double yawSpeed, double pitchSpeed, boolean shouldMoveFix) {
		setRotations(yaw, pitch, yawSpeed, pitchSpeed, shouldMoveFix, shouldMoveFix);
	}

	public static void setRotations(float yaw, float pitch, double yawSpeed, double pitchSpeed, boolean shouldMoveFix,
			boolean shouldInputMoveFix) {
		RotationComponent.targetRotations = new float[] { yaw, pitch };
		RotationComponent.yawSpeed = yawSpeed;
		RotationComponent.pitchSpeed = pitchSpeed;
		RotationComponent.shouldMoveFix = shouldMoveFix;
		RotationComponent.shouldInputMoveFix = shouldInputMoveFix;

		setActive(true);
		fixRotations();
	}

	@Listen
	public void onUpdateEvent(NormalUpdateEvent event) {
		if (!isActive() || rotations == null || lastRotations == null || targetRotations == null
				|| lastServerRotations == null) {
			rotations = lastRotations = targetRotations = lastServerRotations = new float[] { mc.thePlayer.rotationYaw,
					mc.thePlayer.rotationPitch };
		}

		if (isActive()) {
			fixRotations();
		}
	}

	@Listen
	public void onInput(MoveInputEvent event) {
		if (isActive() && shouldInputMoveFix && rotations != null) {
			MovementUtil.correctMovement(event, rotations[0]);
		}
	}

	@Listen
	public void onVelocity(AttackMotionEvent event) {
		if (isActive() && shouldMoveFix && rotations != null) {
			event.setYaw(rotations[0]);
		}
	}

	@Listen
	public void onMoveFlyingEvent(MoveFlyingEvent event) {
		if (isActive() && (shouldMoveFix) && rotations != null) {
			event.setYaw(rotations[0]);
		}
	}

	@Listen
	public void onCorrect(JumpCorrectionEvent event) {
		if (isActive() && (shouldMoveFix) && rotations != null) {
			event.setYaw(rotations[0]);
		}
	}

	@Listen
	public void onLookEvent(LookEvent event) {
		if (active && rotations != null) {
			event.setYaw(rotations[0]);
			event.setPitch(rotations[1]);
		}
	}

	@Listen
	public void onMotion(MotionEvent event) {
		if (isActive() && rotations != null) {
			normalizeRotations();

			final float yaw = rotations[0];
			final float pitch = rotations[1];

			mc.thePlayer.prevRotationYawHead = lastRotations[0];
			mc.thePlayer.prevRenderPitchHead = lastRotations[1];
			mc.thePlayer.renderPitchHead = pitch;
			mc.thePlayer.rotationYawHead = yaw;

			if (Camera.yawBody.getProperty()
					&& GothajClient.getInstance().getModuleManager().find(Camera.class).isToggled())
				updateRenderYawOffset(yaw);

			event.setYaw(yaw);
			event.setPitch(pitch);

			lastServerRotations = new float[] { yaw, pitch };

			if (Math.abs(MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw)) < 1
					&& Math.abs(rotations[1] - mc.thePlayer.rotationPitch) < 1) {
				setActive(false);
			}

			lastRotations = rotations;
		} else {
			lastRotations = new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch };
		}

		targetRotations = new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch };
	}

	private static void updateRenderYawOffset(float yaw) {
		double deltaX = mc.thePlayer.posX - mc.thePlayer.prevPosX;
		double deltaZ = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
		float targetBodyYaw = mc.thePlayer.renderYawOffset;

		if ((float) (deltaX * deltaX + deltaZ * deltaZ) > 0.0025000002F) {
			targetBodyYaw = (float) MathHelper.atan2(deltaZ, deltaX) * 180.0F / (float) Math.PI - 90.0F;
		}

		if (mc.thePlayer.swingProgress > 0.0F) {
			targetBodyYaw = yaw;
		}

		float bodyDelta = MathHelper.wrapAngleTo180_float(targetBodyYaw - mc.thePlayer.renderYawOffset);
		mc.thePlayer.renderYawOffset += bodyDelta * 0.3F;

		float headDelta = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.renderYawOffset);
		if (headDelta < -75.0F) {
			headDelta = -75.0F;
		}

		if (headDelta >= 75.0F) {
			headDelta = 75.0F;
		}

		mc.thePlayer.renderYawOffset = yaw - headDelta;

		if (headDelta * headDelta > 2500.0F) {
			mc.thePlayer.renderYawOffset += headDelta * 0.2F;
		}
	}

	public static void fixRotations() {
		if (lastRotations == null || targetRotations == null)
			return;

		final float lastYaw = lastRotations[0];
		final float lastPitch = lastRotations[1];
		float targetYaw = targetRotations[0];
		float targetPitch = targetRotations[1];

		targetYaw = wrapAngleDelta(targetYaw, lastYaw);

		if (yawSpeed != 180) {
			targetYaw = RotationUtil.smoothRotation(lastYaw, targetYaw, (float) yawSpeed);
		}

		if (pitchSpeed != 180) {
			targetPitch = RotationUtil.smoothRotation(lastPitch, targetPitch, (float) pitchSpeed);
		}

		applyRotations(targetYaw, targetPitch, lastYaw, lastPitch);
	}

	private static float wrapAngleDelta(float target, float current) {
		while (target - current < -180.0F)
			target += 360.0F;
		while (target - current >= 180.0F)
			target -= 360.0F;
		return target;
	}

	private static void applyRotations(float targetYaw, float targetPitch, float lastYaw, float lastPitch) {
		float[] fixedRotations = RotationUtil.getFixedRotations(new float[] { targetYaw, targetPitch },
				new float[] { lastYaw, lastPitch });

		rotations = new float[] { fixedRotations[0], MathHelper.clamp(fixedRotations[1], -90.0f, 90.0f) };
		
		if ((!GothajClient.getInstance().getModuleManager().getAndCast(KillAura.class).isToggled()
				|| GothajClient.getInstance().getModuleManager().getAndCast(KillAura.class).target == null)
				&& !GothajClient.getInstance().getModuleManager().getAndCast(Scaffold.class).isToggled()
				&& !GothajClient.getInstance().getModuleManager().getAndCast(AutoBedDefense.class).isToggled()) {
			

			float yawDelta = RotationUtil.smoothYaw(rotations[0], RotationComponent.lastRotations[0], 0.5f);
			float pitchDelta = RotationUtil.smoothPitch(rotations[1], RotationComponent.lastRotations[1], 0.5f);

			yawDelta += (Math.random() - Math.random());
			pitchDelta += (Math.random() - Math.random());

			rotations[0] = RotationComponent.lastRotations[0] + yawDelta;
			rotations[1] = RotationComponent.lastRotations[1] + pitchDelta;

		}
		
		mc.entityRenderer.getMouseOver(1);
	}

	private static void normalizeRotations() {
		if (rotations == null || lastRotations == null)
			return;

		float currentYaw = rotations[0];
		float currentPitch = rotations[1];
		float prevYaw = lastRotations[0];

		while (currentYaw - prevYaw < -180.0F) {
			currentYaw += 360.0F;
		}
		while (currentYaw - prevYaw >= 180.0F) {
			currentYaw -= 360.0F;
		}

		rotations[0] = currentYaw;
		rotations[1] = MathHelper.clamp(currentPitch, -90.0F, 90.0F);
	}
}
