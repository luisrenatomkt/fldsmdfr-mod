package com.fldsmdfr.tile;

import com.fldsmdfr.config.ModConfig;
import com.fldsmdfr.core.LaunchSystem;
import com.fldsmdfr.core.MachineState;
import com.fldsmdfr.core.MachineView;
import com.fldsmdfr.core.StructureBuilder;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;

/**
 * Server side: runs the logic (evolution, building, launching). Client side: only holds the
 * small {@link MachineView} snapshot used by the renderer and the GUI.
 */
public class TileEntityFldsmdfr extends TileEntity implements ITickable {
    public final MachineState state = new MachineState();
    public final MachineView view = new MachineView();
    public int facing; // horizontal index of the direction the machine front faces

    private int ticks;
    private boolean offlineChecked;
    private int activeFood;

    @Override
    public void update() {
        if (worldObj == null || worldObj.isRemote) return;
        if (state.seed == 0) state.initNew(pos.toLong());
        if (!offlineChecked) {
            offlineChecked = true;
            long elapsed = System.currentTimeMillis() - state.lastActiveMs;
            if (elapsed > 5000) state.applyOffline(elapsed);
            state.lastActiveMs = System.currentTimeMillis();
        }
        ticks++;
        int interval = Math.max(1, ModConfig.i("updateInterval"));
        if (ticks % interval == 0) step(interval);
        if (ticks % Math.max(5, ModConfig.i("syncInterval")) == 0) syncNow();
        if (ticks % 100 == 0) {
            state.lastActiveMs = System.currentTimeMillis();
            markDirty();
        }
    }

    private void step(int interval) {
        long now = worldObj.getTotalWorldTime();
        state.advance(interval);
        if (state.surgeStarted) {
            state.surgeStarted = false;
            if (ModConfig.b("sounds")) worldObj.playSoundEffect(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, "random.explode", 0.6f, 0.5f);
        }
        if (state.running) {
            if (ticks % 100 == 0) state.verify(worldObj, now);
            if (ticks % 40 == 0) activeFood = LaunchSystem.recount(worldObj, ModConfig.i("maxActiveFood"));
            if (worldObj instanceof WorldServer) {
                int spawned = LaunchSystem.update((WorldServer) worldObj, pos, state, interval, activeFood);
                activeFood += spawned;
                effects((WorldServer) worldObj);
            }
            StructureBuilder.update(worldObj, pos, state);
        }
    }

    /** Sparks, smoke and electric arcs between the core and tracked modules; scale with instability. */
    private void effects(WorldServer w) {
        if (!ModConfig.b("visualEffects")) return;
        double sat = state.sat();
        if (sat < 0.05 || state.rng.nextDouble() > sat * 0.8 + 0.05) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1.5, z = pos.getZ() + 0.5;
        w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y + 0.5, z, 2 + (int) (sat * 6), 0.4, 0.2, 0.4, 0.02);
        w.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y, z, 1 + (int) (sat * 5), 0.5, 0.5, 0.5, 0.05);
        if (!state.tracked.isEmpty() && state.rng.nextDouble() < sat) {
            net.minecraft.util.BlockPos to = net.minecraft.util.BlockPos.fromLong(state.tracked.get(state.rng.nextInt(state.tracked.size())).anchor);
            for (int i = 1; i <= 5; i++) {
                double f = i / 6.0;
                double ax = x + (to.getX() + 0.5 - x) * f + (state.rng.nextDouble() - 0.5) * 0.6;
                double ay = y + (to.getY() + 1.0 - y) * f + (state.rng.nextDouble() - 0.5) * 0.6;
                double az = z + (to.getZ() + 0.5 - z) * f + (state.rng.nextDouble() - 0.5) * 0.6;
                w.spawnParticle(EnumParticleTypes.CRIT_MAGIC, ax, ay, az, 1, 0, 0, 0, 0.0);
            }
        }
        if (ModConfig.b("sounds") && state.rng.nextDouble() < sat * 0.05) {
            w.playSoundEffect(x, y, z, "random.fizz", 0.6f, 0.5f + state.rng.nextFloat());
        }
    }

    /** Refreshes the client snapshot and sends it to nearby players. */
    public void syncNow() {
        if (worldObj == null || worldObj.isRemote) return;
        state.fillView(view, activeFood, facing);
        worldObj.markBlockForUpdate(pos);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound t = new NBTTagCompound();
        state.fillView(view, activeFood, facing);
        view.write(t);
        return new S35PacketUpdateTileEntity(pos, 0, t);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        view.read(pkt.getNbtCompound());
        facing = view.facing;
    }

    @Override
    public void writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        state.lastActiveMs = System.currentTimeMillis();
        state.write(t);
        t.setInteger("facing", facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        state.read(t);
        facing = t.getInteger("facing");
        offlineChecked = false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.getX() - 3, pos.getY() - 1, pos.getZ() - 3, pos.getX() + 4, pos.getY() + 5, pos.getZ() + 4);
    }

    @Override
    public double getMaxRenderDistanceSquared() { return 256.0 * 256.0; }
}
