package com.fldsmdfr.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fldsmdfr.block.ModBlocks;
import com.fldsmdfr.config.ModConfig;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * All persistent state of one machine plus the purely logical simulation (no world access except
 * verify()). Logical evolution is separate from physical materialisation: evolution raises the
 * module TARGET; StructureBuilder turns the target into blocks gradually.
 */
public class MachineState {
    public static final class Deferred {
        public long index;
        public int retries;
        public long notBefore;

        public Deferred(long index, int retries, long notBefore) {
            this.index = index;
            this.retries = retries;
            this.notBefore = notBefore;
        }
    }

    public long seed;
    public boolean running = true;
    public long runtimeTicks;
    public BigNum evolution = BigNum.ONE;
    public BigNum produced = BigNum.ZERO;
    public BigNum launched = BigNum.ZERO;
    public double instability;
    public double foodBuffer;
    public double launchCarry;
    public double launchCooldown;
    public int surgeTicks;
    public long nextIndex;
    public long abandoned;
    public long lastActiveMs;

    public final MachineStats stats = new MachineStats();
    public final List<ModuleRecord> tracked = new ArrayList<ModuleRecord>();
    public final List<Long> sealedLaunchers = new ArrayList<Long>();
    public final List<Deferred> deferred = new ArrayList<Deferred>();

    // builder cursor (persisted)
    public long curIndex = -1;
    public int curStep;
    public long curAnchor;

    // transient
    public List<BlockSpec> curSpecs;
    public ModuleType curType;
    public boolean curSkip;
    public boolean surgeStarted;
    public int verifyCursor;
    public final Random rng = new Random();

    public void initNew(long worldSeedHint) {
        seed = Hash.mix(worldSeedHint ^ System.nanoTime());
        lastActiveMs = System.currentTimeMillis();
    }

    public double sat() { return instability / (1.0 + instability); }

    public double evoModifier() { return 1.0 + 0.1 * evolution.log10(); }

    public double capacityPerSec() {
        stats.recompute();
        double c = stats.capacityPerSec * evoModifier() * ModConfig.d("productionIntensity");
        return (Double.isNaN(c) || Double.isInfinite(c)) ? 0 : c;
    }

    /** Number of modules the machine wants. Grows as a power of the evolution value; bounded only by the layout. */
    public long targetModules() {
        double lg = Math.max(0.0, evolution.log10());
        double t = 4.0 + 2.0 * Math.pow(10.0, 0.45 * lg);
        long max = StructureLayout.maxIndex();
        if (Double.isNaN(t) || t >= (double) max) return max;
        return (long) t;
    }

    public long pending() {
        return Math.max(0, targetModules() - nextIndex) + deferred.size() + (curIndex >= 0 ? 1 : 0);
    }

    /** Logical simulation for dt ticks. Safe for very large dt (offline) and very large values. */
    public void advance(double dt) {
        if (dt <= 0 || !running) return;
        runtimeTicks += (long) dt;
        stats.recompute();
        double sat = sat();

        // surge events
        double surgeMul = 1.0;
        if (surgeTicks > 0) {
            surgeTicks -= (int) Math.ceil(dt);
            surgeMul = 1.5 + sat;
            if (surgeTicks <= 0) {
                surgeTicks = 0;
                instability *= 0.9; // pressure relief
            }
        } else {
            double p = sat * sat * 0.0005 * ModConfig.d("stormIntensity") * dt;
            if (rng.nextDouble() < Math.min(0.5, p)) {
                surgeTicks = (int) (100 + rng.nextInt(300) * (1.0 + sat));
                surgeStarted = true;
            }
        }

        // production tied to the physically built structure
        double prod = capacityPerSec() / 20.0 * dt * surgeMul;
        if (prod > 0 && !Double.isNaN(prod) && !Double.isInfinite(prod)) {
            produced = produced.add(prod);
            foodBuffer = Math.min(stats.bufferCap, foodBuffer + prod);
        }

        // evolution: exponential in time, rate grows with size, capacity and instability
        double built = stats.total;
        double r = ModConfig.d("evolutionSpeed") * 4.0e-6
                * (1.0 + 0.5 * Math.log(1.0 + built) + 0.3 * Math.log(1.0 + stats.capacityPerSec) + 0.2 * Math.min(instability, 50.0));
        double x = Math.min(r * dt, 50.0);
        evolution = evolution.mul(Math.exp(x));
        evolution = evolution.add(0.0002 * ModConfig.d("evolutionSpeed") * dt);

        // instability
        double di = ModConfig.d("instabilityRate") * 1.0e-5
                * (0.3 + 0.25 * Math.log(1.0 + stats.instabilityLoad) + 0.1 * Math.log10(1.0 + stats.capacityPerSec))
                * stats.stabilizer * dt;
        if (!Double.isNaN(di) && !Double.isInfinite(di)) instability += di;
        if (Double.isNaN(instability) || instability < 0) instability = 0;
        if (instability > 1.0e12) instability = 1.0e12;
    }

