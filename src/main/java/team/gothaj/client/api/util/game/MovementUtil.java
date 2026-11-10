package team.gothaj.client.api.util.game;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import net.minecraft.client.Minecraft;

public class MovementUtil implements Utils {
	public static Minecraft mc = Minecraft.getMinecraft();

	// Speed constants
	public static final double WALK_SPEED = .221;
	public static final double WEB_SPEED = .105 / WALK_SPEED;
	public static final double SWIM_SPEED = .115f / WALK_SPEED;
	public static final double SNEAK_SPEED = .3f;
	public static final double SPRINTING_SPEED = 1.3f;
	public static final double[] DEPTH_STRIDER = {
			1.f, .1645f / SWIM_SPEED / WALK_SPEED, .1995f / SWIM_SPEED / WALK_SPEED, 1.f / SWIM_SPEED
	};

	public static boolean isEntityMoving(Entity ent) {
		return ent.serverPosX != ent.posX || ent.serverPosY != ent.posY || ent.serverPosZ != ent.posZ;
	}
	
	public static boolean isLookingDiagonally() {
	    float yaw = Math.abs(mc.thePlayer.rotationYaw % 90.0F);

	    if (yaw > 45.0F) {
	        yaw = 90.0F - yaw;
	    }

	    return yaw > 22.5F;
	}

	public static boolean isGoingDiagonally() {
		return (Math.abs(mc.thePlayer.motionX) > 0.05 && Math.abs(mc.thePlayer.motionZ) > 0.05);
	}

	public static double getPredictedMotionY(double motionY) {
		return (motionY - 0.08) * 0.98F;
	}

