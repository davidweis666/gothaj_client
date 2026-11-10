package team.gothaj.client.api.property.properties.inv;

import lombok.Getter;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;

import java.util.function.Predicate;

@Getter
public enum ItemType {

    SWORD("Sword", item -> item instanceof ItemSword, new ItemStack(Items.diamond_sword)),
    BOW("Bow", item -> item instanceof ItemBow, new ItemStack(Items.bow)),
    PICKAXE("Pickaxe", item -> item instanceof ItemPickaxe, new ItemStack(Items.diamond_pickaxe)),
    AXE("Axe", item -> item instanceof ItemAxe, new ItemStack(Items.diamond_axe)),
    FOOD("Food", item -> item instanceof ItemFood, new ItemStack(Items.cooked_beef)),
    BLOCK("Block", item -> item instanceof ItemBlock, new ItemStack(Blocks.stone)),
    PEARL("Ender Pearl", item -> item == Items.ender_pearl, new ItemStack(Items.ender_pearl)),
    BUCKET("Bucket", item -> item == Items.water_bucket, new ItemStack(Items.water_bucket));

    private final String name;
    private final Predicate<Item> validator;
    private final ItemStack itemStack;

    ItemType(String name, Predicate<Item> validator, ItemStack itemStack) {
        this.name = name;
        this.validator = validator;
        this.itemStack = itemStack;
    }

    public boolean matches(Item item) {
        return validator.test(item);
    }
}