    /** Logical progression for time the machine was not ticking. Does NOT build anything. */
    public void applyOffline(long elapsedMs) {
        if (!ModConfig.b("offlineProgress") || !running) return;
        double ticks = Math.min(elapsedMs / 50.0, ModConfig.d("maxOfflineHours") * 72000.0);
        if (ticks < 400) return;
        int steps = (int) Math.min(240, Math.max(1, ticks / 200.0));
        double dt = ticks / steps;
        for (int i = 0; i < steps; i++) advance(dt);
        surgeTicks = 0;
        launchCarry = 0;
    }

    public int pickFood(Random r) {
        long[] c = stats.chambersByFood;
        double total = 1.0;
        for (int i = 0; i < c.length; i++) total += c[i];
        double x = r.nextDouble() * total;
        if (x < 1.0) return 0;
        x -= 1.0;
        for (int i = 0; i < c.length; i++) {
            x -= c[i];
            if (x < 0) return i;
        }
        return 0;
    }

    // ------------------------------------------------------------ modules

    public void onModuleBuilt(ModuleType t, long index, long anchor) {
        stats.add(t, ModuleType.foodCategory(index, seed));
        tracked.add(new ModuleRecord(index, anchor, t.ordinal()));
        int cap = ModConfig.i("maxTrackedModules");
        while (tracked.size() > cap) {
            ModuleRecord r = tracked.remove(0);
            if (r.type == ModuleType.LAUNCHER.ordinal()) {
                sealedLaunchers.add(r.anchor);
                if (sealedLaunchers.size() > 256) sealedLaunchers.remove(0);
            }
        }
    }

    /** Checks a few tracked modules per call; destroyed ones leave the statistics and may be rebuilt. */
    public void verify(World w, long now) {
        int n = Math.min(8, tracked.size());
        List<ModuleRecord> lost = new ArrayList<ModuleRecord>();
        for (int i = 0; i < n && !tracked.isEmpty(); i++) {
            ModuleRecord r = tracked.get(Math.floorMod(verifyCursor++, tracked.size()));
            BlockPos p = BlockPos.fromLong(r.anchor);
            if (w.isBlockLoaded(p) && w.getBlockState(p).getBlock() != ModBlocks.anchor) lost.add(r);
        }
        for (ModuleRecord r : lost) {
            if (!tracked.remove(r)) continue;
            stats.remove(ModuleType.byOrdinal(r.type), ModuleType.foodCategory(r.index, seed));
            if (ModConfig.b("repairModules") && deferred.size() < 256) deferred.add(new Deferred(r.index, 0, now + 200));
        }
        for (int i = 0; i < 4 && !sealedLaunchers.isEmpty(); i++) {
            int idx = Math.floorMod(verifyCursor++, sealedLaunchers.size());
            BlockPos p = BlockPos.fromLong(sealedLaunchers.get(idx));
            if (w.isBlockLoaded(p) && w.getBlockState(p).getBlock() != ModBlocks.anchor) {
                sealedLaunchers.remove(idx);
                stats.remove(ModuleType.LAUNCHER, 0);
            }
        }
    }

    /** Returns the world position (block coords) of a random functional launch point, or null if there is none. */
    public BlockPos randomLaunchPoint(Random r) {
        if (!tracked.isEmpty()) {
            for (int tries = 0; tries < 12; tries++) {
                ModuleRecord rec = tracked.get(r.nextInt(tracked.size()));
                if (rec.type == ModuleType.LAUNCHER.ordinal()) return BlockPos.fromLong(rec.anchor).up(5);
            }
        }
        if (!sealedLaunchers.isEmpty()) return BlockPos.fromLong(sealedLaunchers.get(r.nextInt(sealedLaunchers.size()))).up(5);
        return null;
    }

    public void fillView(MachineView v, long activeFood, int facing) {
        stats.recompute();
        v.runtime = runtimeTicks;
        v.modules = stats.total;
        v.target = targetModules();
        v.nextIndex = nextIndex;
        v.launchers = stats.count[ModuleType.LAUNCHER.ordinal()];
        v.abandoned = abandoned;
        v.pending = pending();
        long[] ext = StructureLayout.extents(nextIndex);
        v.width = ext[0];
        v.height = ext[1];
        v.shell = nextIndex <= 0 ? 0 : StructureLayout.sectorOf(nextIndex - 1);
        v.evolution = evolution;
        v.produced = produced;
        v.instability = instability;
        v.capacity = capacityPerSec();
        v.surge = surgeTicks;
        v.activeFood = (int) activeFood;
        v.deferred = deferred.size();
        v.maxActive = ModConfig.i("maxActiveFood");
        v.facing = facing;
        v.running = running;
    }

