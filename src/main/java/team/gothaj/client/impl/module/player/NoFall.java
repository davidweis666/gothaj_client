package team.gothaj.client.impl.module.player;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ListProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.Logger;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.impl.component.RotationComponent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.PostMotionEvent;
import team.gothaj.client.impl.event.player.item.SpoofClientItemEvent;
import team.gothaj.eventbus.Listen;

public class NoFall extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", "Clutch");
    public final ListProperty clutchMethods = new ListProperty("Clutch Items", "Bucket", "Cobweb", "Ladder").setVisible(() -> mode.is("Clutch"));
    public final BooleanProperty slotSpoof = new BooleanProperty("Spoof Slot", true).setVisible(() -> mode.is("Clutch"));

    public final NumberProperty fallDist = new NumberProperty("Fall Distance", 3, 1, 10, 1);

    private int spoofSlot;
    private int originalSlot = -1;
    private boolean placedWater = false;

    private TimerUtil timer = new TimerUtil();

    public NoFall() {
        super("NoFall", "", ModuleCategory.PLAYER);
        registerProperties(mode, clutchMethods, slotSpoof, fallDist);
    }

    @Override
    public String getSuffix() {
        return mode.getProperty();
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null && getClutchSlot() != -1) {
            spoofSlot = mc.thePlayer.inventory.currentItem;
        }
        originalSlot = -1;
        placedWater = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && slotSpoof.getProperty()) {
            if (spoofSlot != -1) mc.thePlayer.inventory.currentItem = spoofSlot;
        }
        placedWater = false;
        originalSlot = -1;
        super.onDisable();
    }

    @Listen
    public void onSpoofItem(SpoofClientItemEvent event) {
        if (slotSpoof.getProperty()) {
            if (spoofSlot != -1 && spoofSlot != mc.thePlayer.inventory.currentItem) {
                event.setItem(mc.thePlayer.inventory.getStackInSlot(spoofSlot));
            }
        }
    }

    @Listen
    public void onUpdate(LivingUpdateEvent event) {
        if (!mode.is("Clutch") && mc.thePlayer.getActivePotionEffect(Potion.jump) != null) return;

        if (mc.thePlayer.onGround && !placedWater) {
            spoofSlot = mc.thePlayer.inventory.currentItem;
            if (originalSlot != -1) {
                mc.thePlayer.inventory.currentItem = originalSlot;
                originalSlot = -1;
            }
        }

        if (placedWater) {
            if (mc.thePlayer.isInWater()) {
                Logger.debugChat(this, "Placed water bucket");
                int bucketSlot = InventoryUtil.getEmptyBucket();

                if (bucketSlot != -1 && timer.hasTimeReached(750)) {
                    Logger.debugChat(this, "Grabbing water back");
                    mc.thePlayer.inventory.currentItem = bucketSlot;
                    mc.rightClickMouse();
                    timer.reset();

                    placedWater = false;

                    if (!slotSpoof.getProperty() && originalSlot != -1) {
                        mc.thePlayer.inventory.currentItem = originalSlot;
                        originalSlot = -1;
                    }
                }
            }
            return;
        }

        if (mc.thePlayer.fallDistance >= fallDist.getProperty()) {
            int slot = getClutchSlot();

            if (slot == -1) {
                return;
            }

            RotationComponent.setRotations(mc.thePlayer.rotationYaw, 90, 180, 360, true);

            MovingObjectPosition ray = mc.objectMouseOver;

            if (ray != null && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && ray.getBlockPos() != null) {

                Logger.debugChat(this, "Hit block at " + ray.getBlockPos());

                BlockPos pos = ray.getBlockPos();

                double distToBlock = mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                if (distToBlock > 3.0) {
                    Logger.debugChat(this, "Too far away");
                    return;
                }

                if (!mc.theWorld.getBlockState(pos).getBlock().getMaterial().isLiquid()) {

                    Logger.debugChat(this, "Block is replaceable");

                    if (spoofSlot == -1) {
                        spoofSlot = mc.thePlayer.inventory.currentItem;
                    }

                    if (!slotSpoof.getProperty() && originalSlot == -1) {
                        originalSlot = mc.thePlayer.inventory.currentItem;
                    }

                    mc.thePlayer.inventory.currentItem = slot;

                    if (clutchMethods.getOptionState("Ladder") && mc.thePlayer.getHeldItem().getItem() == Item.getItemFromBlock(Blocks.ladder)) {
                        if (ray.sideHit == EnumFacing.UP || ray.sideHit == EnumFacing.DOWN) {
                            return;
                        }
                    }

                    Logger.debugChat(this, "Attempting to place... " + RotationComponent.rotations[1]);

                    if (RotationComponent.rotations[1] > 80) {
                        mc.rightClickMouse();
                        Logger.debugChat(this, "Placed item");
                    }

                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
                    boolean isWaterBucket = stack != null && stack.getItem() == Items.bucket;

                    if (isWaterBucket) {
                        placedWater = true;
                    } else {
                        if (!slotSpoof.getProperty() && originalSlot != -1) {
                            mc.thePlayer.inventory.currentItem = originalSlot;
                            originalSlot = -1;
                        }
                    }
                }
            }
        }
    }

    @Listen
    public void onPostMotion(PostMotionEvent event) {
        if (slotSpoof.getProperty() && getClutchSlot() != -1 && mc.thePlayer.fallDistance >= fallDist.getProperty()) {
            mc.thePlayer.inventory.currentItem = spoofSlot;
        }
    }

    public int getClutchSlot() {
        if (clutchMethods.getOptionState("Bucket")) {
            int slot = InventoryUtil.getBucketSlot();
            if (slot != -1) return slot;
        }

        if (clutchMethods.getOptionState("Cobweb")) {
            int slot = InventoryUtil.getBlockSlot(Blocks.web);
            if (slot != -1) return slot;
        }

        if (clutchMethods.getOptionState("Ladder")) {
            int slot = InventoryUtil.getBlockSlot(Blocks.ladder);
            if (slot != -1) return slot;
        }

        return -1;
    }
}
