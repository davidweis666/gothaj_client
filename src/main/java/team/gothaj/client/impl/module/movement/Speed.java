package team.gothaj.client.impl.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author Kawase
 * @since 31.08.2025
 */
public class Speed extends Module {


    private final ModeProperty speedMode = new ModeProperty("Speed Mode", "Strafe", "Strafe", "Vanilla", "Custom Tick", "Polar 1.20", "Dev");

    private final BooleanProperty PolarCollide = new BooleanProperty("Polar Collide", false).setVisible(() -> speedMode.is("Polar 1.20"));

    private final NumberProperty PolarCollideForce = new NumberProperty("Slow Speed", 0.06f, 0.0f, 0.2f, 0.001f).setVisible(() -> speedMode.is("Polar 1.20"));

    private final BooleanProperty PolarLowJump = new BooleanProperty("Low Jump", false).setVisible(() -> speedMode.is("Polar 1.20"));

    private final NumberProperty PolarLowJumpIntervale = new NumberProperty("Low Jump Intervale", 2, 2, 4, 1).setVisible(() -> speedMode.is("Polar 1.20"));
    private final NumberProperty PolarLowJumpValue = new NumberProperty("Slow Speed", 0.39f, 0.3f, 0.42f, 0.001f).setVisible(() -> speedMode.is("Polar 1.20"));

    private final BooleanProperty onlyWhenMoving = new BooleanProperty("Only When Moving", false);

    private final NumberProperty devSpeed = new NumberProperty("Dev Speed", 0.45f, 0.1f, 2.0f, 0.01f).setVisible(() -> speedMode.is("Dev"));

    private final ModeProperty customTickType = new ModeProperty("Custom Tick Type", "Cycle", "Cycle", "Ticks Existed").setVisible(() -> speedMode.is("Custom Tick"));

    private final BooleanProperty customTickAutoJump = new BooleanProperty("Auto Jump", true).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Cycle"));

    private final BooleanProperty useBlink = new BooleanProperty("Blink", false).setVisible(() -> speedMode.is("Custom Tick"));

    private final ModeProperty blinkMode = new ModeProperty("Blink Mode", "On Release", "On Release", "After Release").setVisible(() -> speedMode.is("Custom Tick") && useBlink.getProperty());

