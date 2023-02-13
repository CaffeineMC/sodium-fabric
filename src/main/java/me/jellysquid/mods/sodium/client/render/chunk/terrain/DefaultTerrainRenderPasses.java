package me.jellysquid.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.render.RenderLayer;

public class DefaultTerrainRenderPasses {
    public static final TerrainRenderPass SOLID = new TerrainRenderPass(RenderLayer.getSolid(), false, false);
    public static final TerrainRenderPass CUTOUT = new TerrainRenderPass(RenderLayer.getCutoutMipped(), false, true);
    public static final TerrainRenderPass TRANSLUCENT = new TerrainRenderPass(RenderLayer.getTranslucent(), true, false);


    public static final TerrainRenderPass[] ALL = new TerrainRenderPass[] { SOLID, CUTOUT, TRANSLUCENT };
}
