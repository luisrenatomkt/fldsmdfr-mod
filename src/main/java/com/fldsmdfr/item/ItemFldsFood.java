package com.fldsmdfr.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemFood;

public class ItemFldsFood extends ItemFood {
    public ItemFldsFood(int hunger, float saturation) {
        super(hunger, saturation, false);
        setAlwaysEdible();
        setCreativeTab(CreativeTabs.tabFood);
    }
}
