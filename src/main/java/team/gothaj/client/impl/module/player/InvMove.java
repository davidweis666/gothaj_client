package team.gothaj.client.impl.module.player;

import net.minecraft.block.BlockContainer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.movement.AutoSprint;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//TODO:: Fix instant release in polar mode
public class InvMove extends Module {

    private final ModeProperty mode = new ModeProperty("Mode", "Vanilla", "Vanilla", "Watchdog", "Polar");
    private final BooleanProperty instantRelease = new BooleanProperty("Instant Release", false).setVisible(() -> !mode.is("Vanilla"));
    private final BooleanProperty watchdogAdaptive = new BooleanProperty("Watchdog Adaptive", true)
            .setVisible(() -> mode.is("Watchdog"));
    private final BooleanProperty watchdogShopkeeperMove = new BooleanProperty("Watchdog Shopkeeper Move", true)
            .setVisible(() -> mode.is("Watchdog") && watchdogAdaptive.getProperty());

    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil releaseTimer = new TimerUtil();
    private final TimerUtil sprintDelayTimer = new TimerUtil();

    private boolean waitingForSprint = false;
    private C0DPacketCloseWindow pendingClosePacket = null;
    private C0DPacketCloseWindow deferredPolarClosePacket = null;
    private int serverClosedWindowId = -1;
    private boolean sentOpenPacket = false;
    private static final long INSTANT_RELEASE_DELAY_MS = 50L;

    private final List<PacketUtil.DelayedPacket> delayedInstantPackets = new ArrayList<>();

    private final List<Packet<?>> clickPackets = new ArrayList<>();
    private final ArrayDeque<Packet<?>> watchdogPacketDeque = new ArrayDeque<>();
    private int watchdogBlinklessTapError;
    private boolean watchdogBlinking;
    public boolean isInvOpenServerSide;

