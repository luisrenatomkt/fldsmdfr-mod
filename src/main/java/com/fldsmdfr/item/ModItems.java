package com.fldsmdfr.item;

import java.util.ArrayList;
import java.util.List;

import com.fldsmdfr.core.FoodRegistry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModItems {
    /** Same order as {@link FoodRegistry}. */
    public static final List<Item> FOODS = new ArrayList<Item>();
    public static final List<String> NAMES = new ArrayList<String>();

    private ModItems() {}

    public static void register() {
        for (FoodRegistry.FoodDef d : FoodRegistry.all()) {
            Item item = new ItemFldsFood(d.hunger, d.saturation);
            item.setUnlocalizedName("fldsmdfr." + d.id);
            GameRegistry.registerItem(item, d.id);
            FOODS.add(item);
            NAMES.add(d.id);
        }
    }

    public static ItemStack stackFor(FoodRegistry.FoodDef def, int count) {
        int i = FoodRegistry.all().indexOf(def);
        if (i < 0 || i >= FOODS.size()) return null;
        return new ItemStack(FOODS.get(i), count);
    }
}
