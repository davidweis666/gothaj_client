package team.gothaj.client.impl.module.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockContainer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import org.lwjgl.input.Keyboard;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.PostMotionEvent;
import team.gothaj.eventbus.Listen;

import java.util.ArrayList;
import java.util.List;

public class AutoBedDefense extends Module {

    public final NumberProperty range = new NumberProperty("Range", 5, 1, 10, 1);

    public final NumberProperty placeDelay = new NumberProperty("Place Delay", 150, 0, 500, 10);

    private final TimerUtil placeTimer = new TimerUtil();
    private long nextDelay = 150;

    private static final float TURN_SPEED = 25f;

    private float currentYaw, currentPitch;
    private boolean rotationInitialized = false;

    private BlockPos bedPos = null;
    private final List<BlockPos> targets = new ArrayList<>();
    private PlaceData pendingPlace = null;
    private int scanTimer = 0;
    private int originalSlot = -1;

    public AutoBedDefense() {
        super("AutoBedDefense", "Auto places a one-layer ring of blocks around your bed", ModuleCategory.WORLD);
        registerProperties(range, placeDelay);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            originalSlot = mc.thePlayer.inventory.currentItem;
        }
        rotationInitialized = false;
        placeTimer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && originalSlot != -1) {
            mc.thePlayer.inventory.currentItem = originalSlot;
        }
        mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        rotationInitialized = false;
        bedPos = null;
        targets.clear();
        pendingPlace = null;
        super.onDisable();
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) return;

        if (scanTimer++ % 5 == 0 || bedPos == null) {
            bedPos = findBed();
            if (bedPos == null) {
                targets.clear();
                pendingPlace = null;
                return;
            }
            if (targets.isEmpty()) {
                buildTargets(bedPos);
            }
        }

        double distToBed = mc.thePlayer.getDistance(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5);
        if (distToBed > range.getProperty() + 1.5) return;
        if (distToBed > mc.playerController.getBlockReachDistance() + 1) return;

        if (pendingPlace != null) {
            setPlaceRotations(pendingPlace);
            return;
        }

        if (!placeTimer.hasTimeReached(nextDelay)) return;

        int blockSlot = InventoryUtil.getBlockSlot();
        if (blockSlot == -1) return;

        BlockPos next = getNextTarget();
        if (next == null) {
            if (!targets.isEmpty()) {
                setToggled(false);
            }
            return;
        }

        PlaceData data = findPlaceFace(next);
        if (data == null) return;

        setPlaceRotations(data);

        mc.thePlayer.inventory.currentItem = blockSlot;
        mc.gameSettings.keyBindSneak.pressed = true;
        pendingPlace = data;
    }

    @Listen
    public void onPostMotion(PostMotionEvent event) {
        if (pendingPlace == null) return;

        PlaceData data = pendingPlace;
        if (!isRotationReady(data)) return;

        pendingPlace = null;

        mc.playerController.syncCurrentPlayItem();

        boolean placed = mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                mc.thePlayer.getHeldItem(), data.pos, data.side, data.hitVec);

        if (placed) {
            mc.thePlayer.swingItem();
            placeTimer.reset();
            nextDelay = (long) (placeDelay.getProperty() * (0.7 + Math.random() * 0.6));
        }

        mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    private void setPlaceRotations(PlaceData data) {
        float[] rotations = getRotationsTo(data.pos, data.side);
        data.targetYaw = rotations[0];
        data.targetPitch = rotations[1];

        if (!rotationInitialized) {
            currentYaw = mc.thePlayer.rotationYaw;
            currentPitch = mc.thePlayer.rotationPitch;
            rotationInitialized = true;
        }

        float speed = TURN_SPEED + (float) (Math.random() * 10);
        float yawDelta = MathHelper.wrapAngleTo180_float(data.targetYaw - currentYaw);
        float pitchDelta = data.targetPitch - currentPitch;
        if (Math.abs(yawDelta) > speed) yawDelta = Math.signum(yawDelta) * speed;
        if (Math.abs(pitchDelta) > speed) pitchDelta = Math.signum(pitchDelta) * speed;

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        yawDelta = yawDelta - (yawDelta % gcd);
        pitchDelta = pitchDelta - (pitchDelta % gcd);

        currentYaw = MathHelper.wrapAngleTo180_float(currentYaw + yawDelta);
        currentPitch = MathHelper.clamp(currentPitch + pitchDelta, -90.0F, 90.0F);

        RotationComponent.setRotations(currentYaw, currentPitch, 180, 180, true);
    }

    private boolean isRotationReady(PlaceData data) {
        return Math.abs(MathHelper.wrapAngleTo180_float(currentYaw - data.targetYaw)) < 3.0F
                && Math.abs(currentPitch - data.targetPitch) < 3.0F;
    }

    private BlockPos findBed() {
        int searchRange = range.getProperty().intValue();
        double best = Double.MAX_VALUE;
        BlockPos bestPos = null;

        for (int dx = -searchRange; dx <= searchRange; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -searchRange; dz <= searchRange; dz++) {
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + dx, mc.thePlayer.posY + dy, mc.thePlayer.posZ + dz);
                    if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.bed) continue;

                    double d = pos.distanceSqToCenter(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                    if (d < best) {
                        best = d;
                        bestPos = pos;
                    }
                }
            }
        }

        return bestPos;
    }

    private void buildTargets(BlockPos bed) {
        targets.clear();

        List<BlockPos> bedCells = new ArrayList<>();
        bedCells.add(bed);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = bed.offset(side);
            if (mc.theWorld.getBlockState(neighbor).getBlock() == Blocks.bed) {
                bedCells.add(neighbor);
            }
        }

        for (BlockPos cell : bedCells) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos pos = cell.add(dx, 0, dz);
                    if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.bed) continue;
                    if (targets.contains(pos)) continue;
                    targets.add(pos);
                }
            }
        }

        for (BlockPos cell : bedCells) {
            BlockPos above = cell.up();
            if (!targets.contains(above)) {
                targets.add(above);
            }
        }

        targets.sort((a, b) -> Double.compare(
                a.distanceSqToCenter(bed.getX() + 0.5, bed.getY() + 0.5, bed.getZ() + 0.5),
                b.distanceSqToCenter(bed.getX() + 0.5, bed.getY() + 0.5, bed.getZ() + 0.5)));
    }

    private BlockPos getNextTarget() {
        for (BlockPos target : targets) {
            if (!isReplaceable(target)) continue;
            if (intersectsPlayer(target)) continue;
            if (findPlaceFace(target) != null) return target;
        }
        return null;
    }

    private boolean intersectsPlayer(BlockPos pos) {
        AxisAlignedBB cell = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        return cell.intersectsWith(mc.thePlayer.getEntityBoundingBox());
    }

    private PlaceData findPlaceFace(BlockPos target) {
        double bestDistance = Double.MAX_VALUE;
        PlaceData best = null;
        double reachSq = mc.playerController.getBlockReachDistance() * mc.playerController.getBlockReachDistance();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos neighbor = target.add(dx, dy, dz);
                    if (!isSolid(neighbor)) continue;

                    for (EnumFacing side : EnumFacing.VALUES) {
                        if (!neighbor.offset(side).equals(target)) continue;

                        double dist = mc.thePlayer.getDistanceSq(
                                neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5);
                        if (dist > reachSq) continue;
                        if (dist < bestDistance) {
                            bestDistance = dist;
                            best = new PlaceData(neighbor, side, getFaceCenter(neighbor, side));
                        }
                    }
                }
            }
        }

        return best;
    }

    private Vec3 getFaceCenter(BlockPos pos, EnumFacing side) {
        Vec3i dir = side.getDirectionVec();
        return new Vec3(pos.getX() + 0.5 + dir.getX() * 0.5,
                pos.getY() + 0.5 + dir.getY() * 0.5,
                pos.getZ() + 0.5 + dir.getZ() * 0.5);
    }

    private float[] getRotationsTo(BlockPos pos, EnumFacing side) {
        Vec3i dir = side.getDirectionVec();
        double x = pos.getX() + 0.5 + dir.getX() * 0.5;
        double y = pos.getY() + 0.5 + dir.getY() * 0.5;
        double z = pos.getZ() + 0.5 + dir.getZ() * 0.5;

        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[] { MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F) };
    }

    private boolean isSolid(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.getMaterial().isSolid() && !block.getMaterial().isLiquid()
                && !block.isReplaceable(mc.theWorld, pos) && !(block instanceof BlockContainer);
    }

    private boolean isReplaceable(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block instanceof BlockAir || block.isReplaceable(mc.theWorld, pos) || block.getMaterial().isLiquid();
    }

    private static class PlaceData {
        final BlockPos pos;
        final EnumFacing side;
        final Vec3 hitVec;
        float targetYaw;
        float targetPitch;

        PlaceData(BlockPos pos, EnumFacing side, Vec3 hitVec) {
            this.pos = pos;
            this.side = side;
            this.hitVec = hitVec;
        }
    }
}
