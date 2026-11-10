package team.gothaj.client.impl.module.combat;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.player.SimulatedPlayer;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.game.TimeDelayEvent;
import team.gothaj.client.impl.event.player.*;
import team.gothaj.client.impl.event.render.RenderEntityEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

public class TimerRange extends Module {

    private int balance;
    private boolean preStateIdle;
    private EntityLivingBase target;
    private TimerUtil delayTimer = new TimerUtil();

    public BooleanProperty onlyKillAura = new BooleanProperty("Only Kill Aura", true);
    public ModeProperty mode = new ModeProperty("Mode", "Pre Charge", "Pre Charge", "Post Charge");
    public NumberProperty range = new NumberProperty("Range", 4, 3.3f, 8, 0.1f);
    public NumberProperty baseTimer = new NumberProperty("Base Timer", 1, 0.1f, 2, 0.05f);
    public NumberProperty chargeTimer = new NumberProperty("Charge Timer", 0, 0, 1, 0.01f);
    public NumberProperty delay = new NumberProperty("Delay", 200, 0, 3000, 50);
    public BooleanProperty onlyOnGround = new BooleanProperty("Only On Ground", true);
    public BooleanProperty debug = new BooleanProperty("Debug", false);

    public TimerRange() {
        super("TimerRange", "teleports to player to give you reach", ModuleCategory.COMBAT);
        registerProperties(onlyKillAura, mode, range, baseTimer, chargeTimer, delay, onlyOnGround, debug);
    }

