package com.fldsmdfr.client;

import com.fldsmdfr.core.MachineView;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;

/**
 * Draws the machine (white/orange capsule, black screen with a blue disc, green keypad, button strip,
 * legs and a fan of blue lights underneath). Evolution adds antennas, pipes and spinning turbines;
 * instability adds vibration and flickering lights. Front of the model is +Z.
 */
public class RenderCore extends TileEntitySpecialRenderer<TileEntityFldsmdfr> {

    @Override
    public void renderTileEntityAt(TileEntityFldsmdfr te, double x, double y, double z, float pt, int destroyStage) {
        MachineView v = te.view;
        double sat = v.sat();
        double lg = Math.max(0.0, v.evolution.log10());
        float time = (te.getWorld() != null ? te.getWorld().getTotalWorldTime() : 0L) + pt;
        boolean on = v.running;
        int stage = (int) Math.min(12.0, lg * 1.5);
        float flick = on ? (float) (0.75 + 0.25 * Math.sin(time * (0.6 + 6.0 * sat)) * (0.5 + sat)) : 0.15f;
        if (sat > 0.4 && ((int) (time * 0.7) % 11) == 0) flick *= 0.2f; // occasional blackout

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        if (on && sat > 0.05) {
            double amp = 0.012 * sat;
            GlStateManager.translate(Math.sin(time * 9.0) * amp, Math.abs(Math.sin(time * 13.0)) * amp, Math.cos(time * 11.0) * amp);
        }
        float s = (float) (1.0 + 0.04 * Math.min(lg, 10.0));
        GlStateManager.scale(s, s, s);
        GlStateManager.rotate((te.facing & 3) * 90.0F, 0, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.color(1, 1, 1, 1);

        // legs and feet
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            box(sx * 0.62 - 0.05, 0.05, sz * 0.45 - 0.05, sx * 0.62 + 0.05, 0.4, sz * 0.45 + 0.05, 0.55f, 0.57f, 0.6f);
            box(sx * 0.62 - 0.11, 0.0, sz * 0.45 - 0.11, sx * 0.62 + 0.11, 0.06, sz * 0.45 + 0.11, 0.15f, 0.15f, 0.17f);
        }
        // underside plate and fan of blue lights
        box(-0.55, 0.3, -0.4, 0.55, 0.37, 0.4, 0.12f, 0.12f, 0.14f);
        for (int i = -3; i <= 3; i++) {
            float b = on ? Math.min(1f, 0.55f + 0.45f * (float) Math.sin(time * 0.5 + i)) * (0.5f + 0.5f * flick) : 0.1f;
            double hh = 0.12 + 0.03 * (3 - Math.abs(i));
            box(i * 0.14 - 0.03, 0.3 - hh, -0.03 + i * 0.01, i * 0.14 + 0.03, 0.3, 0.03 + i * 0.01, 0.5f * b, 0.8f * b, b);
        }
        // body
        box(-0.9, 0.37, -0.7, 0.9, 0.64, 0.7, 0.93f, 0.45f, 0.1f);      // orange skirt
        box(-0.85, 0.64, -0.65, 0.85, 0.92, 0.65, 0.95f, 0.95f, 0.93f); // white lower body
        box(-0.8, 0.92, -0.6, 0.8, 1.9, 0.6, 0.96f, 0.96f, 0.94f);       // white main body
        box(-0.7, 1.9, -0.5, 0.7, 2.05, 0.5, 0.95f, 0.95f, 0.93f);
        box(-0.5, 2.05, -0.35, 0.5, 2.15, 0.35, 0.93f, 0.93f, 0.91f);
        // orange stripes on the front and sides
        box(-0.9 + 0.5, 0.64, 0.6, -0.9 + 0.62, 1.9, 0.605, 0.93f, 0.45f, 0.1f);
        box(0.55, 0.64, 0.6, 0.67, 1.9, 0.605, 0.93f, 0.45f, 0.1f);
        box(-0.805, 1.3, -0.4, -0.8, 1.4, 0.4, 0.93f, 0.45f, 0.1f);
        box(0.8, 1.3, -0.4, 0.805, 1.4, 0.4, 0.93f, 0.45f, 0.1f);
        // side handle
        box(0.8, 1.2, -0.12, 0.95, 1.28, 0.12, 0.6f, 0.62f, 0.65f);
        box(0.9, 1.28, -0.12, 0.95, 1.75, 0.12, 0.6f, 0.62f, 0.65f);
        box(0.8, 1.75, -0.12, 0.95, 1.83, 0.12, 0.6f, 0.62f, 0.65f);

        // screen
        box(-0.68, 1.02, 0.6, 0.2, 1.82, 0.64, 0.03f, 0.03f, 0.04f);
        float rr = (float) (0.3 + 0.7 * sat), gg = (float) (0.6 * (1.0 - sat) + 0.1), bb = (float) (1.0 - 0.8 * sat);
        disc(-0.24, 1.42, 0.645, 0.3, rr * flick, gg * flick + 0.1f, bb * flick);
        // keypad
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            float gl = on ? (0.45f + 0.55f * (float) Math.abs(Math.sin(time * 0.15 + r * 2 + c))) * flick : 0.1f;
            double kx = 0.3 + c * 0.1, ky = 1.62 + r * 0.08;
            box(kx, ky, 0.6, kx + 0.06, ky + 0.045, 0.645, 0.1f * gl, 0.95f * gl, 0.2f * gl);
        }
        float gs = on ? (0.6f + 0.4f * (float) Math.sin(time * 0.3)) * flick : 0.1f;
        box(0.3, 1.2, 0.6, 0.55, 1.3, 0.645, 0.1f * gs, 0.95f * gs, 0.2f * gs);
        box(0.3, 1.4, 0.6, 0.55, 1.45, 0.645, 0.1f, 0.5f, 0.15f);
        // button strip
        box(-0.75, 0.7, 0.6, 0.65, 0.86, 0.63, 0.12f, 0.12f, 0.14f);
        for (int i = 0; i < 10; i++) {
            double bx = -0.7 + i * 0.13;
            float k = (float) i / 9f;
            float lit = on ? (i < 2 ? flick : 1f) : 0.3f;
            box(bx, 0.73, 0.63, bx + 0.09, 0.83, 0.655, (0.85f - 0.3f * k) * lit, (0.2f + 0.55f * k) * lit, (0.15f + 0.4f * k) * lit);
        }