    public InvMove() {
        super("InvMove", "", ModuleCategory.PLAYER);
        registerProperties(mode, instantRelease, watchdogAdaptive, watchdogShopkeeperMove);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    public boolean shouldBlockAdaptiveMovement() {
        return isToggled() && isWatchdogBlinkShopkeeperContainer();
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (mc.thePlayer.isSpectator()) return;
        trackServerSideInventory(event);
        clearWatchdogBlinkOnServerReset(event);

        if (mode.is("Watchdog")) {
            handleWatchdogBlinkPacket(event);
            return;
        }

        if (!mode.is("Vanilla") && (mc.currentScreen instanceof GuiContainer) && MovementUtil.isMoving()) {
            if (event.getDirection() == PacketEvent.Direction.SEND) {
                if (event.getPacket() instanceof C0DPacketCloseWindow packet) {
                    if (instantRelease.getProperty()) {
                        if (mode.is("Polar")) {
                            event.setCancelled(true);
                            deferredPolarClosePacket = packet;
                            return;
                        }

                        sendInstantPacket(packet, event);
                        return;
                    }

                    if (mode.is("Watchdog")) {
                        pendingClosePacket = packet;
                        releaseTimer.reset();
                        sprintDelayTimer.reset();
                        waitingForSprint = true;
                        event.setCancelled(true);
                    }
                }
                if (event.getPacket() instanceof C0EPacketClickWindow window) {
                    if (mc.currentScreen instanceof GuiChest
                            && !(((GuiChest) mc.currentScreen).lowerChestInventory.getDisplayName()
                            .getUnformattedText().contains("chest"))) {
                        return;
                    }

                    if (instantRelease.getProperty()) {
                        sendInstantPacket(window, event);
                        return;
                    }

                    if (window.getMode() == 0 && window.getUsedButton() == 0) {
                        if (!clickPackets.isEmpty()) {
                            Packet<?> lastP = clickPackets.getLast();

                            if (lastP instanceof C0EPacketClickWindow last) {
                                if (last.getWindowId() == window.getWindowId() && last.getMode() == 0 && last.getUsedButton() == 0 && last.getSlotId() == window.getSlotId()) {
                                    clickPackets.removeLast();
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                    }

                    clickPackets.add(window);
                    event.setCancelled(true);
                }


                if (event.getPacket() instanceof C09PacketHeldItemChange) {
                    if (instantRelease.getProperty()) {
                        sendInstantPacket(event.getPacket(), event);
                        return;
                    }

                    if (!clickPackets.isEmpty()) {

                        if (clickPackets.getLast() instanceof C09PacketHeldItemChange) {
                            clickPackets.removeLast();
                        }

                        clickPackets.add(event.getPacket());
                        event.setCancelled(true);
                    }
                }
            }
            if (mode.is("Polar")) {
                if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
                    if (event.getPacket() instanceof S2EPacketCloseWindow packet) {

                        serverClosedWindowId = packet.getWindowId();
                        releaseTimer.reset();
                        sprintDelayTimer.reset();
                        waitingForSprint = true;
                        event.setCancelled(true);
                        sentOpenPacket = false;
                    }
                }
            }
        }
    }

    @Listen
    public void onTick(TickEvent event) {
        if (mc.thePlayer.isSpectator()) return;

        if (mode.is("Watchdog")) {
            handleWatchdogBlinkTick();
            return;
        }
        
        if (!mode.is("Vanilla") && !instantRelease.getProperty() && (serverClosedWindowId != -1 || pendingClosePacket != null) && (mc.currentScreen == null && !InventoryUtil.isInventoryOpen)) {
            mc.thePlayer.setSprinting(false);
            AutoSprint.allowSprint = false;

            if ((releaseTimer.hasTimeReached(100) && !mc.thePlayer.isSprinting()) || releaseTimer.hasTimeReached(500)) {
                if (!clickPackets.isEmpty()) {
                    if (serverClosedWindowId != -1 && mode.is("Polar") && !sentOpenPacket) {
                        PacketUtil.sendSilent(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                        sentOpenPacket = true;
                    }

                    for (Packet<?> p : clickPackets) {
                        PacketUtil.sendSilent(p);
                    }

                    clickPackets.clear();
                } else {
                    if (timer.hasTimeReached(270) && clickPackets.isEmpty()) {
                        if (mode.is("Polar") && serverClosedWindowId != -1) {
                            PacketUtil.sendSilent(new C0DPacketCloseWindow(serverClosedWindowId));
                            serverClosedWindowId = -1;
                            sentOpenPacket = false;
                        }
                        if (mode.is("Watchdog") && pendingClosePacket != null) {
                            PacketUtil.sendSilent(pendingClosePacket);
                            pendingClosePacket = null;
                        }
                        timer.reset();
                    }
                }
            }
            return;
        }

        if (!mode.is("Vanilla") && waitingForSprint) {
            mc.thePlayer.setSprinting(false);
            AutoSprint.allowSprint = false;

            if (sprintDelayTimer.hasTimeReached(10)) {
                waitingForSprint = false;
                AutoSprint.allowSprint = true;
                sprintDelayTimer.reset();
            }
        }

        if (isPolarInstantCarryingItem()) {
            mc.thePlayer.setSprinting(false);
            AutoSprint.allowSprint = false;
        }

        flushDelayedInstantPackets();
        flushDeferredPolarClose();

        if (mc.currentScreen == null && !InventoryUtil.isInventoryOpen) {
            AutoSprint.allowSprint = true;
            return;
        }

        if (mc.currentScreen instanceof GuiChat) return;

        setKey(mc.gameSettings.keyBindForward);
        setKey(mc.gameSettings.keyBindBack);
        setKey(mc.gameSettings.keyBindRight);
        setKey(mc.gameSettings.keyBindLeft);
        setKey(mc.gameSettings.keyBindJump);
    }

    private void handleWatchdogBlinkTick() {
        if (mc.currentScreen == null) {
            watchdogBlinking = false;
            flushWatchdogPackets();
            AutoSprint.allowSprint = true;
            return;
        }

        if (!canUseWatchdogBlink()) {
            setWatchdogBlinkMovementKeys(false);
            return;
        }

        if (isWatchdogBlinkShopkeeperContainer()) {
            handleWatchdogBlinklessTick();
            return;
        }

        if (!(mc.currentScreen instanceof GuiContainer)) {
            return;
        }

        setWatchdogBlinkMovementKeys(true);
    }

    private void handleWatchdogBlinkPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (mc.currentScreen == null) {
            watchdogBlinking = false;
            flushWatchdogPackets();
            return;
        }

        if (!canUseWatchdogBlink()) {
            return;
        }

        if (isWatchdogBlinkShopkeeperContainer()) {
            watchdogBlinking = false;
            flushWatchdogPackets();
            handleWatchdogBlinklessPacket(packet);
            return;
        }

        if (packet instanceof C0EPacketClickWindow click) {
            if (click.getWindowId() == mc.thePlayer.inventoryContainer.windowId && isWatchdogBlinkAllowedClick(click)) {
                PacketUtil.sendSilent(new C0DPacketCloseWindow(click.getWindowId()));
            } else {
                watchdogBlinking = true;
            }
        } else if (packet instanceof C0DPacketCloseWindow) {
            watchdogBlinking = false;
        } else if (watchdogBlinking && !isWatchdogBlinkPassThroughPacket(packet)) {
            watchdogPacketDeque.add(packet);
            event.setCancelled(true);
        }

    }

    private void handleWatchdogBlinklessPacket(Packet<?> packet) {
        if (!(packet instanceof C08PacketPlayerBlockPlacement placement) || mc.theWorld == null) {
            return;
        }

        if (mc.theWorld.getBlockState(placement.getPosition()).getBlock() instanceof BlockContainer) {
            watchdogBlinklessTapError = 0;
        }
    }

    private void handleWatchdogBlinklessTick() {
        if (mc.currentScreen instanceof GuiInventory && !mc.thePlayer.isUsingItem()) {
            switch (mc.thePlayer.ticksExisted % 4) {
                case 0:
                    PacketUtil.sendSilent(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                    break;
                case 1:
                    PacketUtil.sendSilent(new C0DPacketCloseWindow());
                    break;
            }
        }

        if (mc.currentScreen instanceof GuiChest) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }

        if (mc.currentScreen instanceof GuiChest && isWatchdogBlinkShopkeeperContainer()) {
            watchdogBlinklessTapError += 16;
            if (watchdogBlinklessTapError >= 20) {
                watchdogBlinklessTapError -= 20;
                setWatchdogBlinkMovementKeys(false);
            } else {
                setWatchdogBlinkMovementKeys(true);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                AutoSprint.allowSprint = false;
            }
        }
    }

    private void setWatchdogBlinkMovementKeys(boolean enabled) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindForward));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindBack));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindLeft));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindRight));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), enabled && GameSettings.isKeyDown(mc.gameSettings.keyBindSprint));
        AutoSprint.allowSprint = enabled;
    }

    private boolean shouldWatchdogBlinkAdaptiveBlock() {
        return isWatchdogBlinkShopkeeperContainer();
    }

    private boolean isWatchdogBlinkShopkeeperContainer() {
        return mode.is("Watchdog")
                && watchdogShopkeeperMove.getProperty()
                && watchdogAdaptive.getProperty()
                && isBedWars()
                && mc.currentScreen instanceof GuiChest
                && isShopkeeperChestScreen();
    }

    private boolean canUseWatchdogBlink() {
        KillAura killAura = getCastedModule(KillAura.class);
        Scaffold scaffold = getCastedModule(Scaffold.class);
        return mc.thePlayer != null
                && mc.theWorld != null
                && mc.currentScreen instanceof GuiContainer
                && !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isBlocking()
                && (!killAura.isToggled() || KillAura.target == null)
                && !mc.isSingleplayer()
                && (scaffold == null || !scaffold.isToggled());
    }

    private boolean isShopkeeperChestScreen() {
        if (!(mc.currentScreen instanceof GuiChest chest)) {
            return false;
        }

        String name = chest.lowerChestInventory.getDisplayName().getUnformattedText().toLowerCase();
        if (name.contains("ender chest")) {
            return false;
        }

        String[] shopNames = {
                "shop", "quick buy", "buy", "upgrade", "melee", "armor", "blocks", "tools",
                "ranged", "potions", "utility", "traps", "trap", "team", "heal pool",
                "dragon buff", "miner fatigue", "reinforced", "sharpened", "forge"
        };

        for (String shopName : shopNames) {
            if (name.contains(shopName)) {
                return true;
            }
        }

        return !name.contains("chest");
    }

    private boolean isWatchdogBlinkAllowedClick(C0EPacketClickWindow packet) {
        int clickMode = packet.getMode();
        return clickMode == 1 || clickMode == 2 || clickMode == 4;
    }

    private boolean isWatchdogBlinkPassThroughPacket(Packet<?> packet) {
        return packet instanceof C00PacketKeepAlive;
    }

    private void flushWatchdogPackets() {
        while (!watchdogPacketDeque.isEmpty()) {
            PacketUtil.sendSilent(watchdogPacketDeque.pollFirst());
        }
    }

    private void trackServerSideInventory(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (event.getDirection() == PacketEvent.Direction.SEND) {
            if (packet instanceof C16PacketClientStatus status
                    && status.getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                isInvOpenServerSide = true;
            } else if (packet instanceof C0DPacketCloseWindow) {
                isInvOpenServerSide = false;
            }
        } else {
            if (packet instanceof S01PacketJoinGame) {
                isInvOpenServerSide = false;
            } else if (packet instanceof S2DPacketOpenWindow) {
                isInvOpenServerSide = true;
            } else if (packet instanceof S2EPacketCloseWindow) {
                isInvOpenServerSide = false;
            }
        }
    }

    private void clearWatchdogBlinkOnServerReset(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.RECEIVE || mode.is("Vanilla")) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof S01PacketJoinGame || packet instanceof S07PacketRespawn
                || packet instanceof S08PacketPlayerPosLook) {
            watchdogPacketDeque.clear();
            watchdogBlinklessTapError = 0;
            watchdogBlinking = false;
        }
    }

    private boolean isBedWars() {
        if (mc.theWorld == null) {
            return false;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return false;
        }

        String title = objective.getDisplayName();
        return title != null && title.toUpperCase().contains("BED WARS");
    }

    private void setKey(KeyBinding key){
        key.pressed = GameSettings.isKeyDown(key);
    }

    private void sendInstantPacket(Packet<?> packet, PacketEvent event) {
        event.setCancelled(true);

        mc.thePlayer.setSprinting(false);
        AutoSprint.allowSprint = false;
        waitingForSprint = true;
        sprintDelayTimer.reset();

        delayedInstantPackets.add(new PacketUtil.DelayedPacket(packet, System.currentTimeMillis() + INSTANT_RELEASE_DELAY_MS));
    }

    private void flushDelayedInstantPackets() {
        if (delayedInstantPackets.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<PacketUtil.DelayedPacket> iterator = delayedInstantPackets.iterator();

        while (iterator.hasNext()) {
            PacketUtil.DelayedPacket delayed = iterator.next();
            if (now < delayed.delay()) continue;

            sendPolarOpenIfNeeded();
            PacketUtil.sendSilent(delayed.packet());
            iterator.remove();
        }
    }

    private void flushDeferredPolarClose() {
        if (deferredPolarClosePacket == null || !mode.is("Polar") || !instantRelease.getProperty()) return;

        if (!delayedInstantPackets.isEmpty()) {
            return;
        }

        if (mc.thePlayer.inventory.getItemStack() != null) {
            return;
        }

        if ((mc.currentScreen instanceof GuiContainer)) {
            return;
        }

        PacketUtil.sendSilent(deferredPolarClosePacket);
        deferredPolarClosePacket = null;
        sentOpenPacket = false;
    }

    private boolean isPolarInstantCarryingItem() {
        return mode.is("Polar")
                && instantRelease.getProperty()
                && (mc.currentScreen instanceof GuiContainer)
                && mc.thePlayer.inventory.getItemStack() != null;
    }

    private void sendPolarOpenIfNeeded() {
        if (serverClosedWindowId != -1 && mode.is("Polar") && !sentOpenPacket) {
            PacketUtil.sendSilent(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            sentOpenPacket = true;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        flushWatchdogPackets();
        clickPackets.clear();
        delayedInstantPackets.clear();
        watchdogPacketDeque.clear();
        pendingClosePacket = null;
        deferredPolarClosePacket = null;
        serverClosedWindowId = -1;
        waitingForSprint = false;
        sentOpenPacket = false;
        watchdogBlinklessTapError = 0;
        watchdogBlinking = false;
        isInvOpenServerSide = false;
        AutoSprint.allowSprint = true;
        super.onDisable();
    }
}
