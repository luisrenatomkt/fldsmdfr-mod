package com.fldsmdfr.core;

import java.util.ArrayList;
import java.util.List;

import com.fldsmdfr.block.ModBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

/**
 * Block layouts of every module type. Footprint is at most 5x5 (7 for tubes/conductors, which
 * touch the neighbouring cell) and at most 7 blocks high. The anchor is always dx=dy=dz=0 and is
 * the FIRST block of the list. Variations (axis, arm direction) are derived from the module index.
 */
public final class Blueprint {
    private Blueprint() {}

    private static IBlockState white() { return Blocks.stained_hardened_clay.getStateFromMeta(0); }
    private static IBlockState orange() { return Blocks.stained_hardened_clay.getStateFromMeta(1); }
    private static IBlockState iron() { return Blocks.iron_block.getDefaultState(); }
    private static IBlockState quartz() { return Blocks.quartz_block.getDefaultState(); }
    private static IBlockState pillar() { return Blocks.quartz_block.getStateFromMeta(2); }
    private static IBlockState glass() { return Blocks.glass.getDefaultState(); }
    private static IBlockState lantern() { return Blocks.sea_lantern.getDefaultState(); }
    private static IBlockState bars() { return Blocks.iron_bars.getDefaultState(); }
    private static IBlockState slime() { return Blocks.slime_block.getDefaultState(); }

    private static void put(List<BlockSpec> l, int x, int y, int z, IBlockState s) { l.add(new BlockSpec(x, y, z, s, false)); }

    private static void opt(List<BlockSpec> l, int x, int y, int z, IBlockState s) { l.add(new BlockSpec(x, y, z, s, true)); }

    public static List<BlockSpec> build(ModuleType t, long index, long seed, boolean groundLevel) {
        List<BlockSpec> l = new ArrayList<BlockSpec>(96);
        put(l, 0, 0, 0, ModBlocks.anchor.getDefaultState());
        long h = Hash.mix(index ^ (seed * 31L));
        boolean alt = (h & 1L) != 0;
        switch (t) {
            case CHAMBER:
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0) put(l, x, 0, z, quartz());
                    put(l, x, 3, z, orange());
                    boolean edge = Math.abs(x) == 1 || Math.abs(z) == 1;
                    if (edge) {
                        boolean mid = (x == 0) || (z == 0);
                        put(l, x, 1, z, mid ? glass() : white());
                        put(l, x, 2, z, orange());
                    }
                }
                put(l, 0, 4, 0, bars());
                put(l, 0, 5, 0, bars());
                break;
            case TANK:
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0) put(l, x, 0, z, iron());
                    put(l, x, 4, z, iron());
                    for (int y = 1; y <= 3; y++) {
                        if (x == 0 && z == 0) put(l, 0, y, 0, slime());
                        else put(l, x, y, z, glass());
                    }
                }
                break;
            case TUBE:
                put(l, 0, 1, 0, quartz());
                for (int i = 1; i <= 3; i++) {
                    IBlockState s = (i == 3) ? lantern() : quartz();
                    put(l, i, 1, 0, s);
                    put(l, -i, 1, 0, s);
                    if (alt) { put(l, 0, 1, i, s); put(l, 0, 1, -i, s); }
                }
                break;
            case CONDUCTOR:
                for (int y = 1; y <= 4; y++) put(l, 0, y, 0, y == 4 ? iron() : bars());
                for (int i = 1; i <= 3; i++) {
                    IBlockState s = (i == 3) ? lantern() : iron();
                    if (alt) { put(l, 0, 4, i, s); put(l, 0, 4, -i, s); }
                    else { put(l, i, 4, 0, s); put(l, -i, 4, 0, s); }
                }
                break;
            case LAUNCHER:
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) put(l, x, 0, z, iron());
                for (int y = 1; y <= 3; y++) put(l, 0, y, 0, iron());
                put(l, 1, 1, 0, orange()); put(l, -1, 1, 0, orange());
                put(l, 0, 1, 1, orange()); put(l, 0, 1, -1, orange());
                put(l, 0, 4, 0, Blocks.dispenser.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.UP));
                break;
            case TURBINE:
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) put(l, x, 0, z, iron());
                put(l, 0, 1, 0, iron());
                put(l, 0, 2, 0, lantern());
                for (int i = 1; i <= 2; i++) {
                    if (alt) { put(l, i, 2, 0, iron()); put(l, -i, 2, 0, iron()); }
                    else { put(l, 0, 2, i, iron()); put(l, 0, 2, -i, iron()); }
                }
                put(l, 0, 3, 0, iron());
                put(l, 0, 4, 0, bars());
                break;
            case ARM: {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) put(l, x, 0, z, iron());
                for (int y = 1; y <= 3; y++) put(l, 0, y, 0, iron());
                int dx = alt ? 1 : 0, dz = alt ? 0 : 1;
                for (int i = 1; i <= 3; i++) put(l, dx * i, 3, dz * i, iron());
                put(l, dx * 3, 2, dz * 3, Blocks.sticky_piston.getDefaultState().withProperty(BlockPistonBase.FACING, EnumFacing.DOWN));
                break;
            }
            case TOWER:
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) put(l, x, 0, z, iron());
                for (int y = 1; y <= 5; y++) put(l, 0, y, 0, pillar());
                put(l, 0, 6, 0, lantern());
                put(l, 1, 3, 0, bars()); put(l, -1, 3, 0, bars());
                put(l, 0, 3, 1, bars()); put(l, 0, 3, -1, bars());
                break;
            case AUX_CORE:
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0) put(l, x, 0, z, iron());
                    put(l, x, 3, z, iron());
                    if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                        put(l, x, 1, z, white());
                        put(l, x, 2, z, orange());
                    }
                }
                put(l, 0, 2, 1, glass());
                put(l, 0, 1, 1, lantern());
                put(l, 0, 4, 0, lantern());
                break;
            case PLATFORM:
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                    if (x == 0 && z == 0) continue;
                    boolean border = Math.abs(x) == 2 || Math.abs(z) == 2;
                    put(l, x, 0, z, border ? orange() : iron());
                }
                put(l, 2, 1, 2, bars()); put(l, -2, 1, 2, bars());
                put(l, 2, 1, -2, bars()); put(l, -2, 1, -2, bars());
                break;
            default:
                break;
        }
        if (groundLevel) {
            // optional foundation columns (only where replaceable) so ground modules do not float
            IBlockState brick = Blocks.stonebrick.getDefaultState();
            int[][] corners = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
            for (int[] c : corners) for (int y = -1; y >= -8; y--) opt(l, c[0], y, c[1], brick);
        }
        return l;
    }

    /** Bounding box {minX,minY,minZ,maxX,maxY,maxZ} relative to the anchor. */
    public static int[] bounds(List<BlockSpec> specs) {
        int[] b = {0, 0, 0, 0, 0, 0};
        for (BlockSpec s : specs) {
            if (s.dx < b[0]) b[0] = s.dx;
            if (s.dy < b[1]) b[1] = s.dy;
            if (s.dz < b[2]) b[2] = s.dz;
            if (s.dx > b[3]) b[3] = s.dx;
            if (s.dy > b[4]) b[4] = s.dy;
            if (s.dz > b[5]) b[5] = s.dz;
        }
        return b;
    }
}
