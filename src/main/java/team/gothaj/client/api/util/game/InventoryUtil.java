package team.gothaj.client.api.util.game;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.val;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.*;
import net.minecraft.util.EnumFacing;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;
import team.gothaj.client.api.util.math.TimerUtil;
import team.gothaj.client.api.util.network.PacketUtil;
import team.gothaj.client.impl.module.player.InvMove;
import team.gothaj.client.main.GothajClient;

public class InventoryUtil {
	public static Minecraft mc = Minecraft.getMinecraft();

	public static boolean isInventoryOpen;

	public static TimerUtil timer = new TimerUtil();

	public static List<Block> invalidBlocks = Arrays.asList(Blocks.enchanting_table, Blocks.carpet, Blocks.glass_pane,
			Blocks.ladder, Blocks.web, Blocks.stained_glass_pane, Blocks.iron_bars, Blocks.air, Blocks.water,
			Blocks.flowing_water, Blocks.lava, Blocks.ladder, Blocks.soul_sand, Blocks.ice, Blocks.sand,
			Blocks.flowing_lava, Blocks.snow_layer, Blocks.chest, Blocks.ender_chest, Blocks.torch, Blocks.anvil,
			Blocks.trapped_chest, Blocks.noteblock, Blocks.jukebox, Blocks.wooden_pressure_plate,
			Blocks.stone_pressure_plate, Blocks.light_weighted_pressure_plate, Blocks.heavy_weighted_pressure_plate,
			Blocks.stone_button, Blocks.tnt, Blocks.wooden_button, Blocks.lever, Blocks.crafting_table, Blocks.furnace,
			Blocks.stone_slab, Blocks.wooden_slab, Blocks.stone_slab2, Blocks.brown_mushroom, Blocks.red_mushroom,
			Blocks.gold_block, Blocks.red_flower, Blocks.yellow_flower, Blocks.flower_pot, Blocks.barrier);

	private static final Set<Item> WHITELISTED_ITEMS = new HashSet<>(Arrays.asList(Items.ender_pearl, Items.iron_ingot,
			Items.snowball, Items.gold_ingot, Items.redstone, Items.diamond, Items.emerald, Items.quartz, Items.bow,
			Items.arrow, Items.fishing_rod, Items.egg, Items.water_bucket, Items.lava_bucket));

	private static final Set<Integer> WHITELISTED_POTIONS = new HashSet<>(Arrays.asList(1, 5, 6, 8, 10, 12, 14, 16));

	public static int getBlockSlot() {
		int bestSlot = -1;
		int largestStack = 0;

		if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock heldBlock) {
			if (!invalidBlocks.contains(heldBlock.getBlock())
					|| !(mc.thePlayer.getHeldItem().getDisplayName().contains("lobby")
							|| mc.thePlayer.getHeldItem().getDisplayName().contains("hub"))) {
				int heldStack = mc.thePlayer.getHeldItem().stackSize;
				if (heldStack >= 1) {
					return mc.thePlayer.inventory.currentItem;
				}
			}
		}

		for (int i = 36; i < 45; ++i) {
			ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
			if (stack != null && stack.getItem() instanceof ItemBlock) {
				Block block = ((ItemBlock) stack.getItem()).getBlock();
				if (!invalidBlocks.contains(block) && stack.stackSize > 0) {
					if (stack.stackSize > largestStack) {
						largestStack = stack.stackSize;
						bestSlot = i - 36;
					}
				}
			}
		}

