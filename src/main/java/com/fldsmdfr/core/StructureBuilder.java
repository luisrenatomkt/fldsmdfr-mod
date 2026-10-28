package com.fldsmdfr.core;

import java.util.List;

import com.fldsmdfr.block.ModBlocks;
import com.fldsmdfr.config.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Turns the logical module target into blocks, a few blocks per update. Never destroys solid
 * blocks (only air, replaceable blocks, liquids, leaves and plants), never builds in unloaded
 * chunks, and never builds an index twice.
 */
public final class StructureBuilder {
    private static final int MAX_ITER = 64;

    private StructureBuilder() {}

    /** Runs one build step. Returns the number of blocks placed. */
    public static int update(World w, BlockPos core, MachineState st) {
        int budget = (int) Math.max(1, Math.min(5000, ModConfig.i("maxBlocksPerUpdate") * ModConfig.d("expansionSpeed")));
        int strategy = ModConfig.i("obstacleStrategy");
        long now = w.getTotalWorldTime();
        int placed = 0;
        int iter = 0;
        while (budget > 0 && iter++ < MAX_ITER) {
            if (st.curIndex < 0) {
                if (!startNext(w, core, st, now, strategy)) break;
                continue;
            }
            ensureSpecs(st);
            BlockPos anchor = BlockPos.fromLong(st.curAnchor);
            List<BlockSpec> specs = st.curSpecs;
            boolean blocked = false;
            while (budget > 0 && st.curStep < specs.size()) {
                BlockSpec s = specs.get(st.curStep);
                BlockPos p = anchor.add(s.dx, s.dy, s.dz);
                if (!w.isBlockLoaded(p)) { blocked = true; break; }
                IBlockState cur = w.getBlockState(p);
                if (cur != s.state && canReplace(w, p, cur)) {
                    w.setBlockState(p, s.state, 2);
                    budget--;
                    placed++;
                }
                st.curStep++;
            }
            if (blocked) break;
            if (st.curStep >= specs.size()) finish(w, st, anchor);
        }
        return placed;
    }

    private static void ensureSpecs(MachineState st) {
        if (st.curSpecs != null) return;
        st.curType = ModuleType.of(st.curIndex, st.seed);
        st.curSpecs = Blueprint.build(st.curType, st.curIndex, st.seed, StructureLayout.cellOf(st.curIndex).cy == 0);
        st.curSkip = ModConfig.i("obstacleStrategy") == 0;
    }

    private static void finish(World w, MachineState st, BlockPos anchor) {
        if (w.getBlockState(anchor).getBlock() == ModBlocks.anchor) st.onModuleBuilt(st.curType, st.curIndex, st.curAnchor);
        else st.abandoned++;
        st.curIndex = -1;
        st.curStep = 0;
        st.curSpecs = null;
    }

    private static boolean startNext(World w, BlockPos core, MachineState st, long now, int strategy) {
        long index;
        int retries = 0;
        MachineState.Deferred ready = null;
        for (int i = 0; i < st.deferred.size(); i++) {
            if (st.deferred.get(i).notBefore <= now) { ready = st.deferred.remove(i); break; }
        }
        if (ready != null) {
            index = ready.index;
            retries = ready.retries;
        } else if (ModConfig.b("autoBuild") && st.nextIndex < st.targetModules()) {
            index = st.nextIndex++;
        } else {
            return false;
        }

        ModuleType type = ModuleType.of(index, st.seed);
        StructureLayout.Cell cell = StructureLayout.cellOf(index);
        BlockPos anchor = core.add(cell.cx * StructureLayout.CELL, cell.cy * StructureLayout.CELL, cell.cz * StructureLayout.CELL);
        List<BlockSpec> specs = Blueprint.build(type, index, st.seed, cell.cy == 0);

        int shift = 0;
        int state = check(w, specs, anchor);          // 0 free, 1 obstructed, 2 not loaded, 3 out of height
        if (state == 3) { st.abandoned++; return true; }
        if (state == 1 && strategy == 1) {
            for (int s = 1; s <= 7; s++) {
                if (check(w, specs, anchor.up(s)) == 0) { shift = s; state = 0; break; }
            }
        }
        if (state == 2 || (state == 1 && !(strategy == 0 && isAnchorFree(w, anchor)))) {
            defer(st, index, retries, now);
            return true;
        }
        st.curIndex = index;
        st.curStep = 0;
        st.curAnchor = anchor.up(shift).toLong();
        st.curType = type;
        st.curSpecs = specs;
        st.curSkip = strategy == 0;
        return true;
    }

    private static void defer(MachineState st, long index, int retries, long now) {
        if (retries + 1 > ModConfig.i("maxDeferredRetries")) {
            st.abandoned++;
            return;
        }
        st.deferred.add(new MachineState.Deferred(index, retries + 1, now + 200L * (retries + 1)));
    }

    private static boolean isAnchorFree(World w, BlockPos anchor) {
        return canReplace(w, anchor, w.getBlockState(anchor));
    }

    /** 0 = free, 1 = obstructed by non-replaceable block, 2 = area not loaded, 3 = outside world height. */
    private static int check(World w, List<BlockSpec> specs, BlockPos anchor) {
        int[] b = Blueprint.bounds(specs);
        BlockPos min = anchor.add(b[0], b[1], b[2]);
        BlockPos max = anchor.add(b[3], b[4], b[5]);
        if (min.getY() < 1 || max.getY() > 254) return 3;
        if (!w.isAreaLoaded(min, max)) return 2;
        for (BlockSpec s : specs) {
            if (s.optional) continue;
            BlockPos p = anchor.add(s.dx, s.dy, s.dz);
            IBlockState cur = w.getBlockState(p);
            if (cur != s.state && !canReplace(w, p, cur)) return 1;
        }
        return 0;
    }

    /** Only air, replaceable blocks, liquids, leaves and plants may be overwritten. */
    public static boolean canReplace(World w, BlockPos p, IBlockState cur) {
        Block b = cur.getBlock();
        if (b.isAir(w, p) || b.isReplaceable(w, p)) return true;
        Material m = b.getMaterial();
        return m.isLiquid() || m == Material.leaves || m == Material.plants || m == Material.vine;
    }
}