    private final NumberProperty customTickSlowTicks = new NumberProperty("Slow Ticks", 2, 1, 20, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Cycle"));
    private final NumberProperty customTickSlowSpeed = new NumberProperty("Slow Speed", 0.6f, 0.0f, 1.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Cycle"));

    private final NumberProperty customTickBoostTicks = new NumberProperty("Boost Ticks", 10, 1, 40, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Cycle"));
    private final NumberProperty customTickBoostSpeed = new NumberProperty("Boost Speed", 1.6f, 0.0f, 3.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Cycle"));

    private final NumberProperty customTickResetTicks = new NumberProperty("Reset Ticks", 0, 0, 20, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Cycle"));

    private final NumberProperty ticksMod1 = new NumberProperty("Mod 1", 4, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty ticksSpeed1 = new NumberProperty("Speed 1", 0.732f, 0.0f, 2.0f, 0.001f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty ticksMod2 = new NumberProperty("Mod 2", 3, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty ticksSpeed2 = new NumberProperty("Speed 2", 0.731f, 0.0f, 2.0f, 0.001f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty ticksMod3 = new NumberProperty("Mod 3", 2, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty ticksSpeed3 = new NumberProperty("Speed 3", 0.729f, 0.0f, 2.0f, 0.001f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty ticksSpeedDefault = new NumberProperty("Speed Default", 0.73f, 0.0f, 2.0f, 0.001f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty thresholdMod1 = new NumberProperty("Threshold Mod 1", 256, 2, 512, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty thresholdValue1 = new NumberProperty("Threshold Val 1", 0, 0, 20, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty thresholdMod2 = new NumberProperty("Threshold Mod 2", 6, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty thresholdValue2 = new NumberProperty("Threshold Val 2", 1, 0, 20, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty thresholdMod3 = new NumberProperty("Threshold Mod 3", 2, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty thresholdValue3 = new NumberProperty("Threshold Val 3", 2, 0, 20, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    private final NumberProperty thresholdDefault = new NumberProperty("Threshold Default", 3, 0, 20, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    // Release configuration
    private final NumberProperty releaseTicks = new NumberProperty("Release Ticks", 10, 1, 100, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));

    // Dynamic release settings
    private final BooleanProperty dynamicRelease = new BooleanProperty("Dynamic Release", false).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed"));
    private final NumberProperty releaseMultiplier = new NumberProperty("Release Multiplier", 1.0f, 0.5f, 2.0f, 0.1f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && dynamicRelease.getProperty());
    private final NumberProperty dynamicReleaseSpeed = new NumberProperty("Dynamic Release Speed", 1.55f, 0.0f, 3.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && dynamicRelease.getProperty());

    // Modulo-based release settings (only visible when dynamic release is disabled)
    private final NumberProperty releaseTicksMod1 = new NumberProperty("Release Mod 1", 4, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());
    private final NumberProperty releaseTicksSpeed1 = new NumberProperty("Release Speed 1", 1.8f, 0.0f, 3.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());

    private final NumberProperty releaseTicksMod2 = new NumberProperty("Release Mod 2", 3, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());
    private final NumberProperty releaseTicksSpeed2 = new NumberProperty("Release Speed 2", 1.6f, 0.0f, 3.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());

    private final NumberProperty releaseTicksMod3 = new NumberProperty("Release Mod 3", 2, 2, 256, 1).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());
    private final NumberProperty releaseTicksSpeed3 = new NumberProperty("Release Speed 3", 1.4f, 0.0f, 3.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());

    private final NumberProperty releaseTicksSpeedDefault = new NumberProperty("Release Speed Default", 1.55f, 0.0f, 3.0f, 0.05f).setVisible(() -> speedMode.is("Custom Tick") && customTickType.is("Ticks Existed") && !dynamicRelease.getProperty());

    private int customTickTickCounter = 0;
    private int balanceCounter = 0;
    private int releaseTickCounter = 0;
    private int counter = 0;
    private int collisions = 0;
    private int airTicks = 0;
    private boolean releasing = false;

    public Speed() {
        super("Speed", "wee ooo eeee", ModuleCategory.MOVEMENT);
        this.registerProperties(
                speedMode,PolarLowJump, PolarLowJumpIntervale, PolarCollide, PolarCollideForce,customTickType,
                customTickAutoJump, useBlink, blinkMode, customTickSlowTicks, customTickSlowSpeed,
                customTickBoostTicks, customTickBoostSpeed, customTickResetTicks,
                ticksMod1, ticksSpeed1, ticksMod2, ticksSpeed2,
                ticksMod3, ticksSpeed3, ticksSpeedDefault,
                thresholdMod1, thresholdValue1, thresholdMod2, thresholdValue2,
                thresholdMod3, thresholdValue3, thresholdDefault,
                releaseTicks,
                dynamicRelease, releaseMultiplier, dynamicReleaseSpeed,
                releaseTicksMod1, releaseTicksSpeed1,
                releaseTicksMod2, releaseTicksSpeed2,
                releaseTicksMod3, releaseTicksSpeed3,
                releaseTicksSpeedDefault,
                devSpeed);
    }

    @Listen
    public void onMotion(MotionEvent event) {

        if (onlyWhenMoving.getProperty() && !MovementUtil.isMoving()) return;

        if (mc.thePlayer.onGround) {
            if (speedMode.is("Strafe") || speedMode.is("Vanilla") || speedMode.is("Dev")) {
                mc.thePlayer.jump();
            } else if (speedMode.is("Custom Tick") && customTickType.is("Cycle") && customTickAutoJump.getProperty()) {
                mc.thePlayer.jump();
            } else if (speedMode.is("Custom Tick") && customTickType.is("Ticks Existed")) {
                mc.thePlayer.jump();
            }
        }

        switch (speedMode.getProperty()) {
            case "Polar 1.20" -> {
                if (PolarCollide.getProperty()) {
                    collisions = 0;
                    float total = 0;
                    AxisAlignedBB col = mc.thePlayer.getEntityBoundingBox();
                    if (mc.thePlayer.moveForward > 2) ;
                    col = col.expand(mc.thePlayer.moveForward / 10, 0.1, mc.thePlayer.moveForward / 10);
                    for (Entity e : Minecraft.getMinecraft().theWorld.getLoadedEntityList())
                        if (e.getEntityId() != mc.thePlayer.getEntityId() && col.intersectsWith(e.getEntityBoundingBox())) {
                            break;
                        }
                    for (Entity e : Minecraft.getMinecraft().theWorld.getLoadedEntityList()) {
                        if (e.getEntityId() != mc.thePlayer.getEntityId() && col.intersectsWith(e.getEntityBoundingBox())) {
                            if (e instanceof EntityArmorStand || e instanceof EntityPlayer || e instanceof EntityLiving) {
                                collisions += 1;
                                float yaw = (float) Math.toRadians(mc.thePlayer.rotationYaw);
                                float boost = (float) (PolarCollideForce.getProperty() * collisions);
                                total += boost;
                                mc.thePlayer.addVelocity(-Math.sin(yaw) * boost, 0.0, Math.cos(yaw) * boost);
                            }
                        }
                    }
                }
                if (PolarLowJump.getProperty()) {
                    if (mc.thePlayer.onGround) {
                        counter++;
                        mc.thePlayer.jump();

                        if (counter % PolarLowJumpIntervale.getProperty() == 0 || counter == 1)
                            mc.thePlayer.motionY = PolarLowJumpValue.getProperty();
                    }
                }

            }
            case "Strafe" -> MovementUtil.strafe();
            case "Vanilla" -> MovementUtil.strafe(1);
            case "Dev" -> {
                if (mc.thePlayer.onGround) {
                    airTicks = 0;
                } else {
                    airTicks++;
                    if (airTicks == 1) {
                        mc.thePlayer.motionY += 0.057f;
                    } else if (airTicks == 3) {
                        mc.thePlayer.motionY -= 0.1309f;
                    } else if (airTicks == 4) {
                        mc.thePlayer.motionY -= 0.2f;
                    }
                }
                if (GothajClient.getInstance().getModuleManager().getAndCast(Scaffold.class).isToggled()) {
                    MovementUtil.strafe();
                } else {
                    MovementUtil.strafe((double) devSpeed.getProperty());
                }
            }
            case "Custom Tick" -> {

                BlinkComponent blink = GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class);

                if (customTickType.is("Cycle")) {
                    if (!MovementUtil.isMoving()) {
                        mc.timer.timerSpeed = 1.0F;
                        customTickTickCounter = 0;
                        return;
                    }

                    int slowT = customTickSlowTicks.getProperty().intValue();
                    int boostT = customTickBoostTicks.getProperty().intValue();
                    int resetT = customTickResetTicks.getProperty().intValue();

                    int totalCycle = slowT + boostT + resetT;
                    if (totalCycle == 0) totalCycle = 1;

                    int currentTick = customTickTickCounter % totalCycle;

                    if (!blink.isBlinkingOutbound() && useBlink.getProperty()) {
                        blink.startBlinkingOutbound();
                    }

                    if (currentTick < slowT) {
                        mc.timer.timerSpeed = customTickSlowSpeed.getProperty();
                    } else if (currentTick < slowT + boostT) {
                        mc.timer.timerSpeed = customTickBoostSpeed.getProperty();
                    } else {
                        mc.timer.timerSpeed = 1.0f;
                    }

                    if (blink.isBlinkingOutbound() && useBlink.getProperty()) blink.stopBlinkingOutbound();

                    customTickTickCounter++;
                } else {
                    if (!MovementUtil.isMoving()) {
                        mc.timer.timerSpeed = 1.0F;
                        balanceCounter = 0;
                        releaseTickCounter = 0;
                        releasing = false;
                        return;
                    }

                    int ticks = mc.thePlayer.ticksExisted;

                    if (!releasing) {

                        if (!blink.isBlinkingOutbound() && useBlink.getProperty()) {
                            blink.startBlinkingOutbound();
                        }

                        if (ticks % ticksMod1.getProperty().intValue() == 0) {
                            mc.timer.timerSpeed = ticksSpeed1.getProperty();
                        } else if (ticks % ticksMod2.getProperty().intValue() == 0) {
                            mc.timer.timerSpeed = ticksSpeed2.getProperty();
                        } else if (ticks % ticksMod3.getProperty().intValue() == 0) {
                            mc.timer.timerSpeed = ticksSpeed3.getProperty();
                        } else {
                            mc.timer.timerSpeed = ticksSpeedDefault.getProperty();
                        }

                        balanceCounter++;

                        int threshold;
                        if (ticks % thresholdMod1.getProperty().intValue() == 0) {
                            threshold = thresholdValue1.getProperty().intValue();
                        } else if (ticks % thresholdMod2.getProperty().intValue() == 0) {
                            threshold = thresholdValue2.getProperty().intValue();
                        } else if (ticks % thresholdMod3.getProperty().intValue() == 0) {
                            threshold = thresholdValue3.getProperty().intValue();
                        } else {
                            threshold = thresholdDefault.getProperty().intValue();
                        }

                        if (balanceCounter >= threshold) {
                            releasing = true;
                            releaseTickCounter = 0;

                            if (useBlink.getProperty() && blinkMode.is("On Release")) {
                                if (blink.isBlinkingOutbound()) blink.stopBlinkingOutbound();
                            }
                        }
                    } else {
                        float releaseSpeed;

                        if (dynamicRelease.getProperty()) {
                            float progress = (float) releaseTickCounter / releaseTicks.getProperty().intValue();
                            releaseSpeed = dynamicReleaseSpeed.getProperty() * releaseMultiplier.getProperty() * (1.0f - progress);
                        }

                        else {
                            if (ticks % releaseTicksMod1.getProperty().intValue() == 0) {
                                releaseSpeed = releaseTicksSpeed1.getProperty();
                            } else if (ticks % releaseTicksMod2.getProperty().intValue() == 0) {
                                releaseSpeed = releaseTicksSpeed2.getProperty();
                            } else if (ticks % releaseTicksMod3.getProperty().intValue() == 0) {
                                releaseSpeed = releaseTicksSpeed3.getProperty();
                            } else {
                                releaseSpeed = releaseTicksSpeedDefault.getProperty();
                            }
                        }

                        mc.timer.timerSpeed = releaseSpeed;
                        releaseTickCounter++;

                        // End release phase after configured ticks
                        if (releaseTickCounter >= releaseTicks.getProperty().intValue()) {
                            if (useBlink.getProperty() && blinkMode.is("After Release")) {
                                if (blink.isBlinkingOutbound()) blink.stopBlinkingOutbound();
                            }
                            releasing = false;
                            balanceCounter = 0;
                            releaseTickCounter = 0;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0F;
        balanceCounter = 0;
        releaseTickCounter = 0;
        releasing = false;
        customTickTickCounter = 0;
        counter = 0;
        airTicks = 0;

        BlinkComponent blink = GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class);
        if (blink.isBlinkingOutbound()) blink.stopBlinkingOutbound();

        super.onDisable();
    }

    @Override
    public String getSuffix() {
        return speedMode.getProperty();
    }
}
