package team.gothaj.client.impl.module.player;

import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import team.gothaj.client.api.module.Module;
import team.gothaj.client.api.module.ModuleCategory;
import team.gothaj.client.api.property.properties.BooleanProperty;
import team.gothaj.client.api.property.properties.ModeProperty;
import team.gothaj.client.api.property.properties.NumberProperty;
import team.gothaj.client.api.util.game.EntityUtil;
import team.gothaj.client.api.util.game.InventoryUtil;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.impl.event.player.LivingUpdateEvent;
import team.gothaj.client.impl.event.player.move.MoveInputEvent;
import team.gothaj.client.impl.module.combat.KillAura;
import team.gothaj.client.impl.module.world.Scaffold;
import team.gothaj.eventbus.Listen;

import java.util.Arrays;

public class Manager extends Module {

	private final TimerUtil startTimer = new TimerUtil(), swapTimer = new TimerUtil();

	private boolean wasScaffoldEnabled = false;
	private final ItemStack[] originalHotbar = new ItemStack[9];

	private final ModeProperty mode = new ModeProperty("Mode", "Open Inventory", "Open Inventory", "Spoof");
	private final BooleanProperty stopOnSpoof = new BooleanProperty("Stop On Spoof", true)
			.setVisible(() -> mode.is("Spoof"));
	private final BooleanProperty throwGarbage = new BooleanProperty("Throw Garbage", true);
	private final BooleanProperty autoArmor = new BooleanProperty("Auto Armor", true);
	private final NumberProperty startDelay = new NumberProperty("Start Delay", 150, 0, 1000, 1)
			.setVisible(() -> mode.is("Open Inventory"));
	private final NumberProperty speed = new NumberProperty("Speed", 150, 0, 1000, 1);
	private final BooleanProperty sword = new BooleanProperty("Sword", true);
	private final NumberProperty swordSlot = new NumberProperty("Sword Slot", 1, 1, 9, 1)
			.setVisible(sword::getProperty);
	private final BooleanProperty axe = new BooleanProperty("Axe", true);
	private final NumberProperty axeSlot = new NumberProperty("Axe Slot", 2, 1, 9, 1).setVisible(axe::getProperty);
	private final BooleanProperty pickaxe = new BooleanProperty("Pickaxe", true);
	private final NumberProperty pickaxeSlot = new NumberProperty("Pickaxe Slot", 3, 1, 9, 1)
			.setVisible(pickaxe::getProperty);
	private final BooleanProperty shovel = new BooleanProperty("Shovel", false);
	private final NumberProperty shovelSlot = new NumberProperty("Shovel Slot", 4, 1, 9, 1)
			.setVisible(shovel::getProperty);
	private final BooleanProperty bow = new BooleanProperty("Bow", false);
	private final NumberProperty bowSlot = new NumberProperty("Bow Slot", 5, 1, 9, 1).setVisible(bow::getProperty);
	private final BooleanProperty blocks = new BooleanProperty("Blocks", true);
	private final NumberProperty blockSlot = new NumberProperty("Block Slot", 6, 1, 9, 1)
			.setVisible(blocks::getProperty);
	private final BooleanProperty projectiles = new BooleanProperty("Projectiles", true);
	private final NumberProperty projectileSlot = new NumberProperty("Projectile Slot", 7, 1, 9, 1)
			.setVisible(projectiles::getProperty);
	private final BooleanProperty waterBucket = new BooleanProperty("Water Bucket", true);
	private final NumberProperty waterBucketSlot = new NumberProperty("Water Bucket Slot", 8, 1, 9, 1)
			.setVisible(waterBucket::getProperty);

	public Manager() {
		super("Manager", "Manages your inventory.", ModuleCategory.PLAYER);
		registerProperties(mode, stopOnSpoof, throwGarbage, autoArmor, startDelay, speed, sword, swordSlot, axe, axeSlot, pickaxe,
				pickaxeSlot, shovel, shovelSlot, bow, bowSlot, blocks, blockSlot, projectiles, projectileSlot,
				waterBucket, waterBucketSlot);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		InventoryUtil.closeInventory(mode.getProperty());
		wasScaffoldEnabled = false;
		Arrays.fill(originalHotbar, null);
	}

