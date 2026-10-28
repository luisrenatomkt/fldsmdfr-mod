package com.fldsmdfr.core;

/**
 * Aggregated counts of functional modules and every value derived from them.
 * Derived values are recomputed only when a module is added or removed (dirty flag),
 * never per tick.
 */
public final class MachineStats {
    public static final double BASE_CORE_PRODUCTION = 3.0; // food/s per active core
    public static final double BASE_CORE_LAUNCH = 2.0;     // launched food/s per core

    public final long[] count = new long[ModuleType.values().length];
    public long[] chambersByFood = new long[FoodRegistry.count()];
    public long total;
    private boolean dirty = true;

    // derived
    public double cores = 1, integration = 1, capacityPerSec, launchPerSec, rangeBonus, bufferCap, instabilityLoad, stabilizer = 1;

    public void add(ModuleType t, int food) {
        count[t.ordinal()]++;
        total++;
        if (t == ModuleType.CHAMBER && food >= 0 && food < chambersByFood.length) chambersByFood[food]++;
        dirty = true;
    }

    public void remove(ModuleType t, int food) {
        if (count[t.ordinal()] > 0) {
            count[t.ordinal()]--;
            total--;
            if (t == ModuleType.CHAMBER && food >= 0 && food < chambersByFood.length && chambersByFood[food] > 0) chambersByFood[food]--;
            dirty = true;
        }
    }

    public void markDirty() { dirty = true; }

    private static double ratio(double have, double need) {
        return need <= 0 ? 0 : Math.min(1.0, have / need);
    }

    public void recompute() {
        if (!dirty) return;
        dirty = false;
        double ch = count[ModuleType.CHAMBER.ordinal()];
        double tk = count[ModuleType.TANK.ordinal()];
        double tube = count[ModuleType.TUBE.ordinal()];
        double arm = count[ModuleType.ARM.ordinal()];
        double l = count[ModuleType.LAUNCHER.ordinal()];
        double turb = count[ModuleType.TURBINE.ordinal()];
        double cond = count[ModuleType.CONDUCTOR.ordinal()];
        double tower = count[ModuleType.TOWER.ordinal()];
        double plat = count[ModuleType.PLATFORM.ordinal()];

        cores = 1 + count[ModuleType.AUX_CORE.ordinal()];
        double raw = cores * BASE_CORE_PRODUCTION + ch * ModuleType.CHAMBER.production;
        // Explicit integration bonus: complementary modules, bounded to x1.75.
        integration = 1.0 + 0.30 * ratio(tk, ch * 0.5) + 0.20 * ratio(tube, ch) + 0.25 * ratio(arm, ch * 0.5);
        capacityPerSec = raw * integration;

        launchPerSec = cores * BASE_CORE_LAUNCH
                + l * ModuleType.LAUNCHER.launch * (1.0 + 0.35 * ratio(turb, l) + 0.20 * ratio(cond, turb));
        rangeBonus = 3.0 * Math.log(1.0 + l) + 5.0 * Math.log(1.0 + tower);
        bufferCap = 500.0 + tk * 200.0 + ch * 20.0;

        double load = 0;
        ModuleType[] vs = ModuleType.values();
        for (int i = 0; i < vs.length; i++) load += count[i] * vs[i].instability;
        instabilityLoad = load + cores * 0.5;
        stabilizer = 1.0 / (1.0 + 0.1 * Math.log(1.0 + plat));
    }
}
