package com.fldsmdfr.block;

import net.minecraft.block.Block;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModBlocks {
    public static Block core;
    public static Block anchor;

    private ModBlocks() {}

    public static void register() {
        core = new BlockCore();
        anchor = new BlockAnchor();
        GameRegistry.registerBlock(core, "fldsmdfr_core");
        GameRegistry.registerBlock(anchor, "fldsmdfr_anchor");
    }
}
