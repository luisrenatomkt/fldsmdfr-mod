package com.fldsmdfr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Extensible list of food categories. Other mods/classes may call {@link #register} before preInit ends. */
public final class FoodRegistry {
    public static final class FoodDef {
        public final String id;
        public final int hunger;
        public final float saturation;

        FoodDef(String id, int hunger, float saturation) {
            this.id = id;
            this.hunger = hunger;
            this.saturation = saturation;
        }
    }

    private static final List<FoodDef> DEFS = new ArrayList<FoodDef>();

    static {
        register("burger", 8, 0.8f);
        register("pizza", 7, 0.7f);
        register("hotdog", 6, 0.6f);
        register("sandwich", 6, 0.6f);
        register("donut", 3, 0.3f);
        register("cake", 4, 0.4f);
        register("fries", 4, 0.3f);
    }

    private FoodRegistry() {}

    public static synchronized FoodDef register(String id, int hunger, float saturation) {
        FoodDef d = new FoodDef(id, hunger, saturation);
        DEFS.add(d);
        return d;
    }

    public static synchronized int count() { return DEFS.size(); }

    public static synchronized FoodDef get(int i) { return DEFS.get(Math.floorMod(i, DEFS.size())); }

    public static synchronized List<FoodDef> all() { return Collections.unmodifiableList(new ArrayList<FoodDef>(DEFS)); }
}
