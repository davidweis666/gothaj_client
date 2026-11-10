package team.gothaj.client.impl.module.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.*;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.notification.Notification;
import team.gothaj.client.api.property.properties.*;
import team.gothaj.client.api.util.game.WorldUtil;
import team.gothaj.client.api.util.other.ColorUtil;
import team.gothaj.client.api.util.render.RenderUtil;
import team.gothaj.client.api.util.rotation.RotationUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.game.TickEvent;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.network.PacketEvent;
import team.gothaj.client.impl.event.player.item.SpoofClientItemEvent;
import team.gothaj.client.impl.event.render.RenderDimensionEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.misc.AutoTool;
import team.gothaj.client.impl.module.visual.Hud;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

import java.awt.*;
import java.util.*;

@SuppressWarnings("all")
public class Breaker extends Module {

    private BreakState breakState = BreakState.IDLE;
    public static BlockPos targetBlockPos;
    private Vec3 ownPos;

    private final Map<BlockPos, BreakTier> tierCache = new HashMap<>();
    private int cacheTicks = 0;

    private java.util.List<BlockPos> breakPath = new java.util.ArrayList<>();
    private int pathTicks = 0;

    private BlockPos breakingSurroundingBlock = null;
    private boolean hasBrokenSurroundBlock = false;
    private long breakStartTime = 0;
    private BlockPos lastBreakPos = null;

    private int lastSlot = -1;
    private boolean swap;

    private final ModeProperty mode = new ModeProperty("Target Mode", "Bed", "Bed", "Egg", "Custom ID");

    private final InputProperty customMode = new InputProperty("Block ID", "1").setVisible(() -> mode.getProperty().equals("Custom ID"));
    private final NumberProperty range = new NumberProperty("Range", 6, 1, 6, 0.1f);
    private final BooleanProperty protectOwn = new BooleanProperty("Protect Own Pos", true);
    private final ActionProperty resetOwnPos = new ActionProperty("Reset Own Pos").setListener(() -> {
        if (ownPos != null) {
            ownPos = null;
            GothajClient.getInstance().getNotificationManager().add("Breaker", "Home position reset!", Notification.Type.INFO);
        }
    }).setVisible(protectOwn::getProperty);
    private final BooleanProperty showBlockTier = new BooleanProperty("Show Block Tier", true);
    private final BooleanProperty showCurrentBlock = new BooleanProperty("Only Current", true).setVisible(showBlockTier::getProperty);
    private final ModeProperty breakMode = new ModeProperty("Break Mode", "Full Block", "Full Block", "First Block", "Through Walls");

    public Breaker() {
        super("Breaker", "", ModuleCategory.WORLD);
        registerProperties(mode, customMode, range, protectOwn, resetOwnPos, showBlockTier, showCurrentBlock, breakMode);
    }

    @Override
    public String getSuffix() {
        return mode.is("Custom ID") ? String.valueOf(Block.getBlockById(Integer.parseInt(customMode.getString())).getLocalizedName()) : mode.getProperty();
    }

