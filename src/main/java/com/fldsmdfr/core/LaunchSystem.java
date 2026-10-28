package com.fldsmdfr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.fldsmdfr.config.ModConfig;
import com.fldsmdfr.entity.EntityFoodDrop;
import com.fldsmdfr.item.ModItems;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Turns logical production into food entities fired from the real launcher modules. */
public final class LaunchSystem {
    private static final double GRAVITY = 0.04;

    private LaunchSystem() {}

    /** Counts food entities in the world and removes the oldest above the limit. Returns the remaining count. */
    public static int recount(World w, int max) {
        List<EntityFoodDrop> list = new ArrayList<EntityFoodDrop>();
        for (Entity e : w.loadedEntityList) {
            if (e instanceof EntityFoodDrop && !e.isDead) list.add((EntityFoodDrop) e);
        }
        if (list.size() <= max) return list.size();
        Collections.sort(list, new Comparator<EntityFoodDrop>() {
            @Override
            public int compare(EntityFoodDrop a, EntityFoodDrop b) { return b.ticksExisted - a.ticksExisted; }
        });
        int remove = list.size() - max;
        for (int i = 0; i < remove; i++) list.get(i).setDead();
        return max;
    }

    /** One launch cycle. Returns the number of entities spawned. */
    public static int update(WorldServer w, BlockPos core, MachineState st, double dt, int activeFood) {
        if (!st.running) return 0;
        st.launchCooldown -= dt;
        if (st.launchCooldown > 0) return 0;
        double sat = st.sat();
        double interval = 40.0 / (1.0 + 3.0 * sat);
        if (st.surgeTicks > 0) interval *= 0.5;
        interval = Math.max(4.0, interval);
        st.launchCooldown = interval;

        st.stats.recompute();
        double perSec = st.stats.launchPerSec * ModConfig.d("stormIntensity");
        if (st.surgeTicks > 0) perSec *= 2.0 + 3.0 * sat;
        double logical = Math.min(st.foodBuffer, perSec * interval / 20.0);
        if (logical <= 0 || Double.isNaN(logical)) return 0;
        st.foodBuffer -= logical;
        st.launched = st.launched.add(logical);

        double cx = core.getX() + 0.5, cy = core.getY(), cz = core.getZ() + 0.5;
        EntityPlayer near = w.getClosestPlayer(cx, cy, cz, ModConfig.d("simulationDistance"));
        if (near == null) { st.launchCarry = 0; return 0; } // logical only while nobody is close

        st.launchCarry += logical;
        int room = Math.min(ModConfig.i("maxLaunchesPerUpdate"), ModConfig.i("maxActiveFood") - activeFood);
        if (room <= 0) { st.launchCarry = 0; return 0; }
        long whole = (long) Math.min(1.0e9, Math.floor(st.launchCarry));
        int entities = (int) Math.min((long) room, whole);
        if (entities <= 0) return 0;
        int perEntity = (int) Math.max(1L, Math.min(64L, whole / entities));
        st.launchCarry = Math.min(256.0, Math.max(0.0, st.launchCarry - (double) entities * perEntity));

        double range = (ModConfig.d("launchDistance") + st.stats.rangeBonus) * (1.0 + 0.5 * sat);
        range = Math.min(range, 600.0);
        Random r = st.rng;
        for (int i = 0; i < entities; i++) {
            BlockPos lp = st.randomLaunchPoint(r);
            double x, y, z;
            if (lp != null) { x = lp.getX() + 0.5; y = lp.getY() + 0.3; z = lp.getZ() + 0.5; }
            else { x = cx; y = cy + 2.4; z = cz; } // the core itself
            FoodRegistry.FoodDef def = FoodRegistry.get(st.pickFood(r));
            ItemStack stack = ModItems.stackFor(def, perEntity);
            if (stack == null) continue;
            EntityFoodDrop e = new EntityFoodDrop(w, x, y, z, stack);
            double ang = r.nextDouble() * Math.PI * 2.0;
            double dist = range * (0.25 + 0.75 * Math.pow(r.nextDouble(), 0.7));
            dist *= 1.0 + (r.nextDouble() - 0.5) * (0.2 + 0.6 * sat); // dispersion
            double elev = Math.toRadians(50.0);
            double v = Math.sqrt(dist * GRAVITY / Math.sin(2.0 * elev)) * 1.12;
            v = Math.min(v, 3.6);
            e.motionX = Math.cos(ang) * Math.cos(elev) * v;
            e.motionZ = Math.sin(ang) * Math.cos(elev) * v;
            e.motionY = Math.sin(elev) * v;
            e.setPickupDelay(30);
            e.lifespan = ModConfig.i("foodLifespanTicks");
            w.spawnEntityInWorld(e);
            if (i == 0 && ModConfig.b("visualEffects")) {
                w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 4, 0.2, 0.1, 0.2, 0.01);
            }
        }
        if (ModConfig.b("sounds") && r.nextInt(3) == 0) {
            w.playSoundEffect(cx, cy + 2, cz, "tile.piston.out", 0.8f, 0.6f + r.nextFloat() * 0.8f);
        }
        return entities;
    }
}
