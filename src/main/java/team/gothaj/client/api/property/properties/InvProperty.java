package team.gothaj.client.api.property.properties;

import team.gothaj.client.api.property.Property;
import team.gothaj.client.api.property.properties.inv.ItemType;

import java.util.HashMap;
import java.util.Map;

public class InvProperty extends Property<Map<ItemType, Integer>> {

    public InvProperty(String name) {
        super(name, createDefaults());
    }

    private static Map<ItemType, Integer> createDefaults() {
        Map<ItemType, Integer> defaults = new HashMap<>();
        defaults.put(ItemType.SWORD, 0);
        defaults.put(ItemType.PICKAXE, 1);
        defaults.put(ItemType.AXE, 2);
        defaults.put(ItemType.BLOCK, 3);
        defaults.put(ItemType.BUCKET, 4);
        defaults.put(ItemType.FOOD, 5);
        defaults.put(ItemType.BOW, 6);
        return defaults;
    }

    public int getSlot(ItemType type) {
        return this.getProperty().getOrDefault(type, -1);
    }

    public void setSlot(ItemType type, int slot) {
        this.getProperty().put(type, slot);
    }
}
