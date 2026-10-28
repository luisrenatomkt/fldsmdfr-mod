package com.fldsmdfr.proxy;

import com.fldsmdfr.FldsmdfrMod;
import com.fldsmdfr.block.ModBlocks;
import com.fldsmdfr.client.RenderCore;
import com.fldsmdfr.entity.EntityFoodDrop;
import com.fldsmdfr.item.ModItems;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFldsmdfr.class, new RenderCore());
        RenderingRegistry.registerEntityRenderingHandler(EntityFoodDrop.class,
                new RenderEntityItem(mc.getRenderManager(), mc.getRenderItem()));

        model(Item.getItemFromBlock(ModBlocks.core), "fldsmdfr_core");
        model(Item.getItemFromBlock(ModBlocks.anchor), "fldsmdfr_anchor");
        for (int i = 0; i < ModItems.FOODS.size(); i++) model(ModItems.FOODS.get(i), ModItems.NAMES.get(i));
    }

    private static void model(Item item, String name) {
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher()
                .register(item, 0, new ModelResourceLocation(FldsmdfrMod.MODID + ":" + name, "inventory"));
    }
}
