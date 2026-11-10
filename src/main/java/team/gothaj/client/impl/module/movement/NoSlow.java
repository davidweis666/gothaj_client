package team.gothaj.client.impl.module.movement;

import net.minecraft.item.*;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.component.BlinkComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.player.MotionEvent;
import team.gothaj.client.impl.event.player.PostMotionEvent;
import team.gothaj.client.impl.event.player.SlowDownEvent;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author Kawase
 * @since 28.09.2025
 */
public class NoSlow extends Module {
    private final NumberProperty swordSlowDown = new NumberProperty("Sword Slow Down", 0.2f, 0.2f, 1, 0.1f),
            foodSlowDown = new NumberProperty("Food Slow Down", 0.2f, 0.2f, 1, 0.1f),
            bowSlowDown = new NumberProperty("Bow Slow Down", 0.2f, 0.2f, 1, 0.1f),
            potionSlowDown = new NumberProperty("Potion Slow Down", 0.2f, 0.2f, 1, 0.1f);

    public BooleanProperty sprint = new BooleanProperty("Sprint", true);
    public ModeProperty mode = new ModeProperty("Mode", "Normal", "Normal", "Watchdog");
    private final BooleanProperty delayRelease = new BooleanProperty("Delay Release", true).setVisible(() -> mode.is("Watchdog"));

    private boolean pendingRightClick;
    private boolean rightClickPending;
    private boolean blinkBufferEngaged;
    private boolean blinkActive;
    private boolean releaseScheduled;


    public NoSlow() {
        super("NoSlow", "Removes Slow Down", ModuleCategory.MOVEMENT);
        this.registerProperties(swordSlowDown, foodSlowDown, bowSlowDown, potionSlowDown, sprint, mode, delayRelease);
    }

    @Override
    public void onDisable() {
        teardown();
        super.onDisable();
    }

    @Listen
    public void onSlowDown(SlowDownEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return;
        if (!mc.thePlayer.isUsingItem() || !MovementUtil.isMoving()) return;

        event.setShouldSprint(sprint.getProperty());

        if (mode.is("Watchdog")) {
            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                event.setCancelled(true);
                event.setSlowDown(1f);
                return;
            }
        }

        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
            event.setSlowDown(foodSlowDown.getProperty());
        } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            event.setSlowDown(swordSlowDown.getProperty());
        } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            event.setSlowDown(bowSlowDown.getProperty());
        } else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion) {
            event.setSlowDown(potionSlowDown.getProperty());
        }
    }

    @Listen
    public void onMotion(MotionEvent event) {
        if (!mode.is("Watchdog")) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        if (!canRelease()) {
            teardown();
            return;
        }

        if (pendingRightClick) {
            releaseScheduled = true;
            return;
        }

        releaseScheduled = false;
        getBlink().startBlinkingInbound();
        getBlink().startBlinkingOutbound();
        blinkActive = true;
        sendRelease();
        rightClickPending = false;
        blinkBufferEngaged = true;
    }

    @Listen
    public void onPostMotion(PostMotionEvent event) {
        if (!mode.is("Watchdog")) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        if (!canRelease()) {
            teardown();
            return;
        }

        if (pendingRightClick) {
            if (!delayRelease.getProperty()) {
                getBlink().stopBlinkingInbound();
                getBlink().stopBlinkingOutbound();
                blinkActive = false;
                sendBlock();
                rightClickPending = true;
                pendingRightClick = false;
            } else {
                releaseScheduled = true;
            }
        } else if (blinkBufferEngaged) {
            pendingRightClick = true;
            blinkBufferEngaged = false;
        }
    }

    @Listen
    public void onTick(TickEvent event) {
        if (!mode.is("Watchdog")) return;
        if (!delayRelease.getProperty() || !releaseScheduled) return;
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        sendBlock();
        rightClickPending = true;
        getBlink().stopBlinkingInbound();
        getBlink().stopBlinkingOutbound();
        blinkActive = false;
        pendingRightClick = false;
        releaseScheduled = false;
    }

    private boolean canRelease() {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return false;
        if (!(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) return false;
        return mc.thePlayer.isUsingItem() || mc.gameSettings.keyBindUseItem.pressed;
    }

    private void sendRelease() {
        PacketUtil.send(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
    }

    private void sendBlock() {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return;
        PacketUtil.send(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
    }

    private void teardown() {
        releaseScheduled = false;
        if (blinkActive) {
            getBlink().stopBlinkingInbound();
            getBlink().stopBlinkingOutbound();
            blinkActive = false;
        }
        if (rightClickPending) {
            sendRelease();
            rightClickPending = false;
        }
        pendingRightClick = false;
        blinkBufferEngaged = false;
    }

    private BlinkComponent getBlink() {
        return GothajClient.getInstance().getComponentManager().getAndCast(BlinkComponent.class);
    }
}
