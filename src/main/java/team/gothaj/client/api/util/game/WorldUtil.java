package team.gothaj.client.api.util.game;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import team.gothaj.client.api.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class WorldUtil implements Utils {

    public static final Set<BlockPos> openedChests = new HashSet<>();

    public static List<BlockPos> getLineOfSightPath(BlockPos targetPos) {
        List<BlockPos> path = new ArrayList<>();
        if (targetPos == null) return path;

        Vec3 start = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );

        Vec3 end = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);

        double distance = start.distanceTo(end);
        double dirX = (end.xCoord - start.xCoord) / distance;
        double dirY = (end.yCoord - start.yCoord) / distance;
        double dirZ = (end.zCoord - start.zCoord) / distance;

        double step = 0.1;

        for (double i = 0; i <= distance; i += step) {
            double currentX = start.xCoord + (dirX * i);
            double currentY = start.yCoord + (dirY * i);
            double currentZ = start.zCoord + (dirZ * i);

            BlockPos currentPos = new BlockPos(currentX, currentY, currentZ);

            if (currentPos.equals(targetPos)) {
                break;
            }

            Block block = mc.theWorld.getBlockState(currentPos).getBlock();

            if (!(block instanceof BlockAir) &&
                    block.getMaterial() != Material.air &&
                    !(block.getMaterial() instanceof MaterialLiquid) &&
                    !(block instanceof BlockBed)) {

                if (!path.contains(currentPos)) {
                    path.add(currentPos);
                }
            }
        }

        return path;
    }

    public static BlockPos findBlock(Block targetBlock, double range) {
        if (targetBlock == null || mc.thePlayer == null || mc.theWorld == null) {
            return null;
        }

        BlockPos playerPos = mc.thePlayer.getPosition();
        int intRange = MathHelper.ceiling_double_int(range);
        double rangeSq = range * range;

        BlockPos closestPos = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int dx = -intRange; dx <= intRange; dx++) {
            for (int dy = -intRange; dy <= intRange; dy++) {
                for (int dz = -intRange; dz <= intRange; dz++) {
                    BlockPos checkPos = playerPos.add(dx, dy, dz);

                    if (!mc.theWorld.isBlockLoaded(checkPos)) {
                        continue;
                    }

                    Block block = mc.theWorld.getBlockState(checkPos).getBlock();
                    if (block != targetBlock) {
                        continue;
                    }

                    double distSq = playerPos.distanceSq(checkPos);
                    if (distSq > rangeSq) {
                        continue;
                    }

                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestPos = checkPos;
                    }
                }
            }
        }

        return closestPos;
    }

    public static BlockPos getBestObstructingBlock(BlockPos targetPos, Block targetBlock, ToDoubleFunction<BlockPos> costFn) {
        if (targetPos == null || targetBlock == null) return null;

        List<BlockPos> targets = new ArrayList<>();
        targets.add(targetPos);

        IBlockState state = mc.theWorld.getBlockState(targetPos);
        if (targetBlock instanceof BlockBed && state.getBlock() instanceof BlockBed) {
            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isHead = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
            BlockPos otherHalf = isHead ? targetPos.offset(facing.getOpposite()) : targetPos.offset(facing);
            targets.add(otherHalf);
        }

        Vec3 eyes = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos pos : targets) {
            for (Vec3 sample : getSamplePoints(pos)) {
                MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(eyes, sample, false, true, false);
                if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;

                BlockPos hitPos = mop.getBlockPos();
                if (hitPos == null || hitPos.getY() < targetPos.getY()) continue;

                Block hitBlock = mc.theWorld.getBlockState(hitPos).getBlock();
                if (hitBlock == null) continue;
                if (hitBlock == targetBlock) continue;
                if (hitBlock instanceof BlockAir) continue;
                if (hitBlock.getMaterial() == Material.air) continue;
                if (hitBlock.getMaterial() instanceof MaterialLiquid) continue;

                double breakCost = costFn.applyAsDouble(hitPos);
                if (breakCost >= Float.MAX_VALUE) continue;

                double dist = mc.thePlayer.getDistance(hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5);
                double score = breakCost + (dist * 0.05);

                if (score < bestScore) {
                    bestScore = score;
                    best = hitPos;
                }
            }
        }

        return best;
    }

    public static BlockPos getBestVisibleTarget(BlockPos targetPos, Block targetBlock) {
        if (targetPos == null || targetBlock == null) return null;

        List<BlockPos> targets = new ArrayList<>();
        targets.add(targetPos);

        IBlockState state = mc.theWorld.getBlockState(targetPos);
        if (targetBlock instanceof BlockBed && state.getBlock() instanceof BlockBed) {
            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isHead = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
            BlockPos otherHalf = isHead ? targetPos.offset(facing.getOpposite()) : targetPos.offset(facing);
            targets.add(otherHalf);
        }

        Vec3 eyes = new Vec3(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.posZ
        );

        float reach = mc.playerController.getBlockReachDistance();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : targets) {
            double dist = mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist > reach) continue;

            for (Vec3 sample : getSamplePoints(pos)) {
                MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(eyes, sample, false, true, false);
                if (mop == null) {
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                    continue;
                }

                if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && pos.equals(mop.getBlockPos())) {
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private static List<Vec3> getSamplePoints(BlockPos pos) {
        List<Vec3> points = new ArrayList<>();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        points.add(new Vec3(x + 0.5, y + 0.5, z + 0.5));
        points.add(new Vec3(x + 0.5, y + 0.8, z + 0.5));
        points.add(new Vec3(x + 0.2, y + 0.5, z + 0.5));
        points.add(new Vec3(x + 0.8, y + 0.5, z + 0.5));
        points.add(new Vec3(x + 0.5, y + 0.5, z + 0.2));
        points.add(new Vec3(x + 0.5, y + 0.5, z + 0.8));

        return points;
    }

    public static BlockPos getClosestSurround(BlockPos targetPos) {
        IBlockState state = mc.theWorld.getBlockState(targetPos);
        List<BlockPos> targetsToCheck = new ArrayList<>();
        targetsToCheck.add(targetPos);

        if (state.getBlock() instanceof BlockBed) {
            EnumFacing facing = state.getValue(BlockBed.FACING);
            boolean isHead = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
            targetsToCheck.add(isHead ? targetPos.offset(facing.getOpposite()) : targetPos.offset(facing));
        }

        BlockPos closestBlock = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos center : targetsToCheck) {
            BlockPos[] candidates = new BlockPos[]{
                    center.up(),
                    center.north(),
                    center.south(),
                    center.east(),
                    center.west()
            };

            for (BlockPos candidate : candidates) {
                if (targetsToCheck.contains(candidate)) continue;

                if (!mc.theWorld.isBlockLoaded(candidate)) continue;

                Block block = mc.theWorld.getBlockState(candidate).getBlock();
                if (block instanceof BlockAir || block.getMaterial() instanceof MaterialLiquid || block instanceof BlockBed || block == state.getBlock()) continue;

                double dist = mc.thePlayer.getDistance(candidate.getX() + 0.5, candidate.getY() + 0.5, candidate.getZ() + 0.5);
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestBlock = candidate;
                }
            }
        }

        return closestBlock;
    }
}

