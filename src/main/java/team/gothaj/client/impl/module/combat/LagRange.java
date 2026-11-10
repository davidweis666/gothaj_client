package team.gothaj.client.impl.module.combat;

import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.ModuleCategory;

import net.minecraft.entity.EntityLivingBase;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.player.RayCastUtil;
import team.gothaj.client.api.util.player.SimulatedPlayer;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.List;

public class LagRange extends Module {

	public ModeProperty mode = new ModeProperty("Release Mode", "Delay", "Delay", "Full", "Timer Range");
	public BooleanProperty confuse = new BooleanProperty("Confuse", false).setVisible(() -> mode.is("Timer Range"));

	public NumberProperty delay = new NumberProperty("Delay", 150, 50, 1000, 50)
			.setVisible(() -> mode.getProperty().equalsIgnoreCase("Delay"));

	public NumberProperty lag = new NumberProperty("Lag", 150, 50, 5000, 5)
			.setVisible(() -> mode.getProperty().equalsIgnoreCase("Full"));

	public NumberProperty range = new NumberProperty("Range", 6, 0, 10, 0.05f)
			.setVisible(() -> mode.getProperty().equalsIgnoreCase("Timer Range"));

	public BooleanProperty onlyKillAura = new BooleanProperty("Only Kill Aura", true);
	
	public BooleanProperty blinkingInBound = new BooleanProperty("Blinking Inbound", false);
	
	public TimerUtil timer = new TimerUtil();

	public EntityLivingBase target;
	
	public boolean wasBlinkingCancelled = false;

	public LagRange() {
		super("LagRange", "Blinks to give you reach and move you out of target reach", ModuleCategory.COMBAT);
		registerProperties(mode, confuse, delay, lag, range, blinkingInBound, onlyKillAura);
	}

	@Override
	public String getSuffix() {
		return mode.getProperty();
	}
	
	@Override
	public void onDisable() {
		GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class).stopBlinking(true, true);
		super.onDisable();
	}

	@Listen
	public void onTick(TickEvent event) {
		BlinkComponent blink = GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class);
		
		if (!canWork(blink)) {
			blink.stopBlinking(blinkingInBound.getProperty(), true);
			timer.reset();
			return;
		}

		if (inHitRange() && blink.isBlinkingOutbound()) {
			Logger.debugChat(this, "Lag Range > hit on target");
		}

		switch (mode.getProperty()) {
		case "Delay" -> {
			if (inHitRange() && target.hurtTime <= 1) {
				blink.stopBlinking(blinkingInBound.getProperty(), true);
			} else {
				blink.startBlinking(blinkingInBound.getProperty(), true);

				blink.releasePacketsToMS(delay.getProperty().intValue());
			}
		}
		case "Full" -> {
			
			if (inHitRange() && target.hurtTime <= 1) {
				blink.stopBlinking(blinkingInBound.getProperty(), true);
				timer.reset();
			} else {
				blink.startBlinking(blinkingInBound.getProperty(), true);

				if (timer.hasTimeReached(lag.getProperty().intValue(), true)) {
					blink.stopBlinking(blinkingInBound.getProperty(), true);
				}
			}
		}
		
		case "Timer Range" -> {
			if (EntityUtil.getDistanceToEntityBox(target) <= range.getProperty()) {

				if (timer.hasTimeReached(2000, true)) {
					blink.stopBlinking(blinkingInBound.getProperty(), true);
				}

				blink.startBlinking(blinkingInBound.getProperty(), true);

				if (inHitRange() && target.hurtTime <= 1 && mc.objectMouseOver.entityHit == target && (!confuse.getProperty() || RayCastUtil.rayTraceFromTarget(target, range.getProperty(), new float[] {target.rotationYawHead, target.rotationPitch}) != mc.thePlayer)) {
					Logger.debugChat(this, "Lag Range > hit on target");

					blink.stopBlinking(blinkingInBound.getProperty(), true);
				}
			} else {
				timer.reset();
				blink.stopBlinking(blinkingInBound.getProperty(), true);
			}
		}
		}
	}

	public boolean canWork(BlinkComponent blink) {
		if (mc.thePlayer == null || mc.theWorld == null) {
			target = null;
			mc.timer.timerSpeed = 1f;
			return false;
		}

		target = EntityUtil.getTarget(15);

		if (target == null) {
			mc.timer.timerSpeed = 1f;
			return false;
		}

		KillAura ka = getCastedModule(KillAura.class);

		if ((!ka.isToggled() && onlyKillAura.getProperty()) || getCastedModule(Scaffold.class).isToggled()
				|| mc.thePlayer.ticksExisted < 10
				|| (mc.thePlayer.isInWeb || mc.thePlayer.isInLava() || mc.thePlayer.isInWater())
				|| mc.thePlayer.isCollidedHorizontally || !mc.gameSettings.keyBindForward.pressed
				|| mc.thePlayer.hurtTime > 0 || !MovementUtil.isMoving() || isPlayerMovingBackwards()) {

			if(!wasBlinkingCancelled) {
				blink.stopBlinking(blinkingInBound.getProperty(), true);
				wasBlinkingCancelled = true;
			}
			target = null;
			mc.timer.timerSpeed = 1f;
			return false;
		}

		if (EntityUtil.getDistanceToEntityBox(target) <= ka.range.getProperty()
				&& (ka.autoBlock.getProperty() == "Blink" || ka.autoBlock.getProperty() == "Hypixel"
						|| ka.autoBlock.getProperty() == "Watchdog")) {
			target = null;
			mc.timer.timerSpeed = 1f;
			return false;
		}
		
		wasBlinkingCancelled = false;
		return true;
	}

	private boolean inHitRange() {
		return EntityUtil.getDistanceToEntityBox(target) <= 3.0;
	}

	private boolean isPlayerMovingBackwards() {
		return EntityUtil.getDistanceToEntityBox(target) > EntityUtil.getDistanceToEntityBoxFromPosition(
				mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ, target);
	}
}
