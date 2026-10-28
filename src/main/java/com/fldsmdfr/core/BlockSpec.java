package com.fldsmdfr.core;

import net.minecraft.block.state.IBlockState;

/** One block of a module blueprint, relative to the module anchor. */
public final class BlockSpec {
    public final int dx, dy, dz;
    public final IBlockState state;
    public final boolean optional; // optional blocks (foundations) are skipped when obstructed

    public BlockSpec(int dx, int dy, int dz, IBlockState state, boolean optional) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.state = state;
        this.optional = optional;
    }
}
