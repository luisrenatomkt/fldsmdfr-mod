package com.fldsmdfr;

import java.util.HashSet;
import java.util.Set;

import com.fldsmdfr.core.BigNum;
import com.fldsmdfr.core.MachineStats;
import com.fldsmdfr.core.ModuleType;
import com.fldsmdfr.core.StructureLayout;

/** Plain-Java self test of the Minecraft-independent logic. Run: see docs/TESTING.md (no JUnit needed). */
public class LogicSelfTest {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        if (!ok) { fails++; System.out.println("FAIL: " + msg); } else System.out.println("ok:   " + msg);
    }

    public static void main(String[] a) {
        // 1. layout is a bijection (no duplicated cells) for the first 400k modules
        Set<Long> seen = new HashSet<Long>();
        boolean dup = false, coreHit = false, heightOk = true;
        for (long i = 0; i < 400000; i++) {
            StructureLayout.Cell c = StructureLayout.cellOf(i);
            long key = ((long) (c.cx + 100000) << 40) | ((long) c.cy << 32) | (c.cz + 100000);
            if (!seen.add(key)) dup = true;
            if (c.cx == 0 && c.cy == 0 && c.cz == 0) coreHit = true;
            if (c.cy > StructureLayout.VMAX) heightOk = false;
        }
        check(!dup, "layout: no duplicated cells in 400000 modules");
        check(!coreHit, "layout: core cell is never used");
        check(heightOk, "layout: vertical extent bounded by VMAX");
        check(StructureLayout.cellOf(0).shell == 1, "layout: first module in shell 1");
        long maxI = StructureLayout.maxIndex();
        StructureLayout.Cell far = StructureLayout.cellOf(maxI);
        check(far.shell <= StructureLayout.MAX_SHELL, "layout: max index maps inside the shell limit (shell " + far.shell + ")");

        // 2. BigNum beyond double range
        BigNum x = BigNum.ONE;
        for (int i = 0; i < 1000; i++) x = x.mul(1e10);
        check(x.log10() > 9990 && x.log10() < 10010, "BigNum: 1e10^1000 = 1e10000 without overflow (log10=" + x.log10() + ")");
        check(x.format().contains("e"), "BigNum: scientific format " + x.format());
        BigNum y = x.add(BigNum.ONE);
        check(y.compareTo(x) >= 0, "BigNum: adding 1 to 1e10000 stays finite/ordered");
        check(BigNum.of(Double.NaN).isZero() && BigNum.of(Double.POSITIVE_INFINITY).toDouble() > 1e300, "BigNum: NaN/Infinity safe");
        check(BigNum.of(1234567).format().equals("1.23M"), "BigNum: abbreviation " + BigNum.of(1234567).format());
        BigNum tiny = BigNum.of(5).add(BigNum.fromLog10(400));
        check(Math.abs(tiny.log10() - 400) < 1e-9, "BigNum: add dominated by the larger term");

        // 3. production depends on built modules and rewards complementary modules
        MachineStats s = new MachineStats();
        s.recompute();
        double base = s.capacityPerSec;
        s.add(ModuleType.CHAMBER, 0); s.add(ModuleType.CHAMBER, 1); s.recompute();
        double withChambers = s.capacityPerSec;
        s.add(ModuleType.TANK, 0); s.add(ModuleType.TUBE, 0); s.add(ModuleType.ARM, 0); s.recompute();
        double withSupport = s.capacityPerSec;
        check(withChambers > base, "stats: chambers raise capacity (" + base + " -> " + withChambers + ")");
        check(withSupport > withChambers, "stats: tank+tube+arm integration bonus (" + withSupport + ")");
        MachineStats d = new MachineStats();
        for (int i = 0; i < 50; i++) d.add(ModuleType.PLATFORM, 0);
        for (int i = 0; i < 50; i++) d.add(ModuleType.TOWER, 0);
        d.recompute();
        check(Math.abs(d.capacityPerSec - MachineStats.BASE_CORE_PRODUCTION) < 1e-9, "stats: decorative modules do not add production");
        MachineStats big = new MachineStats();
        for (int i = 0; i < 1000000; i++) big.add(ModuleType.CHAMBER, 0);
        big.recompute();
        check(!Double.isNaN(big.capacityPerSec) && !Double.isInfinite(big.capacityPerSec), "stats: 1e6 chambers finite");

        // 4. module types are deterministic and diverse
        int[] cnt = new int[ModuleType.values().length];
        for (long i = 0; i < 10000; i++) cnt[ModuleType.of(i, 12345L).ordinal()]++;
        boolean all = true;
        for (int c : cnt) if (c == 0) all = false;
        check(all, "types: every module type appears in 10000 modules");
        check(ModuleType.of(77, 5L) == ModuleType.of(77, 5L), "types: deterministic");

        System.out.println(fails == 0 ? "ALL TESTS PASSED" : (fails + " FAILURES"));
        if (fails != 0) System.exit(1);
    }
}
