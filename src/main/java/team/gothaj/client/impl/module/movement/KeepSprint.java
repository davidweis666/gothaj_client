package team.gothaj.client.impl.module.movement;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.impl.event.game.PostTickEvent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.AttackEntityEvent;
import team.gothaj.client.impl.event.player.AttackMotionEvent;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class KeepSprint extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Normal", "Normal", "WatchDog2");

    // Normal (vanilla) properties
    public final BooleanProperty sprint = new BooleanProperty("Sprint", false)
            .setVisible(() -> mode.is("Normal"));

    public final BooleanProperty hurtCheck = new BooleanProperty("Hurt Check", true)
            .setVisible(() -> mode.is("Normal"));

    public final NumberProperty sprintSpeed = new NumberProperty("Slowdown", 0.6f, 0.1F, 1.0F, 0.1F)
            .setVisible(() -> mode.is("Normal"));

    // WatchDog2 properties
    private final BooleanProperty onlyJumpBoost = new BooleanProperty("Only Jump Boost", false)
            .setVisible(() -> mode.is("WatchDog2"));

    private final BooleanProperty airOnly = new BooleanProperty("Air Only", true)
            .setVisible(() -> mode.is("WatchDog2"));

    private final BooleanProperty disableAir = new BooleanProperty("Disable Air Cancel", false)
            .setVisible(() -> mode.is("WatchDog2"));

    private final NumberProperty suppressTicks = new NumberProperty("Suppress Ticks", 0, 0, 5, 1)
            .setVisible(() -> mode.is("WatchDog2"));

    private final BooleanProperty debug = new BooleanProperty("Debug", false)
            .setVisible(() -> mode.is("WatchDog2"));

    // WatchDog2 state
    private boolean suppressArmed = false;
    private boolean canKeep = false;
    private int suppressTickCounter = 0;

    public static boolean sprintJumped = false;

    public KeepSprint() {
        super("KeepSprint", "", ModuleCategory.MOVEMENT);
        registerProperties(mode, sprint, sprintSpeed, hurtCheck, onlyJumpBoost, airOnly, disableAir, suppressTicks,
                debug);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetWatchdogState();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        boolean restore = sprintJumped;
        resetWatchdogState();

        if (mc.thePlayer != null && mc.theWorld != null && restore) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(),
                    isKeyReallyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
        }
    }

    private void resetWatchdogState() {
        sprintJumped = false;
        suppressArmed = false;
        canKeep = false;
        suppressTickCounter = 0;
    }

    // ===================== NORMAL (VANILLA) MODE =====================

    @Listen
    public void onAttackMotion(AttackMotionEvent event) {
        if (!mode.is("Normal")) {
            return;
        }

        if (hurtCheck.getProperty() && mc.thePlayer.hurtTime != 0) {
            event.setSprint(false);
            event.setMultiplier(0.6);
            return;
        }
        event.setSprint(sprint.getProperty());
        event.setMultiplier(sprintSpeed.getProperty());
    }

    // ===================== WATCHDOG2 MODE =====================

    @Listen
    public void onTick(TickEvent event) {
        if (!mode.is("WatchDog2") || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        canKeep = mc.thePlayer.isSprinting();
        suppressArmed = computeSuppress();

        if (suppressTickCounter > 0) {
            suppressTickCounter--;
        }

        if (!sprintJumped && suppressTickCounter == 0) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
    }

    @Listen
    public void onPostTick(PostTickEvent event) {
        if (!mode.is("WatchDog2") || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (sprintJumped && mc.thePlayer.onGround && suppressTickCounter == 0) {
            sprintJumped = false;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(),
                    isKeyReallyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
            debugMessage("landed, sprint restored");
        }
    }

    @Listen
    public void onAttackEntity(AttackEntityEvent event) {
        if (!mode.is("WatchDog2") || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        boolean suppress = computeSuppress();
        suppressArmed = suppress;
        if (!suppress) {
            return;
        }

        event.setCancelled(true);

        boolean airHit = canKeep && !mc.thePlayer.onGround;

        sprintJumped = true;
        suppressTickCounter = suppressTicks.getProperty().intValue();
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        mc.thePlayer.sprintingTicksLeft = 1;

        debugMessage(airHit ? "air hit suppressed, sprint reset" : "ground hit suppressed");
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (!mode.is("WatchDog2") || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (event.isCancelled() || event.getDirection() != PacketEvent.Direction.SEND) {
            return;
        }
        if (!(event.getPacket() instanceof C02PacketUseEntity packet)
                || packet.getAction() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }

        Entity target;
        try {
            target = packet.getEntityFromWorld(mc.theWorld);
        } catch (Exception ignored) {
            return;
        }

        if (!(target instanceof EntityPlayer)) {
            return;
        }

        if (suppressArmed) {
            event.setCancelled(true);
            debugMessage("C02 dropped");
        }
    }

    private boolean computeSuppress() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        if (onlyJumpBoost.getProperty() && !mc.thePlayer.isPotionActive(Potion.jump)) {
            return false;
        }

        // Air hits are suppressed while sprinting, unless explicitly disabled
        if (!disableAir.getProperty() && canKeep && !mc.thePlayer.onGround) {
            return true;
        }

        // Ground hits are only suppressed when Air Only is off
        return !airOnly.getProperty()
                && mc.thePlayer.isSprinting()
                && Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())
                && mc.thePlayer.onGround;
    }

    // ===================== SHARED HELPERS =====================

    public boolean isBlockingSprint() {
        return isToggled() && mode.is("WatchDog2") && sprintJumped;
    }

    private static boolean isKeyReallyDown(int code) {
        try {
            if (code < 0) {
                return Mouse.isButtonDown(code + 100);
            }
            return Keyboard.isKeyDown(code);
        } catch (Exception e) {
            return false;
        }
    }

    private void debugMessage(String message) {
        if (debug.getProperty() && mc.thePlayer != null) {
            Logger.debugChat(this, message);
        }
    }
}