    // ------------------------------------------------------------ persistence

    public void write(NBTTagCompound t) {
        t.setLong("seed", seed);
        t.setBoolean("running", running);
        t.setLong("runtime", runtimeTicks);
        evolution.write(t, "evo");
        produced.write(t, "prod");
        launched.write(t, "laun");
        t.setDouble("instab", instability);
        t.setDouble("buffer", foodBuffer);
        t.setDouble("cooldown", launchCooldown);
        t.setInteger("surge", surgeTicks);
        t.setLong("next", nextIndex);
        t.setLong("abandoned", abandoned);
        t.setLong("lastMs", lastActiveMs);
        t.setLong("curIndex", curIndex);
        t.setInteger("curStep", curStep);
        t.setLong("curAnchor", curAnchor);

        NBTTagCompound st = new NBTTagCompound();
        for (int i = 0; i < stats.count.length; i++) st.setLong("c" + i, stats.count[i]);
        for (int i = 0; i < stats.chambersByFood.length; i++) st.setLong("f" + i, stats.chambersByFood[i]);
        t.setTag("stats", st);

        NBTTagList tr = new NBTTagList();
        for (ModuleRecord r : tracked) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("i", r.index);
            c.setLong("a", r.anchor);
            c.setInteger("t", r.type);
            tr.appendTag(c);
        }
        t.setTag("tracked", tr);

        NBTTagList sl = new NBTTagList();
        for (Long a : sealedLaunchers) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("a", a);
            sl.appendTag(c);
        }
        t.setTag("sealed", sl);

        NBTTagList df = new NBTTagList();
        for (Deferred d : deferred) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("i", d.index);
            c.setInteger("r", d.retries);
            c.setLong("n", d.notBefore);
            df.appendTag(c);
        }
        t.setTag("deferred", df);
    }

    public void read(NBTTagCompound t) {
        seed = t.getLong("seed");
        running = !t.hasKey("running") || t.getBoolean("running");
        runtimeTicks = t.getLong("runtime");
        evolution = BigNum.read(t, "evo", BigNum.ONE);
        if (evolution.isZero()) evolution = BigNum.ONE;
        produced = BigNum.read(t, "prod", BigNum.ZERO);
        launched = BigNum.read(t, "laun", BigNum.ZERO);
        instability = t.getDouble("instab");
        if (Double.isNaN(instability) || instability < 0) instability = 0;
        foodBuffer = t.getDouble("buffer");
        launchCooldown = t.getDouble("cooldown");
        surgeTicks = t.getInteger("surge");
        nextIndex = t.getLong("next");
        abandoned = t.getLong("abandoned");
        lastActiveMs = t.getLong("lastMs");
        curIndex = t.hasKey("curIndex") ? t.getLong("curIndex") : -1;
        curStep = t.getInteger("curStep");
        curAnchor = t.getLong("curAnchor");
        curSpecs = null;

        NBTTagCompound st = t.getCompoundTag("stats");
        stats.total = 0;
        for (int i = 0; i < stats.count.length; i++) {
            stats.count[i] = st.getLong("c" + i);
            stats.total += stats.count[i];
        }
        for (int i = 0; i < stats.chambersByFood.length; i++) stats.chambersByFood[i] = st.getLong("f" + i);
        stats.markDirty();

        tracked.clear();
        NBTTagList tr = t.getTagList("tracked", 10);
        for (int i = 0; i < tr.tagCount(); i++) {
            NBTTagCompound c = tr.getCompoundTagAt(i);
            tracked.add(new ModuleRecord(c.getLong("i"), c.getLong("a"), c.getInteger("t")));
        }
        sealedLaunchers.clear();
        NBTTagList sl = t.getTagList("sealed", 10);
        for (int i = 0; i < sl.tagCount(); i++) sealedLaunchers.add(sl.getCompoundTagAt(i).getLong("a"));
        deferred.clear();
        NBTTagList df = t.getTagList("deferred", 10);
        for (int i = 0; i < df.tagCount(); i++) {
            NBTTagCompound c = df.getCompoundTagAt(i);
            deferred.add(new Deferred(c.getLong("i"), c.getInteger("r"), c.getLong("n")));
        }
    }
}