		return bestSlot;
	}

	public static boolean isValidBlock(Block block, boolean toPlace) {
		if (block instanceof BlockContainer)
			return false;
		if (toPlace) {
			return !(block instanceof BlockFalling) && block.isFullBlock() && block.isFullCube();
		} else {
			final Material material = block.getMaterial();
			return !material.isReplaceable() && !material.isLiquid();
		}
	}

	public static boolean isInventoryFull() {
		int i = mc.thePlayer.inventory.getSizeInventory();

		for (int j = 0; j < i; ++j) {
			ItemStack itemstack = mc.thePlayer.inventory.getStackInSlot(j);

			if (itemstack == null || itemstack.stackSize != itemstack.getMaxStackSize()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isInventoryEmpty(IInventory inventoryIn) {
		int j = inventoryIn.getSizeInventory();

		for (int k = 0; k < j; ++k) {
			ItemStack itemstack = inventoryIn.getStackInSlot(k);

			if (itemstack == null)
				continue;
			if (itemWhitelisted(itemstack)) {
				return false;
			}
		}

		return true;
	}

	public static boolean itemWhitelisted(ItemStack itemStack) {
		if (itemStack == null)
			return false;
		Item item = itemStack.getItem();
		String itemName = itemStack.getDisplayName();

		if (itemName.contains("Right Click") || itemName.contains("Click to Use")
				|| itemName.contains("Players Finder")) {
			return true;
		}

		if (item instanceof ItemPotion) {
			return WHITELISTED_POTIONS.contains(getPotionId(itemStack));
		}

		if (item instanceof ItemBlock ib) {
			return !(ib.getBlock() instanceof BlockTNT) && !(ib.getBlock() instanceof BlockSlime)
					&& !(ib.getBlock() instanceof BlockFalling);
		}

		return item instanceof ItemSword || item instanceof ItemArmor || item instanceof ItemTool
				|| item instanceof ItemFood || item instanceof ItemSkull || itemName.contains("\247")
				|| (WHITELISTED_ITEMS.contains(item) && !item.equals(Items.spider_eye));
	}

	private static int getPotionId(ItemStack potion) {
		if (potion.getItem() instanceof ItemPotion p) {
			try {
				return p.getEffects(potion.getMetadata()).getFirst().getPotionID();
			} catch (Throwable ignored) {
				return 0;
			}
		}
		return 0;
	}

	public static int getBlockCount() {
		int blockCount = 0;

		for (int i = 9; i < 45; i++) {
			final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

			if (stack != null && stack.getItem() instanceof ItemBlock
					&& !invalidBlocks.contains(((ItemBlock) stack.getItem()).getBlock())) {
				blockCount += stack.stackSize;
			}
		}
		return blockCount;
	}

	public static int findSlot(Item item) {
		ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
		for (int i = 0; i < inv.length; i++) {
			ItemStack s = inv[i];
			if (s != null && s.getItem() == item) {
				return i;
			}
		}
		return -1;
	}

	public static boolean isConsumable(ItemStack stack) {
		return stack.getItemUseAction().equals(EnumAction.DRINK) || stack.getItemUseAction().equals(EnumAction.EAT);
	}

	public static ItemStack getBlockSlotInventory() {
		ItemStack item = null;
		int stacksize = 0;

		if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() != null
				&& mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock
				&& !invalidBlocks.contains(((ItemBlock) mc.thePlayer.getHeldItem().getItem()).getBlock()))
			return mc.thePlayer.getHeldItem();
		for (int i = 9; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock
					&& !invalidBlocks.contains(
							((ItemBlock) mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem()).getBlock())
					&& mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize >= stacksize) {
				item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
		}

		return item;
	}

	public static int getBucketSlot() {
		int item = -1;
		int stacksize = 0;

		for (int i = 36; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() == Items.water_bucket)) {
				item = i - 36;
				stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
		}

		return item;
	}

	public static int getEmptyBucket() {
		int item = -1;
		int stacksize = 0;

		for (int i = 36; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() == Items.bucket)) {
				item = i - 36;
				stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
		}

		return item;
	}

	public static int getCobwebSlot() {
		int item = -1;
		int stacksize = 0;

		for (int i = 36; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock block)) {
				if (block.getBlock() == Blocks.web) {
					item = i - 36;
					stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
				}
			}
		}

		return item;
	}

	public static int getBlockSlot(Block slotBlock) {
		int item = -1;

		for (int i = 36; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock block)) {
				if (block.getBlock() == slotBlock) {
					item = i - 36;
				}
			}
		}

		return item;
	}

	public static boolean hasKb() {
		var heldItem = mc.thePlayer.getHeldItem();
		if (heldItem != null) {
			var knockbackLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, heldItem);
			return knockbackLevel > 0;
		}
		return false;
	}

	public static boolean hasTool() {
		var heldItem = mc.thePlayer.getHeldItem();
		if (heldItem != null) {
			return heldItem.getItem() instanceof ItemTool;
		}
		return false;
	}

	public static ItemStack getBucketSlotInventory() {
		ItemStack item = null;
		int stacksize = 0;

		for (int i = 9; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() == Items.water_bucket)) {
				item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
		}

		return item;
	}

	public static int getProjectileSlot() {
		int item = -1;
		int stacksize = 0;

		for (int i = 36; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemSnowball
							|| mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemEgg
							|| mc.thePlayer.inventoryContainer.getSlot(i).getStack()
									.getItem() instanceof ItemFishingRod)
					&& mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize >= stacksize) {
				item = i - 36;
				stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
		}

		return item;
	}

	public static ItemStack getProjectileSlotInventory() {
		ItemStack item = null;
		int stacksize = 0;

		for (int i = 9; i < 45; ++i) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null
					&& (mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemSnowball
							|| mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemEgg
							|| mc.thePlayer.inventoryContainer.getSlot(i).getStack()
									.getItem() instanceof ItemFishingRod)
					&& mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize >= stacksize) {
				item = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
				stacksize = mc.thePlayer.inventoryContainer.getSlot(i).getStack().stackSize;
			}
		}

		return item;
	}

	public static float getProtection(ItemStack stack) {
		float prot = 0.0F;

		if (stack.getItem() instanceof ItemArmor) {
			ItemArmor armor = (ItemArmor) stack.getItem();
			prot = (float) (prot + armor.damageReduceAmount + ((100 - armor.damageReduceAmount)
					* EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)) * 0.0075D);
			prot = (float) (prot
					+ EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) / 100.0D);
			prot = (float) (prot
					+ EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) / 100.0D);
			prot = (float) (prot + EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) / 100.0D);
			prot = (float) (prot
					+ EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 50.0D);
			prot = (float) (prot
					+ EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) / 100.0D);
		}

		return prot;
	}

	public static boolean isBestArmor(ItemStack stack, int type) {
		float prot = getProtection(stack);
		String strType = "";

		if (type == 1) {
			strType = "helmet";
		} else if (type == 2) {
			strType = "chestplate";
		} else if (type == 3) {
			strType = "leggings";
		} else if (type == 4) {
			strType = "boots";
		}

		if (!stack.getUnlocalizedName().contains(strType)) {
			return false;
		}

		for (int i = 5; i < 45; i++) {
			if ((Minecraft.getMinecraft()).thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				ItemStack is = (Minecraft.getMinecraft()).thePlayer.inventoryContainer.getSlot(i).getStack();

				if (getProtection(is) > prot && is.getUnlocalizedName().contains(strType)) {
					return false;
				}
			}
		}
		return true;
	}

	public static void drop(int slot) {
		(Minecraft.getMinecraft()).playerController.windowClick(
				(Minecraft.getMinecraft()).thePlayer.inventoryContainer.windowId, slot, 1, 4,
				(EntityPlayer) (Minecraft.getMinecraft()).thePlayer);
	}

	public static void shiftClick(int slot) {
		(Minecraft.getMinecraft()).playerController.windowClick(
				(Minecraft.getMinecraft()).thePlayer.inventoryContainer.windowId, slot, 0, 1,
				(EntityPlayer) (Minecraft.getMinecraft()).thePlayer);
	}

	public static boolean isBadStack(ItemStack is, boolean preferSword, boolean keepTools) {
		for (int type = 1; type < 5; type++) {
			String strType = "";

			if (type == 1) {
				strType = "helmet";
			} else if (type == 2) {
				strType = "chestplate";
			} else if (type == 3) {
				strType = "leggings";
			} else if (type == 4) {
				strType = "boots";
			}
			if (is.getItem() instanceof ItemArmor && !isBestArmor(is, type)
					&& is.getUnlocalizedName().contains(strType)) {
				return true;
			}
			if (mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack()
					&& isBestArmor(mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack(), type)
					&& mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack().getUnlocalizedName()
							.contains(strType)
					&& is.getUnlocalizedName().contains(strType)) {
				return true;
			}
		}
		if ((is.getItem() instanceof ItemSword) && is != bestWeapon(false) && !preferSword) {
			return true;
		}

		if (is.getItem() instanceof ItemSword && is != bestSword(false) && preferSword) {
			return true;
		}

		if (is.getItem() instanceof ItemBow && is != bestBow()) {
			return true;
		}

		if (keepTools) {
			if (is.getItem() instanceof ItemAxe && is != bestAxe() && (preferSword || is != bestWeapon(false))) {
				return true;
			}

			if (is.getItem() instanceof ItemPickaxe && is != bestPick() && (preferSword || is != bestWeapon(false))) {
				return true;
			}

			if (is.getItem() instanceof ItemSpade && is != bestShovel()) {
				return true;
			}
		} else {
			if (is.getItem() instanceof ItemAxe && (preferSword || is != bestWeapon(false))) {
				return true;
			}

			if (is.getItem() instanceof ItemPickaxe && (preferSword || is != bestWeapon(false))) {
				return true;
			}

			if (is.getItem() instanceof ItemSpade) {
				return true;
			}
		}

		return false;
	}

	public static ItemStack bestWeapon(boolean hotBar) {
		ItemStack bestWeapon = null;
		float itemDamage = -1;

		for (int i = hotBar ? 36 : 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				if (is.getItem() instanceof ItemSword || is.getItem() instanceof ItemAxe
						|| is.getItem() instanceof ItemPickaxe) {
					float toolDamage = getItemDamage(is);

					if (toolDamage >= itemDamage) {
						itemDamage = getItemDamage(is);
						bestWeapon = is;
					}
				}
			}
		}

		return bestWeapon;
	}

	public static ItemStack bestSword(boolean hotBar) {
		ItemStack bestSword = null;
		float itemDamage = -1;

		for (int i = hotBar ? 36 : 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				if (is.getItem() instanceof ItemSword) {
					float swordDamage = getItemDamage(is);

					if (swordDamage >= itemDamage) {
						itemDamage = getItemDamage(is);
						bestSword = is;
					}
				}
			}
		}

		return bestSword;
	}

	public static ItemStack bestBow() {
		ItemStack bestBow = null;
		float itemDamage = -1;

		for (int i = 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				if (is.getItem() instanceof ItemBow) {
					float bowDamage = getBowDamage(is);

					if (bowDamage >= itemDamage) {
						itemDamage = getBowDamage(is);
						bestBow = is;
					}
				}
			}
		}

		return bestBow;
	}

	public static ItemStack bestAxe() {
		ItemStack bestTool = null;
		float itemSkill = -1;

		for (int i = 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				if (is.getItem() instanceof ItemAxe) {
					float toolSkill = getToolRating(is);

					if (toolSkill >= itemSkill) {
						itemSkill = getToolRating(is);
						bestTool = is;
					}
				}
			}
		}

		return bestTool;
	}

	public static ItemStack bestPick() {
		ItemStack bestTool = null;
		float itemSkill = -1;

		for (int i = 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				if (is.getItem() instanceof ItemPickaxe) {
					float toolSkill = getToolRating(is);

					if (toolSkill >= itemSkill) {
						itemSkill = getToolRating(is);
						bestTool = is;
					}
				}
			}
		}

		return bestTool;
	}

	public static ItemStack bestShovel() {
		ItemStack bestTool = null;
		float itemSkill = -1;

		for (int i = 9; i < 45; i++) {
			if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
				final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

				if (is.getItem() instanceof ItemSpade) {
					float toolSkill = getToolRating(is);

					if (toolSkill >= itemSkill) {
						itemSkill = getToolRating(is);
						bestTool = is;
					}
				}
			}
		}

		return bestTool;
	}

	public static float getToolRating(ItemStack itemStack) {
		float damage = getToolMaterialRating(itemStack, false);
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack) * 2.00F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, itemStack) * 0.50F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemStack) * 0.50F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.10F;
		damage += (itemStack.getMaxDamage() - itemStack.getItemDamage()) * 0.000000000001F;
		return damage;
	}

	public static float getItemDamage(ItemStack itemStack) {
		float damage = getToolMaterialRating(itemStack, true);
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) * 0.50F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.01F;
		damage += (itemStack.getMaxDamage() - itemStack.getItemDamage()) * 0.000000000001F;

		if (itemStack.getItem() instanceof ItemSword) {
			damage += 0.2;
		}

		return damage;
	}

	public static float getBowDamage(ItemStack itemStack) {
		float damage = 5;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack) * 1.25F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, itemStack) * 0.75F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack) * 0.50F;
		damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.10F;
		damage += itemStack.getMaxDamage() - itemStack.getItemDamage() * 0.001F;
		return damage;
	}

	public static float getToolMaterialRating(ItemStack itemStack, boolean checkForDamage) {
		final Item is = itemStack.getItem();
		float rating = 0;

		if (is instanceof ItemSword) {
			switch (((ItemSword) is).getToolMaterialName()) {
			case "WOOD":
				rating = 4;
				break;

			case "GOLD":
				rating = 4;
				break;

			case "STONE":
				rating = 5;
				break;

			case "IRON":
				rating = 6;
				break;

			case "EMERALD":
				rating = 7;
				break;
			}
		} else if (is instanceof ItemPickaxe) {
			switch (((ItemPickaxe) is).getToolMaterialName()) {
			case "WOOD":
				rating = 2;
				break;

			case "GOLD":
				rating = 2;
				break;

			case "STONE":
				rating = 3;
				break;

			case "IRON":
				rating = checkForDamage ? 4 : 40;
				break;

			case "EMERALD":
				rating = checkForDamage ? 5 : 50;
				break;
			}
			;
		} else if (is instanceof ItemAxe) {
			switch (((ItemAxe) is).getToolMaterialName()) {
			case "WOOD":
				rating = 3;
				break;

			case "GOLD":
				rating = 3;
				break;

			case "STONE":
				rating = 4;
				break;

			case "IRON":
				rating = 5;
				break;

			case "EMERALD":
				rating = 6;
				break;
			}
			;
		} else if (is instanceof ItemSpade) {
			switch (((ItemSpade) is).getToolMaterialName()) {
			case "WOOD":
				rating = 1;
				break;

			case "GOLD":
				rating = 1;
				break;

			case "STONE":
				rating = 2;
				break;

			case "IRON":
				rating = 3;
				break;

			case "EMERALD":
				rating = 4;
				break;
			}
		}

		return rating;
	}

	public static void openInventory(String mode) {
		if (mode.equalsIgnoreCase("Spoof")) {

			if (!InventoryUtil.isInventoryOpen && !(mc.currentScreen instanceof GuiInventory)) {
				PacketUtil.send(new C16PacketClientStatus(EnumState.OPEN_INVENTORY_ACHIEVEMENT));
				InventoryUtil.isInventoryOpen = true;
			}
		}
	}

	public static void closeInventory(String mode) {
		if (mode.equalsIgnoreCase("Spoof")) {
			if (InventoryUtil.isInventoryOpen && !(mc.currentScreen instanceof GuiInventory)) {
				PacketUtil.send(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
				InventoryUtil.isInventoryOpen = false;
			}
		}
	}

	public static boolean isSword(ItemStack stack) {
		return stack.getItem() instanceof ItemSword;
	}

	public static boolean isToolOrFireballOrShears(ItemStack stack) {
		return stack.getItem() instanceof ItemPickaxe || stack.getItem() instanceof ItemAxe
				|| stack.getItem() == Items.fire_charge || stack.getItem() instanceof ItemShears;
	}

	public static int findBlockInInventory() {
		for (int i = 9; i < 36; i++) {
			ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
			if (stack != null && stack.getItem() instanceof ItemBlock) {
				return i;
			}
		}
		return -1;
	}

	public static int findStackInInventory(ItemStack stack) {
		if (stack == null)
			return -1;
		for (int i = 0; i < mc.thePlayer.inventory.mainInventory.length; i++) {
			if (mc.thePlayer.inventory.mainInventory[i] == stack) {
				return i;
			}
		}
		return -1;
	}

	public static void swapItems(int hotbarSlot, int invSlot) {
		mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, invSlot, hotbarSlot, 2, mc.thePlayer);
	}

	static KeyBinding[] moveKeys = new KeyBinding[] { mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
			mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump,
			mc.gameSettings.keyBindSneak };
}
