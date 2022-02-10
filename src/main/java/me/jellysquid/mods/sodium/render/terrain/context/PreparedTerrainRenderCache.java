package me.jellysquid.mods.sodium.render.terrain.context;

import me.jellysquid.mods.sodium.world.slice.WorldSlice;
import me.jellysquid.mods.sodium.world.slice.WorldSliceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.Level;
import me.jellysquid.mods.sodium.render.terrain.BlockRenderer;
import me.jellysquid.mods.sodium.render.terrain.FluidRenderer;
import me.jellysquid.mods.sodium.render.terrain.color.blender.ColorBlender;
import me.jellysquid.mods.sodium.render.terrain.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.render.terrain.light.cache.ArrayLightDataCache;

public class PreparedTerrainRenderCache extends TerrainRenderCache {
    private final ArrayLightDataCache lightDataCache;

    private final BlockRenderer blockRenderer;
    private final FluidRenderer fluidRenderer;

    private final BlockModelShaper blockModels;
    private final WorldSlice slice;

    public PreparedTerrainRenderCache(Minecraft client, Level world) {
        this.slice = new WorldSlice(world);
        this.lightDataCache = new ArrayLightDataCache(this.slice);

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache);
        ColorBlender colorBlender = this.createBiomeColorBlender();

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, colorBlender);
        this.fluidRenderer = new FluidRenderer(lightPipelineProvider, colorBlender);

        this.blockModels = client.getModelManager().getBlockModelShaper();
    }

    public BlockModelShaper getBlockModels() {
        return this.blockModels;
    }

    public BlockRenderer getBlockRenderer() {
        return this.blockRenderer;
    }

    public FluidRenderer getFluidRenderer() {
        return this.fluidRenderer;
    }

    public void init(WorldSliceData context) {
        this.lightDataCache.reset(context.getOrigin());
        this.slice.init(context);
    }

    @Deprecated
    public WorldSlice getWorldSlice() {
        return this.slice;
    }
}
