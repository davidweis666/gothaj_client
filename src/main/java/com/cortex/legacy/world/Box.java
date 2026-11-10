package com.cortex.legacy.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

/**
 * An axis-aligned box in absolute world coordinates.
 *
 * <p>Held in doubles, like everything else positional in Minecraft. The conversion to the
 * floats the GPU wants happens only after the camera position has been subtracted, which is
 * what keeps a box steady out at the edges of a world where a raw float coordinate has
 * error measured in whole blocks.
 */
public final class Box {

    public final double minX, minY, minZ;
    public final double maxX, maxY, maxZ;

    public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public static Box of(double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ) {
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The unit cube occupied by a block. */
    public static Box ofBlock(BlockPos pos) {
        return new Box(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    /** A box spanning {@code size} blocks from a corner — the shape {@code Breaker} draws. */
    public static Box ofSize(double x, double y, double z,
                             double sizeX, double sizeY, double sizeZ) {
        return new Box(x, y, z, x + sizeX, y + sizeY, z + sizeZ);
    }

    public static Box of(AxisAlignedBB bb) {
        return new Box(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    /**
     * An entity's bounding box, interpolated to where it is being drawn this frame.
     *
     * <p>Taking {@code getEntityBoundingBox()} directly would box the entity at its last
     * ticked position while the model itself is drawn interpolated, so the outline visibly
     * lags behind anything moving.
     */
    public static Box ofEntity(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        AxisAlignedBB bb = entity.getEntityBoundingBox();

        return new Box(
                x - (entity.posX - bb.minX), y - (entity.posY - bb.minY), z - (entity.posZ - bb.minZ),
                x + (bb.maxX - entity.posX), y + (bb.maxY - entity.posY), z + (bb.maxZ - entity.posZ));
    }

    /** Grows the box by {@code amount} on every axis (negative shrinks it). */
    public Box inflate(double amount) {
        return new Box(minX - amount, minY - amount, minZ - amount,
                maxX + amount, maxY + amount, maxZ + amount);
    }

    public Box offset(double dx, double dy, double dz) {
        return new Box(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    /**
     * The box with its top brought down to {@code progress} of its full height, for
     * fill-up indicators such as block break progress.
     *
     * @param progress 0..1; values outside are clamped
     */
    public Box toProgress(float progress) {
        double clamped = Math.clamp(progress, 0.0, 1.0);
        return new Box(minX, minY, minZ, maxX, minY + (maxY - minY) * clamped, maxZ);
    }

    public double centerX() {
        return (minX + maxX) * 0.5;
    }

    public double centerY() {
        return (minY + maxY) * 0.5;
    }

    public double centerZ() {
        return (minZ + maxZ) * 0.5;
    }

    @Override
    public String toString() {
        return "Box[" + minX + ", " + minY + ", " + minZ
                + " -> " + maxX + ", " + maxY + ", " + maxZ + "]";
    }
}