    @Listen
    public void onSpoofItem(SpoofClientItemEvent event) {
        if (KillAura.target != null) {
            return;
        }
        if (mc.thePlayer.getHeldItem() != null && (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) return;
        if (getCastedModule(AutoTool.class).isToggled() && getCastedModule(AutoTool.class).spoof.getProperty() && swap && lastSlot != -1) {
            event.setItem(mc.thePlayer.inventory.getStackInSlot(lastSlot));
        }
    }

    private Block getTargetBlockType() {
        switch (mode.getProperty()) {
            case "Bed": return Blocks.bed;
            case "Egg": return Blocks.dragon_egg;
            case "Custom ID":
                try {
                    int id = Integer.parseInt(customMode.getString());
                    return Block.getBlockById(id);
                } catch (Exception e) {
                    return Blocks.bed;
                }
            default: return Blocks.bed;
        }
    }

    @Listen
    public void onTick(TickEvent event) {
        if (KillAura.target != null) {
            resetBreaking();
            return;
        }

        Block targetType = getTargetBlockType();
        BlockPos newPos = WorldUtil.findBlock(targetType, range.getProperty());

        if (newPos != null && (targetBlockPos == null || !newPos.equals(targetBlockPos))) {
            hasBrokenSurroundBlock = false;
            breakingSurroundingBlock = null;
            breakState = BreakState.IDLE;
            breakPath.clear();
        }

        targetBlockPos = newPos;

        if (++cacheTicks >= 20) {
            tierCache.clear();
            cacheTicks = 0;
        }

        if (ownPos != null && targetBlockPos != null && protectOwn.getProperty()) {
            double distSq = mc.thePlayer.getDistanceSq(ownPos.xCoord, ownPos.yCoord, ownPos.zCoord);
            if (distSq < 40 * 40) {
                targetBlockPos = null;
                resetBreaking();
                return;
            }
        }

        if (targetBlockPos == null) {
            resetBreaking();
            return;
        }

        if (!breakMode.is("Through Walls")) {
            Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
            Vec3 bedCenter = new Vec3(targetBlockPos.getX() + 0.5, targetBlockPos.getY() + 0.5, targetBlockPos.getZ() + 0.5);
            boolean bedVisible = mc.theWorld.rayTraceBlocks(eyes, bedCenter) == null;
            double distanceToBed = mc.thePlayer.getDistance(targetBlockPos.getX() + 0.5, targetBlockPos.getY() + 0.5, targetBlockPos.getZ() + 0.5);
            float blockReach = mc.playerController.getBlockReachDistance();

            if (bedVisible && distanceToBed > blockReach) {
                targetBlockPos = null;
                resetBreaking();
            }
        }
    }

    @Listen
    public void onLegitClick(MouseClickEvent event) {
        if (KillAura.target != null) {
            resetBreaking();
            return;
        }

        if (targetBlockPos == null) {
            resetBreaking();
            return;
        }

        IBlockState state = mc.theWorld.getBlockState(targetBlockPos);
        Block block = state.getBlock();
        Block targetType = getTargetBlockType();

        if (block != targetType) return;

        switch (breakMode.getProperty()) {
            case "Through Walls" -> handleThroughWallsMode();
            case "First Block" -> handleFirstBlockMode();
            default -> handleFullBlockMode();
        }
    }

    private BlockPos nextBreakTarget() {
        breakPath = WorldUtil.getLineOfSightPath(targetBlockPos);

        breakPath.removeIf(pos -> {
            Block b = mc.theWorld.getBlockState(pos).getBlock();
            return b instanceof BlockAir || b.getMaterial() == Material.air;
        });

        float blockReach = mc.playerController.getBlockReachDistance();

        for (BlockPos pos : breakPath) {
            if (withinReach(pos, blockReach)) {
                return pos;
            }
        }

        return null;
    }

    private boolean withinReach(BlockPos pos, float blockReach) {
        double dist = mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return dist <= blockReach;
    }

    private void attackTrackedBlock(BlockPos pos) {
        if (pos == null) return;

        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();

        if (block == null || block instanceof BlockAir || block.getMaterial() == Material.air) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), pos, -1);
            if (pos.equals(breakingSurroundingBlock)) {
                breakingSurroundingBlock = null;
            }
            breakStartTime = 0;
            return;
        }

        if (lastBreakPos == null || !lastBreakPos.equals(pos)) {
            breakStartTime = System.currentTimeMillis();
            lastBreakPos = pos;
        }

        attackBlock(pos);
    }


    private void handleFullBlockMode() {
        Block targetType = getTargetBlockType();

        BlockPos visibleTarget = WorldUtil.getBestVisibleTarget(targetBlockPos, targetType);
        if (visibleTarget != null) {
            hasBrokenSurroundBlock = true;
            breakingSurroundingBlock = null;
            breakState = BreakState.BREAKING_BLOCK;
            attackTrackedBlock(visibleTarget);

            IBlockState targetState = mc.theWorld.getBlockState(visibleTarget);
            if (targetState.getBlock() == Blocks.air) {
                mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), visibleTarget, -1);
                if (swap) setLastSlot();
                resetBreaking();
            }
            return;
        }

        BlockPos obstruct = WorldUtil.getBestObstructingBlock(targetBlockPos, targetType, this::breakSeconds);
        if (obstruct == null) {
            breakState = BreakState.IDLE;
            breakingSurroundingBlock = null;
            return;
        }

        hasBrokenSurroundBlock = false;
        breakingSurroundingBlock = obstruct;
        breakState = BreakState.BREAKING_SURROUND;
        attackTrackedBlock(obstruct);
    }

    private void handleFirstBlockMode() {
        boolean bedPartVisible = canSeeAnyBedPart(targetBlockPos);

        if (!bedPartVisible && !hasBrokenSurroundBlock) {
            if (breakingSurroundingBlock == null) {
                breakingSurroundingBlock = WorldUtil.getClosestSurround(targetBlockPos);
            }

            if (breakingSurroundingBlock != null) {
                IBlockState toBreak = mc.theWorld.getBlockState(breakingSurroundingBlock);
                if (toBreak.getBlock().getMaterial() == Material.air) {
                    mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), breakingSurroundingBlock, -1);
                    hasBrokenSurroundBlock = true;
                    breakingSurroundingBlock = null;
                    breakStartTime = 0;
                } else {
                    breakState = BreakState.BREAKING_SURROUND;
                    if (lastBreakPos == null || !lastBreakPos.equals(breakingSurroundingBlock)) {
                        breakStartTime = System.currentTimeMillis();
                        lastBreakPos = breakingSurroundingBlock;
                    }
                    attackBlock(breakingSurroundingBlock);
                    return;
                }
            } else {
                hasBrokenSurroundBlock = true;
            }
        }

        breakBedDirectly();
    }

    private void handleThroughWallsMode() {
        breakBedDirectly();
    }

    private void breakBedDirectly() {
        if (targetBlockPos == null) {
            resetBreaking();
            return;
        }

        breakState = BreakState.BREAKING_BLOCK;

        if (lastBreakPos == null || !lastBreakPos.equals(targetBlockPos)) {
            breakStartTime = System.currentTimeMillis();
            lastBreakPos = targetBlockPos;
        }

        attackBlock(targetBlockPos);

        IBlockState targetState = mc.theWorld.getBlockState(targetBlockPos);
        if (targetState.getBlock() == Blocks.air) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), targetBlockPos, -1);
            if (swap) setLastSlot();
            resetBreaking();
        }
    }

    private void attackBlock(BlockPos pos) {
        double ax = pos.getX() + 0.5;
        double ay = pos.getY() + 0.5;
        double az = pos.getZ() + 0.5;

        float[] rots = RotationUtil.getRotationsFromPositionToPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ,
                ax, ay, az);

        RotationComponent.setRotations(rots[0], rots[1], 180, 180, true);

        double distanceToBlock = mc.thePlayer.getDistance(ax, ay, az);
        float blockReach = mc.playerController.getBlockReachDistance();

        if (distanceToBlock > blockReach) {
            resetBreaking();
            return;
        }

        if (getCastedModule(AutoTool.class).isToggled()) swap(pos);

        if (mc.playerController.onPlayerDamageBlock(pos, EnumFacing.UP)) {
            mc.thePlayer.swingItem();
        }

        mc.effectRenderer.addBlockHitEffects(pos, EnumFacing.UP);
    }

    @Listen
    public void onPacket(PacketEvent event) {
        if (event.getDirection() == PacketEvent.Direction.RECEIVE) {
            if (event.getPacket() instanceof S08PacketPlayerPosLook packet) {
                if (protectOwn.getProperty()) {
                    final double distance = mc.thePlayer.getDistance(packet.getX(), packet.getY(), packet.getZ());
                    if (distance > 40) {
                        ownPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
                        GothajClient.getInstance().getNotificationManager().add("Breaker", "Saved home position", Notification.Type.INFO);
                    }
                }
            }
        }
    }

    @Listen
    public void onRenderDimension(RenderDimensionEvent event) {
        if (KillAura.target != null || targetBlockPos == null) return;

        IBlockState state = mc.theWorld.getBlockState(targetBlockPos);
        if (state.getBlock() != getTargetBlockType()) return;

        float progress = 0.0f;
        BlockPos progressPos = null;

        if (breakState != BreakState.IDLE && breakStartTime > 0) {
            if (breakState == BreakState.BREAKING_SURROUND && breakingSurroundingBlock != null) {
                progressPos = breakingSurroundingBlock;
                progress = calculateBreakProgress(breakingSurroundingBlock);
            } else if (breakState == BreakState.BREAKING_BLOCK) {
                progressPos = targetBlockPos;
                progress = calculateBreakProgress(targetBlockPos);
            }
        }

        renderBedAndSurroundings(state, progress, progressPos);
        renderBlockTierIfEnabled();
    }

    private float calculateBreakProgress(BlockPos pos) {
        float seconds = breakSeconds(pos);
        if (seconds <= 0.0f || seconds == Float.MAX_VALUE) return 0.0f;

        float elapsed = (System.currentTimeMillis() - breakStartTime) / 1000.0f;
        return Math.min(1.0f, elapsed / seconds);
    }

    private float breakSeconds(BlockPos pos) {
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();
        if (block == null || block instanceof BlockAir || block.getMaterial() == Material.air) return 0.0f;
        if (mc.thePlayer.capabilities.isCreativeMode) return 0.05f;

        float hardness = block.getBlockHardness(mc.theWorld, pos);
        if (hardness < 0.0f) return Float.MAX_VALUE;

        float delta = block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, pos);
        if (delta <= 0.0f) return Float.MAX_VALUE;

        return (1.0f / delta) / 20.0f;
    }

    private void renderBedAndSurroundings(IBlockState state, float progress, BlockPos progressPos) {
        BlockPos otherHalfPos = targetBlockPos;
        if (state.getBlock() instanceof BlockBed) {
            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isHead = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
            otherHalfPos = isHead ? targetBlockPos.offset(facing.getOpposite()) : targetBlockPos.offset(facing);
        }

        int minX = Math.min(targetBlockPos.getX(), otherHalfPos.getX());
        int minY = Math.min(targetBlockPos.getY(), otherHalfPos.getY());
        int minZ = Math.min(targetBlockPos.getZ(), otherHalfPos.getZ());
        double sizeX = Math.abs(targetBlockPos.getX() - otherHalfPos.getX()) + 1;
        var stateAA = mc.theWorld.getBlockState(otherHalfPos);
        var bb = state.getBlock().getCollisionBoundingBox(mc.theWorld, otherHalfPos, stateAA);
        double sizeY = Math.abs(targetBlockPos.getY() - otherHalfPos.getY()) + bb.maxY - bb.minY;
        double sizeZ = Math.abs(targetBlockPos.getZ() - otherHalfPos.getZ()) + 1;

        float bedProgress = (progressPos != null && (progressPos.equals(targetBlockPos) || progressPos.equals(otherHalfPos))) ? progress : 0.0f;
        RenderUtil.renderBox(minX, minY, minZ, sizeX, sizeY, sizeZ, ColorUtil.setAlpha(Hud.getColor(5), 56), true, false, bedProgress);

        if (progressPos != null && breakState == BreakState.BREAKING_SURROUND && breakingSurroundingBlock != null) {
            RenderUtil.renderBlock(progressPos, ColorUtil.setAlpha(new Color(255, 100, 100).getRGB(), 80), true, false, progress);
        }
    }

    private void renderBlockTierIfEnabled() {
        if (!showBlockTier.getProperty()) return;

        BlockPos otherHalfPos = targetBlockPos;
        IBlockState state = mc.theWorld.getBlockState(targetBlockPos);
        if (state.getBlock() instanceof BlockBed) {
            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isHead = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
            otherHalfPos = isHead ? targetBlockPos.offset(facing.getOpposite()) : targetBlockPos.offset(facing);
        }

        if (showCurrentBlock.getProperty()) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
                renderBlockTier(mc.objectMouseOver.getBlockPos());
            }
        } else {
            renderBlockTierLoop(otherHalfPos);
        }
    }

    private void renderBlockTierLoop(BlockPos otherHalfPos) {
        var targets = new ArrayList<BlockPos>();
        targets.add(targetBlockPos);
        if (!otherHalfPos.equals(targetBlockPos)) targets.add(otherHalfPos);

        int intRange = range.getProperty().intValue();
        double rangeSq = intRange * intRange;

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        targets.forEach(queue::add);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) continue;

            if (!targets.contains(current)) {
                renderBlockTier(current);
            }

            boolean inRange = targets.stream().anyMatch(t -> t.distanceSq(current) <= rangeSq);
            if (!inRange) continue;

            if (current.getY() < Math.min(targetBlockPos.getY(), otherHalfPos.getY())) continue;

            for (EnumFacing dir : EnumFacing.values()) {
                if (dir == EnumFacing.DOWN) continue;
                BlockPos adj = current.offset(dir);
                if (visited.contains(adj)) continue;
                Block block = mc.theWorld.getBlockState(adj).getBlock();
                if (!(block instanceof BlockAir) && block != getTargetBlockType() && !(block.getMaterial() instanceof MaterialLiquid)) {
                    queue.add(adj);
                }
            }
        }
    }

    private void renderBlockTier(BlockPos checkPos) {
        Block block = mc.theWorld.getBlockState(checkPos).getBlock();

        if (breakMode.is("Through Walls") || block instanceof BlockAir || block.getMaterial() instanceof MaterialLiquid || block == getTargetBlockType())
            return;

        BreakTier tier = tierCache.get(checkPos);
        if (tier == null) {
            tier = getBreakTier(checkPos);
            if (tier != null) tierCache.put(checkPos, tier);
        }
        if (tier != null) {
            RenderUtil.renderBlock(checkPos, ColorUtil.setAlpha(tier.getColor(), 26), false, true);
        }
    }

    @Override
    public void onEnable() {
        tierCache.clear();
        hasBrokenSurroundBlock = false;
        breakingSurroundingBlock = null;
        breakState = BreakState.IDLE;
        breakStartTime = 0;
        lastBreakPos = null;
        breakPath.clear();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        targetBlockPos = null;
        hasBrokenSurroundBlock = false;
        breakingSurroundingBlock = null;
        tierCache.clear();
        setLastSlot();
        breakState = BreakState.IDLE;
        breakStartTime = 0;
        lastBreakPos = null;
        breakPath.clear();
        super.onDisable();
    }

    private void resetBreaking() {

        breakingSurroundingBlock = null;
        hasBrokenSurroundBlock = false;
        targetBlockPos = null;
        breakState = BreakState.IDLE;
        breakStartTime = 0;
        lastBreakPos = null;
        breakPath.clear();

        setLastSlot();
    }

    private void swap(BlockPos block) {
        if (KillAura.target != null || mc.theWorld.getBlockState(block).getBlock() instanceof BlockAir) return;

        float toolSpeed = -1;
        int bestIndex = -1;
        Block targetBlock = mc.theWorld.getBlockState(block).getBlock();

        for (int i = 36; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack == null) continue;

            float bestSpeed = 0;

            if (stack.getItem() instanceof ItemTool) {
                bestSpeed = stack.getStrVsBlock(targetBlock);
            } else if (stack.getItem() instanceof ItemShears) {
                if (isShearableBlock(targetBlock)) {
                    bestSpeed = 15.0f;
                } else {
                    bestSpeed = stack.getStrVsBlock(targetBlock);
                }
            } else {
                continue;
            }

            if (bestSpeed > toolSpeed) {
                bestIndex = i;
                toolSpeed = bestSpeed;
            }
        }

        if (bestIndex != -1 && mc.thePlayer.inventory.currentItem != bestIndex - 36) {
            if (!swap) {
                lastSlot = mc.thePlayer.inventory.currentItem;
                swap = true;
            }
            setSlot(bestIndex);
        }
    }

    private boolean isShearableBlock(Block block) {
        return block instanceof BlockWeb ||
                block.getUnlocalizedName().contains("wool") ||
                block.getUnlocalizedName().contains("leaves") ||
                block.getUnlocalizedName().contains("vine");
    }

    private boolean canSeeBlock(BlockPos pos) {
        if (pos == null) return false;
        Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(eyes, center, false, false, false);

        return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.getBlockPos().equals(pos);
    }

    private boolean canSeeAnyBedPart(BlockPos target) {
        IBlockState state = mc.theWorld.getBlockState(target);
        if (!(state.getBlock() instanceof BlockBed)) {
            return canSeeBlock(target);
        }

        EnumFacing facing = state.getValue(BlockBed.FACING);
        boolean isHead = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
        BlockPos headPos = isHead ? target : target.offset(facing);
        BlockPos footPos = isHead ? target.offset(facing.getOpposite()) : target;

        return canSeeBlock(headPos) || canSeeBlock(footPos);
    }

    private BlockPos getObstructingBlock(BlockPos target) {
        Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        BlockPos headPos = target;
        BlockPos footPos = target;
        IBlockState state = mc.theWorld.getBlockState(target);
        if (state.getBlock() instanceof BlockBed) {
            EnumFacing facing = state.getValue(BlockBed.FACING);
            if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                footPos = target.offset(facing.getOpposite());
            } else {
                headPos = target.offset(facing);
            }
        }

        Vec3 headCenter = new Vec3(headPos.getX() + 0.5, headPos.getY() + 0.5625, headPos.getZ() + 0.5);
        MovingObjectPosition mopHead = mc.theWorld.rayTraceBlocks(eyes, headCenter, false, true, false);

        if (mopHead != null && mopHead.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hit = mopHead.getBlockPos();
            Block block = mc.theWorld.getBlockState(hit).getBlock();
            if (block != getTargetBlockType() && block.getMaterial() != Material.air) {
                return hit;
            }
        }

        Vec3 footCenter = new Vec3(footPos.getX() + 0.5, footPos.getY() + 0.5625, footPos.getZ() + 0.5);
        MovingObjectPosition mopFoot = mc.theWorld.rayTraceBlocks(eyes, footCenter, false, true, false);

        if (mopFoot != null && mopFoot.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hit = mopFoot.getBlockPos();
            Block block = mc.theWorld.getBlockState(hit).getBlock();
            if (block != getTargetBlockType() && block.getMaterial() != Material.air) {
                return hit;
            }
        }

        return null;
    }

    private void setLastSlot() {
        if (swap && mc.thePlayer != null && lastSlot != -1 && mc.thePlayer.inventory.currentItem != lastSlot) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
        swap = false;
        lastSlot = -1;
    }

    private void setSlot(int slot) {
        mc.thePlayer.inventory.currentItem = slot - 36;
    }

    public BreakTier getBreakTier(BlockPos pos) {
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();

        if (block == null || block instanceof BlockAir) return null;
        if (mc.thePlayer.capabilities.isCreativeMode) return BreakTier.EASY;
        if (block == Blocks.obsidian) return BreakTier.HARD;

        float seconds = breakSeconds(pos);
        if (seconds == Float.MAX_VALUE) return BreakTier.HARD;

        if (seconds <= 0.40f) return BreakTier.EASY;
        if (seconds <= 1.50f) return BreakTier.MEDIUM;
        return BreakTier.HARD;
    }

    @Getter
    @AllArgsConstructor
    enum BreakTier {
        EASY(new Color(69, 204, 69).getRGB()),
        MEDIUM(new Color(248, 216, 66).getRGB()),
        HARD(new Color(186, 54, 46).getRGB());

        private final int color;
    }

    enum BreakState {
        IDLE,
        BREAKING_SURROUND,
        BREAKING_BLOCK
    }
}