	public static double getDirectionKeybinds(float yaw) {
		float rotationYaw = yaw;

		if (!Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
				&& Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
			rotationYaw += 180.0F;
		}

		float forward = 1.0F;

		if (!Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
				&& Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
			forward = -0.5F;
		} else if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())
				&& !Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
			forward = 0.5F;
		}

		if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())
				&& !Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
			rotationYaw -= 90.0F * forward;
		}

		if (!Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())
				&& Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
			rotationYaw += 90.0F * forward;
		}

		return Math.toRadians(rotationYaw);
	}

	public static double getDirectionKeybinds() {
		float forward = mc.thePlayer.movementInput.realMoveForward;
		float strafe = mc.thePlayer.movementInput.realMoveStrafe;

		if (forward == 0f && strafe == 0f) {
			return mc.thePlayer.rotationYaw;
		}

		double targetYaw = mc.thePlayer.rotationYaw;
		float forwardFactor;

		if (forward < 0f) {
			targetYaw += 180f;
		}

		if (strafe != 0f) {
			if (forward < 0f) {
				forwardFactor = -0.5f;
			} else if (forward > 0f) {
				forwardFactor = 0.5f;
			} else {
				forwardFactor = 1f;
			}

			if (strafe > 0f) {
				targetYaw -= 90f * forwardFactor;
			}

			if (strafe < 0f) {
				targetYaw += 90f * forwardFactor;
			}
		}

		return targetYaw;
	}

	public static double getDirection(float yaw) {
		float rotationYaw = yaw;

		if (mc.thePlayer.moveForward < 0.0F) {
			rotationYaw += 180.0F;
		}

		float forward = 1.0F;

		if (mc.thePlayer.moveForward < 0.0F) {
			forward = -0.5F;
		} else if (mc.thePlayer.moveForward > 0.0F) {
			forward = 0.5F;
		}

		if (mc.thePlayer.moveStrafing > 0.0F) {
			rotationYaw -= 90.0F * forward;
		}

		if (mc.thePlayer.moveStrafing < 0.0F) {
			rotationYaw += 90.0F * forward;
		}

		return Math.toRadians(rotationYaw);
	}

	public static double getDirection(float yaw, float moveForward, float moveStrafing) {
		float rotationYaw = yaw;

		if (moveForward < 0.0F) {
			rotationYaw += 180.0F;
		}

		float forward = 1.0F;

		if (moveForward < 0.0F) {
			forward = -0.5F;
		} else if (moveForward > 0.0F) {
			forward = 0.5F;
		}

		if (moveStrafing > 0.0F) {
			rotationYaw -= 90.0F * forward;
		}

		if (moveStrafing < 0.0F) {
			rotationYaw += 90.0F * forward;
		}

		return Math.toRadians(rotationYaw);
	}

	public static float getthePlayerDirection() {
		float direction = mc.thePlayer.rotationYaw;

		if (mc.thePlayer.moveForward > 0) {
			if (mc.thePlayer.moveStrafing > 0) {
				direction -= 45;
			} else if (mc.thePlayer.moveStrafing < 0) {
				direction += 45;
			}
		} else if (mc.thePlayer.moveForward < 0) {
			if (mc.thePlayer.moveStrafing > 0) {
				direction -= 135;
			} else if (mc.thePlayer.moveStrafing < 0) {
				direction += 135;
			} else {
				direction -= 180;
			}
		} else {
			if (mc.thePlayer.moveStrafing > 0) {
				direction -= 90;
			} else if (mc.thePlayer.moveStrafing < 0) {
				direction += 90;
			}
		}

		return direction;
	}

	public static double getBaseMoveSpeed() {
		return getBaseMoveSpeed(mc.thePlayer);
	}

	public static double getBaseMoveSpeed(EntityPlayer entity) {
		double baseSpeed = 0.2873D;

		if (entity.isUsingItem()) {
			baseSpeed *= 0.2D;
		}

		if (entity.isPotionActive(Potion.moveSpeed)) {
			baseSpeed *= 1.0D + 0.2D * (entity.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
		}

		return baseSpeed;
	}

	public static double getSpeed() {
		return getSpeed(mc.thePlayer);
	}

	public static double getSpeed(EntityPlayer entity) {
		return Math.hypot(entity.motionX, entity.motionZ);
	}

	public static boolean isMoving() {
		return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
	}

	public static boolean isInLiquid() {
		return mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
	}

	public static boolean enoughMovementForSprinting() {
		return Math.abs(mc.thePlayer.moveForward) >= .8f || Math.abs(mc.thePlayer.moveStrafing) >= .8f;
	}

	public static double baseSpeed() {
		double speed;
		boolean useModifiers = false;

		if (mc.thePlayer.isInWeb)
			speed = WEB_SPEED * WALK_SPEED;
		else if (isInLiquid()) {
			speed = SWIM_SPEED * WALK_SPEED;

			final int level = EnchantmentHelper.getDepthStriderModifier(mc.thePlayer);

			if (level > 0) {
				speed *= DEPTH_STRIDER[level];
				useModifiers = true;
			}
		} else if (mc.thePlayer.isSneaking()) {
			speed = SNEAK_SPEED * WALK_SPEED;
		} else {
			speed = WALK_SPEED;
			useModifiers = true;
		}

		if (useModifiers) {
			if (enoughMovementForSprinting())
				speed *= SPRINTING_SPEED;

			if (mc.thePlayer.isPotionActive(Potion.moveSpeed))
				speed *= 1 + (.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1));

			if (mc.thePlayer.isPotionActive(Potion.moveSlowdown))
				speed = .29;
		}

		return speed;
	}

	public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
		if (moveForward < 0F) rotationYaw += 180F;

		float forward = 1F;

		if (moveForward < 0F) forward = -0.5F;
		else if (moveForward > 0F) forward = 0.5F;

		if (moveStrafing > 0F) rotationYaw -= 90F * forward;
		if (moveStrafing < 0F) rotationYaw += 90F * forward;

		return Math.toRadians(rotationYaw);
	}

	public static void correctMovement(MoveInputEvent event, float yaw) {
		boolean up    = event.isKeyForward();
		boolean down  = event.isKeyBack();
		boolean left  = event.isKeyLeft();
		boolean right = event.isKeyRight();

		if (mc.thePlayer != null && (up != down || left != right)) {
			float playerYaw = mc.thePlayer.rotationYaw;

			float delta = (up == down)
					? (left ? -90.0F : 90.0F)
					: (up ? (left == right ? 0.0F : (left ? -45.0F : 45.0F))
					: (left == right ? 180.0F : (left ? -135.0F : 135.0F)));

			float y = MathHelper.wrapAngleTo180_float(playerYaw + delta - yaw);

			up = down = left = right = false;

			if (y >= -22.5F && y < 22.5F) up = true;
			if (y < -22.5F && y >= -67.5F) { left = true; up = true; }
			if (y < -67.5F && y >= -112.5F) left = true;
			if (y < -112.5F && y >= -157.5F) { left = true; down = true; }
			if (y <= -157.5F || y >= 157.5F) down = true;
			if (y >= 112.5F && y < 157.5F) { right = true; down = true; }
			if (y >= 67.5F && y < 112.5F) right = true;
			if (y >= 22.5F && y < 67.5F) { right = true; up = true; }
		}

		event.setKeyForward(up);
		event.setKeyBack(down);
		event.setKeyLeft(left);
		event.setKeyRight(right);

		event.recomputeFromKeys();
	}

	public static void strafe(double speed) {
		float direction = (float) Math.toRadians(getthePlayerDirection());

		if (isMoving()) {
			mc.thePlayer.motionX = -Math.sin(direction) * speed;
			mc.thePlayer.motionZ = Math.cos(direction) * speed;
		} else {
			mc.thePlayer.motionX = 0;
			mc.thePlayer.motionZ = 0;
		}
	}

	public static void strafe(float speed) {
		double yaw = getDirection(mc.thePlayer.rotationYaw);
		mc.thePlayer.motionX = -Math.sin((float) yaw) * speed;
		mc.thePlayer.motionZ = Math.cos((float) yaw) * speed;
	}

	public static void strafe() {
		float direction = (float) Math.toRadians(getthePlayerDirection());

		if (isMoving()) {
			mc.thePlayer.motionX = -Math.sin(direction) * baseSpeed();
			mc.thePlayer.motionZ = Math.cos(direction) * baseSpeed();
		} else {
			mc.thePlayer.motionX = 0;
			mc.thePlayer.motionZ = 0;
		}
	}

	public static void boost() {
		float direction = (float) Math.toRadians(getthePlayerDirection());

		if (isMoving()) {
			mc.thePlayer.motionX = -Math.sin(direction) * baseSpeed() * 0.045f;
			mc.thePlayer.motionZ = Math.cos(direction) * baseSpeed() * 0.045f;
		}
	}

	public static void boost(float speed) {
		float direction = (float) Math.toRadians(mc.thePlayer.rotationYaw);

		mc.thePlayer.motionX = -Math.sin(direction) * speed;
		mc.thePlayer.motionZ = Math.cos(direction) * speed;
	}

	public static void strafe(float speed, float yaw) {
		mc.thePlayer.motionX = -Math.sin((float) yaw) * speed;
		mc.thePlayer.motionZ = Math.cos((float) yaw) * speed;
	}

	public static void strafe(float speed, float yaw, float moveForward, float moveStrafing) {
		yaw = (float) getDirection((float) yaw, moveForward, moveStrafing);
		mc.thePlayer.motionX = -Math.sin(yaw) * speed;
		mc.thePlayer.motionZ = Math.cos(yaw) * speed;
	}
}
