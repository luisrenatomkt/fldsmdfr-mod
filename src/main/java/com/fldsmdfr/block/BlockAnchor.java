package com.fldsmdfr.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Marker block placed at the origin of every module. The machine checks it to know a module is still functional. */
public class BlockAnchor extends Block {
    public BlockAnchor() {
        super(Material.iron);
        setUnlocalizedName("fldsmdfr.anchor");
        setHardness(4.0F);
        setResistance(20.0F);
        setStepSound(Block.soundTypeMetal);
        setLightLevel(0.5F);
    }
}
