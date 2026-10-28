package com.fldsmdfr.core;

/**
 * Deterministic index -> cell mapping. Cells are grouped in concentric "shells" (= sectors).
 * Shell k holds all cells with horizontal Chebyshev radius k. The vertical extent grows with k
 * until VMAX layers, after which the machine only grows horizontally (world height is finite).
 * Order inside a shell is bottom-up, so supports always exist before upper cells.
 * The mapping is a bijection: no index maps to two cells and no cell to two indices.
 */
public final class StructureLayout {
    public static final int CELL = 7;
    public static final int VMAX = 12;
    public static final long MAX_SHELL = 1_000_000L;

    private StructureLayout() {}

    public static final class Cell {
        public final long shell;
        public final int cx, cy, cz;

        Cell(long shell, int cx, int cy, int cz) {
            this.shell = shell;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
        }
    }

    /** Number of cells in the box of shell radius k (including the core cell). */
    public static long boxCells(long k) {
        if (k < 0) return 0;
        long s = 2 * k + 1;
        return s * s * (Math.min(k, VMAX) + 1);
    }

    /** Highest usable module index. */
    public static long maxIndex() {
        return boxCells(MAX_SHELL) - 2;
    }

    public static Cell cellOf(long index) {
        if (index < 0) index = 0;
        if (index > maxIndex()) index = maxIndex();
        long q = index + 1; // cell 0 is the core
        long lo = 1, hi = MAX_SHELL;
        while (lo < hi) {
            long mid = (lo + hi) >>> 1;
            if (boxCells(mid) > q) hi = mid; else lo = mid + 1;
        }
        long k = lo;
        long idx = q - boxCells(k - 1);
        int ki = (int) k;
        int ring = 8 * ki;
        int x, y, z;
        if (k <= VMAX) {
            long ringTotal = (long) ring * ki;
            if (idx < ringTotal) {
                y = (int) (idx / ring);
                int[] p = perimeter((int) (idx % ring), ki);
                x = p[0]; z = p[1];
            } else {
                long j = idx - ringTotal;
                int side = 2 * ki + 1;
                x = -ki + (int) (j % side);
                z = -ki + (int) (j / side);
                y = ki;
            }
        } else {
            y = (int) (idx / ring);
            int[] p = perimeter((int) (idx % ring), ki);
            x = p[0]; z = p[1];
        }
        return new Cell(k, x, y, z);
    }

    private static int[] perimeter(int t, int k) {
        int side = t / (2 * k);
        int u = t % (2 * k);
        switch (side) {
            case 0: return new int[]{-k + u, -k};
            case 1: return new int[]{k, -k + u};
            case 2: return new int[]{k - u, k};
            default: return new int[]{-k, k - u};
        }
    }

    public static long sectorOf(long index) { return cellOf(index).shell; }

    /** Approximate footprint {width, height} in blocks for a machine that has used `modules` indices. */
    public static long[] extents(long modules) {
        if (modules <= 0) return new long[]{CELL, CELL};
        long k = cellOf(modules - 1).shell;
        return new long[]{(2 * k + 1) * CELL, (Math.min(k, VMAX) + 1) * CELL};
    }
}