	@Override
	public String getSuffix() {
		return mode.getProperty();
	}

	/**
	 * Movement protection when "intave" is enabled and inventory is open. This
	 * mirrors the Intave protection in the example: when the inventory is open we
	 * zero movement input so the player can't move/jump/sneak.
	 */
	@Listen
	public void onMoveInput(MoveInputEvent e) {
		if (InventoryUtil.isInventoryOpen && stopOnSpoof.getProperty() && !getCastedModule(InvMove.class).isToggled()) {
			e.setKeyForward(false);
			e.setKeyBack(false);
			e.setKeyLeft(false);
			e.setKeyRight(false);
			e.setKeyJump(false);
			e.setKeySneak(false);
		}
	}

	/**
	 * Main inventory manager update. Keeps the behavior and API from the "new"
	 * version, but follows the (cleaner) structure from the example module.
	 */
	@Listen
	public void onUpdate(LivingUpdateEvent e) {
		if (mode.is("Spoof") && mc.currentScreen instanceof GuiChest) {
			return;
		}

		// Handle startDelay when Mode == "Open Inventory"
		if (mode.is("Open Inventory")) {
			if (mc.currentScreen == null) {
				startTimer.reset();
			}
			if (!startTimer.hasTimeReached(startDelay.getProperty().longValue(), false)) {
				return;
			}
		}

		// If Spoof mode and we have a target or the screen is open, close inventories
		// and bail
		EntityLivingBase target = EntityUtil.getTarget(KillAura.interactRange.getProperty() + 2);

		if (this.mode.getProperty().equalsIgnoreCase("Spoof") && target != null) {
			InventoryUtil.closeInventory(mode.getProperty());
			return;
		}

		// If Open Inv mode but current screen isn't the player inventory, do nothing
		if (mode.is("Open Inventory") && !(mc.currentScreen instanceof GuiInventory))
			return;

		// Track scaffold enable/disable to perform hotbar replacements/restores
		boolean scaffoldEnabled = getCastedModule(Scaffold.class).isToggled();

		if (swapTimer.hasTimeReached(55, false)) {
			if (scaffoldEnabled && !wasScaffoldEnabled) {
				// Save original hotbar then fill with blocks (except sword)
				for (int i = 0; i < 9; i++) {
					originalHotbar[i] = InventoryUtil.mc.thePlayer.inventory.mainInventory[i];
				}
				InventoryUtil.openInventory(mode.getProperty());
				fillHotbarWithBlocksExceptSword();
			} else if (!scaffoldEnabled && wasScaffoldEnabled) {
				// Restore the original hotbar items
				InventoryUtil.openInventory(mode.getProperty());
				restoreHotbarItems();
			}

			wasScaffoldEnabled = scaffoldEnabled;

			InventoryUtil.closeInventory(mode.getProperty());
			swapTimer.reset();
		}

		if(scaffoldEnabled)return;

		if (autoArmor.getProperty() && handleAutoArmor(speed.getProperty().longValue())) {
			return;
		}
		
		for (int i = 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				final long speed = this.speed.getProperty().longValue();
				final int swordSlot = this.swordSlot.getProperty().intValue();
				final int bowSlot = this.bowSlot.getProperty().intValue();
				final int pickaxeSlot = this.pickaxeSlot.getProperty().intValue();
				final int axeSlot = this.axeSlot.getProperty().intValue();
				final int shovelSlot = this.shovelSlot.getProperty().intValue();
				final int blockSlot = this.blockSlot.getProperty().intValue();
				final int projectileSlot = this.projectileSlot.getProperty().intValue();
				final int waterBucketSlot = this.waterBucketSlot.getProperty().intValue();

				if (InventoryUtil.timer.hasTimeReached(speed, false)) {
					// Sword
					if (swordSlot != 0 && is.getItem() instanceof ItemSword && is == InventoryUtil.bestSword(false)
							&& mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtil.bestSword(false))
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + swordSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + swordSlot).getStack() != is)
							&& sword.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, swordSlot - 1, 2,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
					// Bow
					if (bowSlot != 0 && is.getItem() instanceof ItemBow && is == InventoryUtil.bestBow()
							&& mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtil.bestBow())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + bowSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + bowSlot).getStack() != is)
							&& bow.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, bowSlot - 1, 2,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
					// Pickaxe
					if (pickaxeSlot != 0 && is.getItem() instanceof ItemPickaxe && is == InventoryUtil.bestPick()
							&& is != InventoryUtil.bestWeapon(false)
							&& mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtil.bestPick())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + pickaxeSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + pickaxeSlot).getStack() != is)
							&& pickaxe.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, pickaxeSlot - 1, 2,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
					// Axe
					else if (axeSlot != 0 && is.getItem() instanceof ItemAxe && is == InventoryUtil.bestAxe()
							&& is != InventoryUtil.bestWeapon(false)
							&& mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtil.bestAxe())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + axeSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + axeSlot).getStack() != is)
							&& axe.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, axeSlot - 1, 2,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())

							InventoryUtil.closeInventory(mode.getProperty());
						InventoryUtil.timer.reset();
						if (speed != 0)
							break;

					}
					// Shovel
					if (shovelSlot != 0 && is.getItem() instanceof ItemSpade && is == InventoryUtil.bestShovel()
							&& is != InventoryUtil.bestWeapon(false)
							&& mc.thePlayer.inventoryContainer.getInventory().contains(InventoryUtil.bestShovel())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + shovelSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + shovelSlot).getStack() != is)
							&& shovel.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, shovelSlot - 1, 2,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
					// Blocks
					if (blockSlot != 0 && is.getItem() instanceof ItemBlock
							&& is == InventoryUtil.getBlockSlotInventory()
							&& mc.thePlayer.inventoryContainer.getInventory()
									.contains(InventoryUtil.getBlockSlotInventory())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + blockSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + blockSlot).getStack() != is)
							&& (((sword.getProperty()
									&& is != mc.thePlayer.inventoryContainer.getSlot(35 + swordSlot).getStack()))
									&& (axe.getProperty()
											&& is != mc.thePlayer.inventoryContainer.getSlot(35 + axeSlot).getStack())
									&& (pickaxe.getProperty() && is != mc.thePlayer.inventoryContainer
											.getSlot(35 + pickaxeSlot).getStack())
									&& (shovel.getProperty() && is != mc.thePlayer.inventoryContainer
											.getSlot(35 + shovelSlot).getStack())
									&& (bow.getProperty()
											&& is != mc.thePlayer.inventoryContainer.getSlot(35 + bowSlot).getStack()))
							&& blocks.getProperty()) {
						if (mc.thePlayer.inventoryContainer.getSlot(35 + blockSlot).getStack() != null
								&& mc.thePlayer.inventoryContainer.getSlot(35 + blockSlot).getStack()
										.getItem() instanceof ItemBlock
								&& !InventoryUtil.invalidBlocks.contains(((ItemBlock) mc.thePlayer.inventoryContainer
										.getSlot(35 + blockSlot).getStack().getItem()).getBlock()))
							break;

						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, blockSlot - 1, 2,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
					// Projectiles
					if (projectileSlot != 0 && is == InventoryUtil.getProjectileSlotInventory()
							&& mc.thePlayer.inventoryContainer.getInventory()
									.contains(InventoryUtil.getProjectileSlotInventory())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + projectileSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + projectileSlot).getStack() != is)
							&& projectiles.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, projectileSlot - 1,
								2, mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
					// Water bucket
					if (waterBucketSlot != 0 && is.getItem() == Items.water_bucket
							&& is == InventoryUtil.getBucketSlotInventory()
							&& mc.thePlayer.inventoryContainer.getInventory()
									.contains(InventoryUtil.getBucketSlotInventory())
							&& (mc.thePlayer.inventoryContainer.getSlot(35 + waterBucketSlot).getStack() == null
									|| mc.thePlayer.inventoryContainer.getSlot(35 + waterBucketSlot).getStack() != is)
							&& waterBucket.getProperty()) {
						if (mc.thePlayer.inventoryContainer.getSlot(35 + waterBucketSlot).getStack() != null
								&& mc.thePlayer.inventoryContainer.getSlot(35 + waterBucketSlot).getStack()
										.getItem() == Items.water_bucket)
							break;
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i,
								waterBucketSlot - 1, 2, mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;

					}
					// Throw garbage
					if (InventoryUtil.isBadStack(is, true, true) && throwGarbage.getProperty()) {
						InventoryUtil.openInventory(mode.getProperty());
						mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, i, 1, 4,
								mc.thePlayer);
						if (!stopOnSpoof.getProperty())
							InventoryUtil.closeInventory(mode.getProperty());

						InventoryUtil.timer.reset();
						if (speed != 0)
							break;
					}
				}
			}
		}
		if (InventoryUtil.timer.getTime() > speed.getProperty() + 75) {
			InventoryUtil.closeInventory(mode.getProperty());
		}
	}

	private boolean handleAutoArmor(long speed) {
		if (!InventoryUtil.timer.hasTimeReached(speed, false)) {
			return false;
		}

		for (int type = 1; type < 5; type++) {
			if (mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack()) {
				ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack();

				if (InventoryUtil.isBestArmor(stack, type)) {
					continue;
				}

				InventoryUtil.openInventory(mode.getProperty());
				InventoryUtil.drop(4 + type);
				if (!stopOnSpoof.getProperty()) {
					InventoryUtil.closeInventory(mode.getProperty());
				}

				InventoryUtil.timer.reset();
				return true;
			}
		}

		for (int type = 1; type < 5; type++) {
			for (int i = 9; i < 45; i++) {
				if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
					continue;
				}

				ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				if (InventoryUtil.getProtection(stack) <= 0.0F) {
					continue;
				}

				if (InventoryUtil.isBestArmor(stack, type) && !InventoryUtil.isBadStack(stack, true, true)) {
					InventoryUtil.openInventory(mode.getProperty());
					InventoryUtil.shiftClick(i);
					if (!stopOnSpoof.getProperty()) {
						InventoryUtil.closeInventory(mode.getProperty());
					}

					InventoryUtil.timer.reset();
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Fill the hotbar with blocks (except the sword) when scaffold is enabled. Kept
	 * exactly as requested (logic preserved).
	 */
	private void fillHotbarWithBlocksExceptSword() {
		for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			ItemStack stack = InventoryUtil.mc.thePlayer.inventory.mainInventory[hotbarSlot];
			if (stack != null && InventoryUtil.isSword(stack))
				continue;
			if (stack == null || InventoryUtil.isToolOrFireballOrShears(stack)
					|| !(stack.getItem() instanceof ItemBlock)) {
				int blockSlot = InventoryUtil.findBlockInInventory();
				if (blockSlot != -1) {
					InventoryUtil.swapItems(hotbarSlot, blockSlot);
				}
			}
		}
	}

	/**
	 * Restore previously saved hotbar items after scaffold is disabled. Kept
	 * exactly as requested (logic preserved).
	 */
	private void restoreHotbarItems() {
		for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			ItemStack original = originalHotbar[hotbarSlot];
			ItemStack current = InventoryUtil.mc.thePlayer.inventory.mainInventory[hotbarSlot];
			if (original == null)
				continue;
			if (current == null || !current.isItemEqual(original) || current.getMetadata() != original.getMetadata()) {
				int origSlot = InventoryUtil.findStackInInventory(original);
				if (origSlot != -1) {
					InventoryUtil.swapItems(hotbarSlot, origSlot);
				}
			}
		}
		Arrays.fill(originalHotbar, null);
	}
}
