package team.gothaj.client.impl.module.misc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockWeb;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.item.*;
import net.minecraft.world.WorldSettings;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.util.Utils;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.game.MovementUtil;
import team.gothaj.client.impl.event.input.MouseClickEvent;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.item.SpoofClientItemEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.player.NoFall;
import team.gothaj.client.main.GothajClient;
import team.gothaj.eventbus.Listen;

/**
 * @author vajzii
 * @since 2025
 */
public class AutoTool extends Module {

	private int lastSlot;
	private boolean swap;

	public final BooleanProperty spoof = new BooleanProperty("Spoof Item", true);

	public final ModeProperty spoofMode = new ModeProperty("Spoof Mode", "Server", "Client", "Server")
			.setVisible(spoof::getProperty);

	public final BooleanProperty autoBlock = new BooleanProperty("Auto Block", true);

	public final BooleanProperty weapon = new BooleanProperty("Auto Weapon", true);
	public final BooleanProperty ignoreOnKb = new BooleanProperty("Ignore On Kb", true);

	public AutoTool() {
		super("AutoTool", "", ModuleCategory.MISC);
		registerProperties(spoof, spoofMode, autoBlock, weapon, ignoreOnKb);
	}

	@Listen
	public void onSpoofItem(SpoofClientItemEvent event) {

		if (mc.thePlayer.getHeldItem() != null && (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword))
			return;

		if (spoof.getProperty() && spoofMode.is("Client") && swap && lastSlot != -1) {
			event.setItem(mc.thePlayer.inventory.getStackInSlot(lastSlot));
		}
	}

	@Listen
	public void onMouseClick(MouseClickEvent event) {
		if (ignoreOnKb.getProperty() && InventoryUtil.hasKb()) return;

		EntityLivingBase target = null;
		if (getCastedModule(KillAura.class).isToggled())
			target = EntityUtil.getTarget(KillAura.range.getProperty() + 0.4f);

		if (mc.gameSettings.keyBindAttack.pressed) {
			if (mc.objectMouseOver != null) {
				switch (mc.objectMouseOver.typeOfHit) {
				case BLOCK: {
					if (target != null || mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos())
							.getBlock() instanceof BlockAir)
						return;

					float toolSpeed = -1;
					int bestIndex = -1;
					Block targetBlock = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();

					for (int i = 36; i < 45; i++) {
						ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

						if (stack == null)
							continue;

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
					break;

				case ENTITY: {
					swapWeapon();
				}
					break;
				}
			}
		} else if (target != null) {
			swapWeapon();

		} else if (autoBlock.getProperty() && mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null
				&& mc.gameSettings.keyBindUseItem.pressed) {

			if ((mc.thePlayer.fallDistance > 3
					&& GothajClient.getInstance().getModuleManager().find(NoFall.class).isToggled())
					|| mc.playerController.getCurrentGameType() == WorldSettings.GameType.ADVENTURE
					|| mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() instanceof BlockAir
					|| (mc.thePlayer.inventory.getCurrentItem() != null && (mc.thePlayer.inventory.getCurrentItem().getItemUseAction().equals(EnumAction.BOW)
					|| InventoryUtil.isConsumable(mc.thePlayer.inventory.getCurrentItem()))
					|| mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemEgg
					|| mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFireball))
				return;

			int blockSlot = InventoryUtil.getBlockSlot();

			if (blockSlot != -1) {
				int bestIndex = blockSlot + 36;

				if (mc.thePlayer.inventory.currentItem != blockSlot) {
					if (!swap) {
						lastSlot = mc.thePlayer.inventory.currentItem;
						swap = true;
					}
					setSlot(bestIndex);
				}
			}

		} else if (swap) {
			setLastSlot();

		}
	}

	private boolean isShearableBlock(Block block) {
		return block instanceof BlockWeb || block.getUnlocalizedName().contains("wool")
				|| block.getUnlocalizedName().contains("leaves") || block.getUnlocalizedName().contains("vine");
	}

	private void swapWeapon() {

		if (!weapon.getProperty())
			return;

		float itemDamage = -1;
		int itemIndex = -1;

		for (int i = 36; i < 45; i++) {
			ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

			if (stack == null)
				continue;

			if (stack == InventoryUtil.bestWeapon(true)) {
				itemIndex = i;
			}
		}
		if (itemIndex != -1 && mc.thePlayer.inventory.currentItem != itemIndex - 36) {
			if (!swap) {
				lastSlot = mc.thePlayer.inventory.currentItem;
				swap = true;
			}
			setSlot(itemIndex);
		}

	}

	private void setLastSlot() {
		if (mc.thePlayer != null && lastSlot != -1 && mc.thePlayer.inventory.currentItem != lastSlot) {
			mc.thePlayer.inventory.currentItem = lastSlot;
		}
		swap = false;
		lastSlot = -1;
	}

	private void setSlot(int slot) {
		mc.thePlayer.inventory.currentItem = slot - 36;
	}

	@Override
	public void onDisable() {
		setLastSlot();
		super.onDisable();
	}

	@Override
	public void onEnable() {
		super.onEnable();
	}
}
