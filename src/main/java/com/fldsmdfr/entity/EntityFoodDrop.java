package com.fldsmdfr.entity;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Food launched by the machine. Behaves as an item entity (can be picked up/eaten) but is counted and culled by the machine. */
public class EntityFoodDrop extends EntityItem {
    public EntityFoodDrop(World world) {
        super(world);
    }

    public EntityFoodDrop(World world, double x, double y, double z, ItemStack stack) {
        super(world, x, y, z, stack);
    }
}
