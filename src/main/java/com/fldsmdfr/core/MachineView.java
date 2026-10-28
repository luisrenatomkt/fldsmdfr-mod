package com.fldsmdfr.core;

import net.minecraft.nbt.NBTTagCompound;

/** Small snapshot sent to clients (a few numbers, once per syncInterval). Used by the GUI and the renderer. */
public final class MachineView {
    public long runtime, modules, target, nextIndex, launchers, abandoned;
    public long pending, width, height, shell;
    public BigNum evolution = BigNum.ONE, produced = BigNum.ZERO;
    public double instability, capacity;
    public int surge, activeFood, deferred, maxActive, facing;
    public boolean running = true;

    /** Instability saturated to [0,1). */
    public double sat() { return instability / (1.0 + instability); }

    public String stageName() {
        double lg = evolution.log10();
        if (lg < 1) return "Prototipo compacto";
        if (lg < 3) return "Cozinha em expansao";
        if (lg < 6) return "Complexo industrial";
        if (lg < 12) return "Colosso mecanico";
        if (lg < 30) return "Catastrofe alimentar";
        return "Singularidade culinaria";
    }

    public String stateName() {
        if (!running) return "Desligada";
        if (surge > 0) return "SUPERPRODUCAO";
        if (sat() > 0.8) return "Instabilidade critica";
        if (pending > 0) return "Expandindo";
        return "Operacional";
    }

    public void write(NBTTagCompound t) {
        t.setLong("rt", runtime);
        t.setLong("mod", modules);
        t.setLong("tg", target);
        t.setLong("ni", nextIndex);
        t.setLong("ln", launchers);
        t.setLong("ab", abandoned);
        t.setLong("w", width);
        t.setLong("h", height);
        t.setLong("sh", shell);
        t.setLong("pd", pending);
        evolution.write(t, "ev");
        produced.write(t, "pr");
        t.setDouble("in", instability);
        t.setDouble("cp", capacity);
        t.setInteger("su", surge);
        t.setInteger("af", activeFood);
        t.setInteger("df", deferred);
        t.setInteger("ma", maxActive);
        t.setInteger("fc", facing);
        t.setBoolean("ru", running);
    }

    public void read(NBTTagCompound t) {
        runtime = t.getLong("rt");
        modules = t.getLong("mod");
        target = t.getLong("tg");
        nextIndex = t.getLong("ni");
        launchers = t.getLong("ln");
        abandoned = t.getLong("ab");
        width = t.getLong("w");
        height = t.getLong("h");
        shell = t.getLong("sh");
        pending = t.getLong("pd");
        evolution = BigNum.read(t, "ev", BigNum.ONE);
        produced = BigNum.read(t, "pr", BigNum.ZERO);
        instability = t.getDouble("in");
        capacity = t.getDouble("cp");
        surge = t.getInteger("su");
        activeFood = t.getInteger("af");
        deferred = t.getInteger("df");
        maxActive = t.getInteger("ma");
        facing = t.getInteger("fc");
        running = t.getBoolean("ru");
    }
}