    @Override
    public String getSuffix() {
        return range.getProperty() + "";
    }

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }
    
    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @Listen
    public void onWorldChange(WorldChangeEvent event) {
        reset();
    }

    @Listen
    public void onTick(TickEvent event) {
        balance++;
    }
    
    @Listen
    public void onRender(RenderEntityEvent event) {
    	if (mc.timer.timerSpeed == chargeTimer.getProperty()) {
            for (Entity e : mc.theWorld.loadedEntityList) {
                if (e instanceof EntityLivingBase && e != mc.thePlayer) {
                    try {
                        mc.theWorld.updateEntity(e);
                    } catch (Throwable throwable1) {
                        // Silently handle entity update errors
                    }
                }
            }
        }
    	
    	//mc.timer.timerSpeed = 1f;
    }

    @Listen
    public void onTimeDelay(TimeDelayEvent event) {
    	
        balance--;
        
        // Early exit: if we can't work right now, force timer back to 1 and clear drift.
        boolean working = canWork();
        if (!working) {
            resetTimerIdle();
            return;
        }

        if (mode.getProperty().equalsIgnoreCase("Pre Charge")) {
        	if(isClose()) {
        		mc.timer.timerSpeed = baseTimer.getProperty();
        		mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
        		return;
        	}
        	
            if (target == null) {
                return;
            }

            int maxTicks = -getTicks();

            if (balance == maxTicks && balance < 0) {
                mc.timer.timerSpeed = baseTimer.getProperty();
                mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;

                if (isClose() && mc.thePlayer.hurtTime == 0) {
                    return;
                }

                delayTimer.reset();
                for (int i = maxTicks; i < 0; i++) {
                    try {
                        mc.runTick();
                    } catch (IOException ignored) {
                    }

                    if (isClose()) {
                        if (debug.getProperty()) {
                            Logger.clientChat("Timer Range > hit on target");
                        }
                        
                        mc.clickMouseEvent();
                        break;
                    }
                }
                return;
            }

            if (!isClose() && delayTimer.hasTimeReached(delay.getProperty().longValue(), false)) {
                chargeReleaseIdlePre(maxTicks);
            }
        } else if (mode.getProperty().equalsIgnoreCase("Post Charge")) {
            if (balance == 0) {
                mc.timer.timerSpeed = baseTimer.getProperty();
                mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;

                if (!isClose() && delayTimer.hasTimeReached(delay.getProperty().longValue(), true)) {
                    int maxTicks = getTicks();
                    if(maxTicks == 0)return;

                    for (int i = 0; i < maxTicks+3; i++) {
                        if (isClose()) {
                            if (debug.getProperty()) {
                                Logger.clientChat("Timer Range > hit on target");
                            }
                            
                            mc.clickMouseEvent();
                            break;
                        }

                        try {
                            mc.runTick();
                        } catch (IOException e1) {
                        }
                    }
                }
            } else {
                chargeReleaseIdlePost(0);
            }
        }
    }

    public boolean canWork() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            target = null;
            mc.timer.timerSpeed = baseTimer.getProperty();
            mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
            return false;
        }

        target = EntityUtil.getTarget(14);
        if (target == null) {
            mc.timer.timerSpeed = baseTimer.getProperty();
            mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
            return false;
        }

        if (EntityUtil.getDistanceToEntityBox(target) > range.getProperty()) {
            target = null;
            mc.timer.timerSpeed = baseTimer.getProperty();
            mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
            return false;
        }

        if ((!getCastedModule(KillAura.class).isToggled() && onlyKillAura.getProperty())
                || getCastedModule(Scaffold.class).isToggled()
                || mc.thePlayer.ticksExisted < 10
                || (onlyOnGround.getProperty() && !mc.thePlayer.onGround)
                || (mc.thePlayer.isInWeb || mc.thePlayer.isInLava() || mc.thePlayer.isInWater())
                || mc.thePlayer.isCollidedHorizontally
                || !mc.gameSettings.keyBindForward.pressed
                || mc.gameSettings.keyBindSneak.pressed
                || mc.thePlayer.hurtTime > 0) {
            target = null;
            mc.timer.timerSpeed = baseTimer.getProperty();
            mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
            return false;
        }

        return true;
    }

    public void chargeReleaseIdlePre(int tick) {
        if (tick == Integer.MIN_VALUE) {
            return;
        }

        if (balance > tick) {
            mc.timer.timerSpeed = (float) chargeTimer.getProperty();
            mc.fakeTimer.timerSpeed = 1f;
        } else if (balance < tick) {
            try {
                mc.runTick();
            } catch (IOException e) {
                // Ignore IO exceptions
            }
        }
    }

    /**
     * Post-mode balance correction: charge with {@link #chargeTimer} only when ahead of the tick budget.
     * When behind, do not call {@link net.minecraft.client.Minecraft#runTick()} during charging — that was
     * alternating with charge speed and causing jitter. Resync to base timer and snap balance instead.
     */
    private void chargeReleaseIdlePost(int tick) {
        if (tick == Integer.MIN_VALUE) {
            return;
        }

        if (balance > tick) {
            mc.timer.timerSpeed = (float) chargeTimer.getProperty();
            mc.fakeTimer.timerSpeed = 1f;
        } else if (balance < tick) {
            mc.timer.timerSpeed = baseTimer.getProperty();
            mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
            balance = 0;
        }
    }

    public int getTicks() {
        if (target == null) {
            return 0;
        }

        SimulatedPlayer simPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
        simPlayer.rotationYaw = RotationComponent.active
                ? RotationComponent.rotations[0]
                : mc.thePlayer.rotationYaw;

        int maxIter = Math.min(240, Math.max(40, getEstimatedTicks() + 32));

        SimulatedPlayer.enterSpeculativeMovement();
        try {
            for (int i = 1; i < maxIter; i++) {
                simPlayer.tick();

                if (EntityUtil.getDistanceToEntityBoxFromPosition(
                        simPlayer.posX,
                        simPlayer.posY,
                        simPlayer.posZ,
                        target) <= 3) {
                    return i;
                }
            }
        } finally {
            SimulatedPlayer.leaveSpeculativeMovement();
        }

        return 0;
    }

    public boolean isClose() {
        return EntityUtil.getDistanceToEntityBox(target) <= 3;
    }

    public void reset() {
        preStateIdle = true;
        balance = 0;
        mc.timer.timerSpeed = 1;
        mc.fakeTimer.timerSpeed = 1;
    }

    private void resetTimerIdle() {
        // Prevent timerSpeed from sticking when we're not in a valid state
        if(getCastedModule(KillAura.class).isToggled()) {
        	mc.timer.timerSpeed = baseTimer.getProperty();
        }else {
        	if(onlyKillAura.getProperty()) {
        		mc.timer.timerSpeed = 1;
        	}else {
        		mc.timer.timerSpeed = baseTimer.getProperty();
        	}
        }
        mc.fakeTimer.timerSpeed = mc.timer.timerSpeed;
        balance = 0;
    }

    public int getEstimatedTicks() {
        double speed = Math.max(0.05, MovementUtil.getBaseMoveSpeed());
        double distExtra = EntityUtil.getDistanceToEntityBox(target) - 3.0;
        double rangeExtra = range.getProperty() - 3.0;
        int byDistance = (int) Math.floor(distExtra / speed * 3.0);
        int byRange = (int) Math.floor(rangeExtra / speed * 3.0);
        return Math.max(0, Math.min(byDistance, byRange));
    }
}