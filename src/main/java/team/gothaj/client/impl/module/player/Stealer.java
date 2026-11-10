package team.gothaj.client.impl.module.player;

import java.util.*;
import java.util.stream.Collectors;

import net.minecraft.block.*;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.*;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.property.properties.RangedNumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.game.WorldUtil;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.RotationEvent;
import team.gothaj.client.impl.event.player.WorldChangeEvent;
import team.gothaj.eventbus.Listen;

public class Stealer extends Module {


    private final BooleanProperty randomize = new BooleanProperty("Randomize", false);
    private final BooleanProperty whitelist = new BooleanProperty("Whitelist", true);

    private final BooleanProperty slotFlash = new BooleanProperty("Highlight", true);

    public static int flashSlot = -1;
    private static final long[] pulseTimes = new long[8];
    private static int pulseIndex = 0;
    private static int pulseCount = 0;
    private static final long FLASH_DURATION = 600L;
    
    private final BooleanProperty autoOpen = new BooleanProperty("Auto Open", true);
    private final BooleanProperty checkWalls = new BooleanProperty("Check Walls", true).setVisible(autoOpen::getProperty);
    
    private final BooleanProperty autoClose = new BooleanProperty("Auto Close", true);
    private final NumberProperty closeDelay = new NumberProperty("Close Delay", 50, 0, 1000, 25).setVisible(autoClose::getProperty);
    
    private final BooleanProperty chestName = new BooleanProperty("Check Chest Name", false);
    private final BooleanProperty disableOnWorldChange = new BooleanProperty("Disable on World Change", false);

    

    private final NumberProperty startDelay = new NumberProperty("Start Delay", 50, 0, 1000, 25);
    

    private final RangedNumberProperty delay = new RangedNumberProperty("Delay", 50, 500, 0, 1200, 25);

    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil startTimer = new TimerUtil();
    private final TimerUtil closeTimer = new TimerUtil();

    private int currentGrabTime;

    private TileEntity currentChest;

    private static final String[] BLACKLIST = new String[]{
            "mode", "menu", "selector", "game", "gui", "inventory", "play", "teleporter",
            "shop", "melee", "armor", "block", "castle", "mini", "warp", "teleport", "team",
            "tool", "trade", "cancel", "accept", "soul", "book", "recipe", "profile", "tele",
            "port", "map", "kit", "select", "lobby", "vault", "lock", "travel", "settings",
            "preference", "compass", "cake", "wars", "buy", "upgrade", "ranged", "potions",
            "utility", "arena", "hub", "queue", "rank", "ranked", "world", "event", "quest",
            "reward", "daily", "claim", "donate", "perks", "store", "credits", "coins",
            "points", "stats", "leaderboard", "spawn", "tutorial", "help", "vote", "ping",
            "npc", "parkour", "build", "creative", "survival", "adventure", "spectator",
            "region", "area", "setup", "join", "exit", "connect", "portal", "instance",
            "skin", "clan", "guild", "party", "chat", "message", "cooldown", "score",
            "objective", "economy", "bank", "market", "auction", "shopkeeper", "crate",
            "key", "login", "session", "timer", "round", "challenge", "delivery",
            "user", "server", "support", "restore", "core", "minigame"};

    private List<TileEntity> chestList = new ArrayList<>();


    public Stealer() {
        super("Stealer", "Takes items from chests.", ModuleCategory.PLAYER);
        registerProperties(startDelay, closeDelay, delay, randomize, checkWalls, whitelist, autoOpen, autoClose, chestName, disableOnWorldChange, slotFlash);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (!chestList.isEmpty()) chestList.clear();
        currentChest = null;
        flashSlot = -1;
        pulseCount = 0;
        pulseIndex = 0;
        super.onDisable();
    }

    public static float getFlashAlpha() {
        if (flashSlot < 0 || pulseCount == 0) return 0f;
        long now = System.currentTimeMillis();
        float max = 0f;
        for (int k = 0; k < pulseCount; k++) {
            long elapsed = now - pulseTimes[k];
            if (elapsed < 0 || elapsed > FLASH_DURATION) continue;
            float t = (float) elapsed / FLASH_DURATION;
            float fadeIn = Math.min(1f, t / 0.2f);
            float fadeOut = 1f - Math.max(0f, (t - 0.2f) / 0.8f);
            max = Math.max(max, fadeIn * fadeOut);
        }
        return max;
    }

