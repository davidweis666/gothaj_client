package team.gothaj.client.impl.module.ghost;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.eventbus.Listen;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {

    private final NumberProperty minCPS = new NumberProperty("MinCPS", 8, 1, 20, 1);
    private final NumberProperty maxCPS = new NumberProperty("MaxCPS", 12, 1, 20, 1);
    private final NumberProperty jitter = new NumberProperty("Jitter", 0, 0, 5, 1);
    private final BooleanProperty breakBlocks = new BooleanProperty("Break Blocks", false);
    private final BooleanProperty rightClicker = new BooleanProperty("Right Clicker", false);
    private final BooleanProperty disableInv = new BooleanProperty("Disable in Inventory", true);

    private final Random random = new Random();
    private long lastClick;
    private long hold;
    private double speed;
    private double holdLength;
    private double min, max;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks at a randomized CPS", ModuleCategory.GHOST);
        registerProperties(minCPS, maxCPS, jitter, breakBlocks, rightClicker, disableInv);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        update();
    }

    @Listen
    public void onClick(MouseClickEvent event) {
        if (mc.thePlayer == null || mc.thePlayer.isDead) return;

        if (disableInv.getProperty() && mc.currentScreen != null) {
            return;
        }

        if (Mouse.isButtonDown(0) && !mc.thePlayer.isUsingItem()) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();

            if (isLookingAtBlock() && breakBlocks.getProperty() && !(pointedEntity() instanceof EntityLivingBase)) {
                KeyBinding.setKeyBindState(key, true);
                update();
                return;
            }

            if (System.currentTimeMillis() - this.lastClick > this.speed * 1000.0) {
                this.lastClick = System.currentTimeMillis();
                if (this.hold < this.lastClick) {
                    this.hold = this.lastClick;
                }

                KeyBinding.setKeyBindState(key, true);
                KeyBinding.onTick(key);

                mc.thePlayer.rotationPitch += random.nextBoolean() ? random.nextFloat() * jitter.getProperty()
                        : -random.nextFloat() * jitter.getProperty();
                mc.thePlayer.rotationYaw += random.nextBoolean() ? random.nextFloat() * jitter.getProperty()
                        : -random.nextFloat() * jitter.getProperty();
                update();
            } else if (System.currentTimeMillis() - hold > holdLength * 1000.0) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                update();
            }
        }

        if (Mouse.isButtonDown(1) && rightClicker.getProperty() && mc.gameSettings.keyBindUseItem.isKeyDown()) {
            if (System.currentTimeMillis() - lastClick > speed * 1000.0) {
                this.lastClick = System.currentTimeMillis();
                if (hold < lastClick) {
                    hold = lastClick;
                }
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
            } else if (System.currentTimeMillis() - this.hold > this.holdLength * 1000.0) {
                update();
            }
        }
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private Entity pointedEntity() {
        return mc.objectMouseOver != null ? mc.objectMouseOver.entityHit : null;
    }

    private void update() {
        this.min = minCPS.getProperty();
        this.max = maxCPS.getProperty();
        if (this.min >= this.max) {
            this.max = this.min + 1.0;
        }
        this.speed = 1.0 / ThreadLocalRandom.current().nextDouble(this.min - 0.2, this.max);
        this.holdLength = this.speed / ThreadLocalRandom.current().nextDouble(this.min, this.max);
    }
}
