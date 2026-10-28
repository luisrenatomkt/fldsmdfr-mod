package com.fldsmdfr.core;

/** A built module remembered for verification and launch points (bounded window, see maxTrackedModules). */
public final class ModuleRecord {
    public final long index;
    public final long anchor; // BlockPos.toLong()
    public final int type;

    public ModuleRecord(long index, long anchor, int type) {
        this.index = index;
        this.anchor = anchor;
        this.type = type;
    }
}