        // ---- evolution decorations ----
        for (int i = 0; i < Math.min(stage, 6); i++) { // antennas with blinking tips
            double ax = (i % 2 == 0 ? -1 : 1) * (0.55 - 0.1 * (i / 2));
            double az = (i % 3 == 0 ? -0.35 : 0.3);
            double h = 0.25 + 0.12 * i;
            box(ax - 0.015, 2.05, az - 0.015, ax + 0.015, 2.05 + h, az + 0.015, 0.6f, 0.62f, 0.65f);
            float bl = ((int) (time / (4 + i)) % 2 == 0) ? 1f : 0.2f;
            box(ax - 0.03, 2.05 + h, az - 0.03, ax + 0.03, 2.1 + h, az + 0.03, bl, 0.1f * bl, 0.05f * bl);
        }
        for (int i = 0; i < stage / 3; i++) { // side pipes
            double py = 0.8 + 0.28 * i;
            box(-1.2, py, -0.18, -0.8, py + 0.2, 0.18, 0.6f, 0.62f, 0.65f);
            box(-1.25, py - 0.02, -0.22, -1.15, py + 0.22, 0.22, 0.35f, 0.37f, 0.4f);
        }
        for (int i = 0; i < stage / 2; i++) { // spinning turbines on top
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 2.15 + 0.09 * i, 0);
            GlStateManager.rotate((float) (time * (6 + 4 * i) * (i % 2 == 0 ? 1 : -1)), 0, 1, 0);
            box(-0.42, 0, -0.03, 0.42, 0.04, 0.03, 0.55f, 0.57f, 0.6f);
            box(-0.03, 0, -0.42, 0.03, 0.04, 0.42, 0.55f, 0.57f, 0.6f);
            GlStateManager.popMatrix();
        }

        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private static void v(WorldRenderer wr, double x, double y, double z, float r, float g, float b) {
        wr.pos(x, y, z).color(clamp(r), clamp(g), clamp(b), 1.0f).endVertex();
    }

    private static float clamp(float f) { return f < 0f ? 0f : (f > 1f ? 1f : f); }

    private static void box(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        float top = 1.0f, side = 0.82f, side2 = 0.66f, bot = 0.5f;
        // top / bottom
        v(wr, x0, y1, z0, r * top, g * top, b * top); v(wr, x0, y1, z1, r * top, g * top, b * top);
        v(wr, x1, y1, z1, r * top, g * top, b * top); v(wr, x1, y1, z0, r * top, g * top, b * top);
        v(wr, x0, y0, z0, r * bot, g * bot, b * bot); v(wr, x1, y0, z0, r * bot, g * bot, b * bot);
        v(wr, x1, y0, z1, r * bot, g * bot, b * bot); v(wr, x0, y0, z1, r * bot, g * bot, b * bot);
        // north / south
        v(wr, x0, y0, z0, r * side, g * side, b * side); v(wr, x0, y1, z0, r * side, g * side, b * side);
        v(wr, x1, y1, z0, r * side, g * side, b * side); v(wr, x1, y0, z0, r * side, g * side, b * side);
        v(wr, x0, y0, z1, r * side, g * side, b * side); v(wr, x1, y0, z1, r * side, g * side, b * side);
        v(wr, x1, y1, z1, r * side, g * side, b * side); v(wr, x0, y1, z1, r * side, g * side, b * side);
        // west / east
        v(wr, x0, y0, z0, r * side2, g * side2, b * side2); v(wr, x0, y0, z1, r * side2, g * side2, b * side2);
        v(wr, x0, y1, z1, r * side2, g * side2, b * side2); v(wr, x0, y1, z0, r * side2, g * side2, b * side2);
        v(wr, x1, y0, z0, r * side2, g * side2, b * side2); v(wr, x1, y1, z0, r * side2, g * side2, b * side2);
        v(wr, x1, y1, z1, r * side2, g * side2, b * side2); v(wr, x1, y0, z1, r * side2, g * side2, b * side2);
        t.draw();
    }

    private static void disc(double cx, double cy, double z, double radius, float r, float g, float b) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
        v(wr, cx, cy, z, Math.min(1f, r + 0.25f), Math.min(1f, g + 0.25f), Math.min(1f, b + 0.1f));
        for (int i = 0; i <= 24; i++) {
            double a = i * Math.PI * 2.0 / 24.0;
            v(wr, cx + Math.cos(a) * radius, cy + Math.sin(a) * radius, z, r, g, b);
        }
        t.draw();
    }
}
