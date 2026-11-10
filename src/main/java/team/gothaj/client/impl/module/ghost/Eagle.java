package team.gothaj.client.impl.module.ghost;

import java.awt.Color;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.math.MathUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.render.RenderOverlayEvent;
import team.gothaj.eventbus.Listen;

public class Eagle extends Module {

    private final BooleanProperty doShift = new BooleanProperty("Shift", true);
    private final NumberProperty shiftTimeMin = new NumberProperty("Shift Time Min (ms)", 140, 0, 500, 5);
    private final NumberProperty shiftTimeMax = new NumberProperty("Shift Time Max (ms)", 200, 0, 500, 5);
    private final BooleanProperty onSneakHold = new BooleanProperty("On Sneak Hold", false);
    private final BooleanProperty blocksOnly = new BooleanProperty("Blocks only", true);
    private final BooleanProperty shiftOnJump = new BooleanProperty("Shift during jumps", false);
    private final BooleanProperty lookDown = new BooleanProperty("Only when looking down", true);
    private final NumberProperty pitchMin = new NumberProperty("Pitch Min", 70, 0, 90, 1).setVisible(lookDown::getProperty);
    private final NumberProperty pitchMax = new NumberProperty("Pitch Max", 85, 0, 90, 1).setVisible(lookDown::getProperty);
    private final BooleanProperty showBlockCount = new BooleanProperty("Show block count", true);

    private boolean shouldBridge;
    private final TimerUtil shiftTimer = new TimerUtil();
    private int shiftDuration;

    public Eagle() {
        super("Eagle", "Sneaks at block edges.", ModuleCategory.GHOST);
        registerProperties(doShift, shiftTimeMin, shiftTimeMax, onSneakHold, blocksOnly, shiftOnJump,
                lookDown, pitchMin, pitchMax, showBlockCount);
    }

    @Override
    public void onEnable() {
        shouldBridge = false;
        shiftTimer.reset();
        super.onEnable();
    }

    @Listen
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!doShift.getProperty()) {
            if (mc.gameSettings.keyBindSneak.pressed && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                mc.gameSettings.keyBindSneak.pressed = false;
            }
            return;
        }

        if (lookDown.getProperty()) {
            float pitch = mc.thePlayer.rotationPitch;
            if (pitch < pitchMin.getProperty() || pitch > pitchMax.getProperty()) {
                shouldBridge = false;
                return;
            }
        }

        if (onSneakHold.getProperty()) {
            if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                shouldBridge = false;
                return;
            }
        }

        if (blocksOnly.getProperty()) {
            ItemStack item = mc.thePlayer.getHeldItem();
            if (item == null || !(item.getItem() instanceof ItemBlock)) {
                if (mc.gameSettings.keyBindSneak.pressed && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.gameSettings.keyBindSneak.pressed = false;
                }
                return;
            }
        }

        if (mc.thePlayer.onGround) {
            if (isOverEdge()) {
                int min = shiftTimeMin.getProperty().intValue();
                int maxTime = shiftTimeMax.getProperty().intValue();
                if (maxTime > 0) {
                    shiftDuration = MathUtil.getRandomNumber(min, maxTime);
                    shiftTimer.reset();
                }
                mc.gameSettings.keyBindSneak.pressed = true;
                shouldBridge = true;
            } else if (onSneakHold.getProperty() && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                mc.gameSettings.keyBindSneak.pressed = false;
                shouldBridge = false;
            } else if (mc.thePlayer.isSneaking()
                    && (shiftTimeMax.getProperty().intValue() == 0 || shiftTimer.hasTimeReached(shiftDuration))) {
                mc.gameSettings.keyBindSneak.pressed = false;
            }
        } else if (shouldBridge && shiftOnJump.getProperty() && isOverEdge()) {
            mc.gameSettings.keyBindSneak.pressed = true;
        } else {
            mc.gameSettings.keyBindSneak.pressed = false;
            shouldBridge = false;
        }
    }

    @Listen
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!showBlockCount.getProperty() || !shouldBridge || mc.thePlayer == null) return;

        int totalBlocks = getBlockCount();
        if (totalBlocks <= 0) return;

        int rgb;
        if (totalBlocks < 16) rgb = Color.red.getRGB();
        else if (totalBlocks < 32) rgb = Color.orange.getRGB();
        else if (totalBlocks < 128) rgb = Color.yellow.getRGB();
        else rgb = Color.green.getRGB();

        String text = totalBlocks + " blocks";
        ScaledResolution sr = event.getScaledResolution();
        int x = sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2;
        int y = sr.getScaledHeight() / 2 + 20;
        mc.fontRendererObj.drawStringWithShadow(text, x, y, rgb);
    }

    private int getBlockCount() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private boolean isOverEdge() {
        final double x = mc.thePlayer.posX;
        final double y = mc.thePlayer.posY;
        final double z = mc.thePlayer.posZ;
        final double w = mc.thePlayer.width / 2.0;

        final AxisAlignedBB bb = new AxisAlignedBB(
                x - w, y - 1.0, z - w,
                x + w, y, z + w);

        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.gameSettings.keyBindSneak.pressed = false;
        }
        shouldBridge = false;
        super.onDisable();
    }
}