    @Listen
    public void onClick(MouseClickEvent event) {
        if (mc.currentScreen instanceof GuiChest || !autoOpen.getProperty()) return;
        if (currentChest == null) return;

        boolean canClick = !checkWalls.getProperty()
                || (mc.objectMouseOver != null
                && (mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() instanceof BlockChest
                || mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() instanceof BlockEnderChest));

        if (!canClick) return;

        if (checkWalls.getProperty() && mc.objectMouseOver != null) {
            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec);
        } else {
            BlockPos pos = currentChest.getPos();
            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), pos, EnumFacing.UP, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        }

        WorldUtil.openedChests.add(currentChest.getPos());
        chestList.remove(currentChest);
        currentChest = null;
    }


    @Listen
    public void onRotation(RotationEvent event) {

        if (!autoOpen.getProperty()) return;

        double reach = mc.playerController.getBlockReachDistance() + 0.5;

        chestList = mc.theWorld.loadedTileEntityList.stream()
                .filter(te -> te instanceof TileEntityChest || te instanceof TileEntityEnderChest)
                .filter(te -> !WorldUtil.openedChests.contains(te.getPos()))
                .filter(te -> te.getPos().distanceSq(mc.thePlayer.getPosition()) <= reach * reach)
                .filter(te -> !checkWalls.getProperty() || RotationUtil.canSeeBlock(te))
                .sorted(Comparator.comparingDouble(te -> mc.thePlayer.getDistanceSq(te.getPos())))
                .collect(Collectors.toCollection(ArrayList::new));

        if (chestList.isEmpty()) {
            return;
        }

        currentChest = chestList.getFirst();

        float[] rotations = RotationUtil.getRotationsFromPositionToPosition(
                mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ,
                currentChest.getPos().getX() + 0.5,
                currentChest.getPos().getY() + 0.5,
                currentChest.getPos().getZ() + 0.5);

        RotationComponent.setRotations(rotations[0], rotations[1], 180, 180, true);
    }


    @Listen
    public void onMotion(LivingUpdateEvent e) {
        if (!(mc.thePlayer.openContainer instanceof ContainerChest container)) {
            currentGrabTime = getDelay();
            startTimer.reset();
            return;
        }
        
        if(mc.thePlayer.isSprinting() || mc.thePlayer.serverSprintState) {
        	mc.thePlayer.setSprinting(false);
        }
        if(mc.thePlayer.serverSprintState)return;

        if (chestName.getProperty()) {
            String name = container.getLowerChestInventory().getDisplayName().getUnformattedText().toLowerCase();
            for (String str : BLACKLIST) {
                if (name.contains(str)) return;
            }
        }

        if (!startTimer.hasTimeReached(startDelay.getProperty().longValue())) return;

        if (timer.hasTimeReached(currentGrabTime)) {
            int size = container.getLowerChestInventory().getSizeInventory();
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < size; i++) slots.add(i);
            if (randomize.getProperty()) Collections.shuffle(slots);

            for (int i : slots) {
                ItemStack stack = container.getLowerChestInventory().getStackInSlot(i);
                if (stack == null) continue;
                if (whitelist.getProperty() && !InventoryUtil.itemWhitelisted(stack)) continue;
                if (slotFlash.getProperty()) {
                    flashSlot = i;
                    pulseTimes[pulseIndex] = System.currentTimeMillis();
                    pulseIndex = (pulseIndex + 1) % pulseTimes.length;
                    if (pulseCount < pulseTimes.length) pulseCount++;
                }
                mc.playerController.windowClick(container.windowId, i, 0, 1, mc.thePlayer);
                currentGrabTime = getDelay();
                timer.reset();
                if (currentGrabTime > 0) {
                    break;
                }
            }
        }

        if (autoClose.getProperty()) {
            if (InventoryUtil.isInventoryFull() || InventoryUtil.isInventoryEmpty(container.getLowerChestInventory())) {
                if (closeTimer.hasTimeReached(closeDelay.getProperty().longValue())) {
                    mc.thePlayer.closeScreen();
                }
            } else {
                closeTimer.reset();
            }
        }
    }

    @Listen
    public void onWorld(WorldChangeEvent e) {
        if (disableOnWorldChange.getProperty()) {
            this.toggle();
        }

        chestList.clear();
        WorldUtil.openedChests.clear();
        currentChest = null;
    }

    private int getDelay() {
        int min = (int) delay.getLeft();
        int max = (int) delay.getRight();
        return MathUtil.getRandomNumber(min, Math.max(min, max));
    }



}
