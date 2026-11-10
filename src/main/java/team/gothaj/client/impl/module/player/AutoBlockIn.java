package team.gothaj.client.impl.module.player;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.impl.event.player.RotationEvent;
import team.gothaj.eventbus.Listen;

public class AutoBlockIn extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 200, 50, 1000, 50);

    private final TimerUtil timer = new TimerUtil();
    private float[] rotations = new float[2];
    private int rotationTicks;

    public AutoBlockIn() {
        super("AutoBlockIn", "Places blocks around you to block into beds", ModuleCategory.PLAYER);
        registerProperties(delay);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rotationTicks = 0;
    }

    @Override
    public String getSuffix() {
        return "";
    }

    @Listen
    public void onRotation(RotationEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        BlockPos base = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        if (place(base.add(1, 0, 0))) return;
        if (place(base.add(-1, 0, 0))) return;
        if (place(base.add(0, 0, 1))) return;
        if (place(base.add(0, 0, -1))) return;

        if (place(base.add(1, 1, 0))) return;
        if (place(base.add(-1, 1, 0))) return;
        if (place(base.add(0, 1, 1))) return;
        if (place(base.add(0, 1, -1))) return;

        if (place(base.add(1, 2, 0))) return;
        if (place(base.add(-1, 2, 0))) return;
        if (place(base.add(0, 2, 1))) return;
        if (place(base.add(0, 2, -1))) return;

        place(base.add(0, 2, 0));
    }

    private boolean place(BlockPos pos) {
        if (!timer.hasTimeReached(delay.getProperty().longValue())) return false;
        if (!mc.theWorld.isAirBlock(pos)) return false;

        int slot = findBlockInHotbar();
        if (slot == -1) return false;

        BlockPos[] offsets = {
            pos.add(1, 0, 0), pos.add(-1, 0, 0),
            pos.add(0, 0, 1), pos.add(0, 0, -1),
            pos.add(0, -1, 0), pos.add(0, 1, 0)
        };

        for (BlockPos neighbor : offsets) {
            if (!isValidSupport(neighbor)) continue;
            EnumFacing side = getFacingFrom(pos, neighbor);
            if (side == null) continue;

            faceBlock(neighbor, side);

            if (rotationTicks >= 1) {
                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
                        rotations[0], rotations[1], mc.thePlayer.onGround));

                Vec3 hitVec = new Vec3(
                    neighbor.getX() + 0.5 + side.getFrontOffsetX() * 0.5,
                    neighbor.getY() + 0.5 + side.getFrontOffsetY() * 0.5,
                    neighbor.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5
                );

                int old = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = slot;
                mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(),
                        neighbor, side, hitVec);
                mc.thePlayer.inventory.currentItem = old;

                rotationTicks = 0;
                timer.reset();
                checkComplete();
                return true;
            }

            rotationTicks++;
        }

        return false;
    }

    private boolean isValidSupport(BlockPos pos) {
        if (mc.theWorld == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null || block == Blocks.air) return false;
        return block.getMaterial().isSolid() && block.isFullCube();
    }

    private EnumFacing getFacingFrom(BlockPos target, BlockPos neighbor) {
        if (neighbor.getX() + 1 == target.getX()) return EnumFacing.EAST;
        if (neighbor.getX() - 1 == target.getX()) return EnumFacing.WEST;
        if (neighbor.getY() + 1 == target.getY()) return EnumFacing.UP;
        if (neighbor.getY() - 1 == target.getY()) return EnumFacing.DOWN;
        if (neighbor.getZ() + 1 == target.getZ()) return EnumFacing.SOUTH;
        if (neighbor.getZ() - 1 == target.getZ()) return EnumFacing.NORTH;
        return null;
    }

    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                return i;
            }
        }
        return -1;
    }

    private void checkComplete() {
        BlockPos base = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        BlockPos[] all = {
            base.add(1, 0, 0), base.add(-1, 0, 0), base.add(0, 0, 1), base.add(0, 0, -1),
            base.add(1, 1, 0), base.add(-1, 1, 0), base.add(0, 1, 1), base.add(0, 1, -1),
            base.add(1, 2, 0), base.add(-1, 2, 0), base.add(0, 2, 1), base.add(0, 2, -1),
            base.add(0, 2, 0)
        };
        for (BlockPos pos : all) {
            if (mc.theWorld.isAirBlock(pos)) return;
        }
        toggle();
    }

    private void faceBlock(BlockPos pos, EnumFacing side) {
        double x = pos.getX() + 0.5 + side.getFrontOffsetX() * 0.5;
        double y = pos.getY() + 0.25 + side.getFrontOffsetY() * 0.5;
        double z = pos.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5;
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        rotations[0] = yaw;
        rotations[1] = pitch;
    }
}
