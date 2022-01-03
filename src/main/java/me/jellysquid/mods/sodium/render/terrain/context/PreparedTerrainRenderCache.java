package me.jellysquid.mods.sodium.render.terrain.context;

import me.jellysquid.mods.sodium.render.chunk.compile.WorldSlice;
import me.jellysquid.mods.sodium.render.chunk.compile.WorldSliceData;
import me.jellysquid.mods.sodium.render.terrain.BlockRenderer;
import me.jellysquid.mods.sodium.render.terrain.FluidRenderer;
import me.jellysquid.mods.sodium.render.terrain.color.blender.ColorBlender;
import me.jellysquid.mods.sodium.render.terrain.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.render.terrain.light.cache.ArrayLightDataCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.world.World;

public class PreparedTerrainRenderCache extends TerrainRenderCache {
    private final ArrayLightDataCache lightDataCache;

    private final BlockRenderer blockRenderer;
    private final FluidRenderer fluidRenderer;

    private final BlockModels blockModels;
    private final WorldSlice slice;

    public PreparedTerrainRenderCache(MinecraftClient client, World world) {
        this.slice = new WorldSlice(world);
        this.lightDataCache = new ArrayLightDataCache(this.slice);

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache);
        ColorBlender colorBlender = this.createBiomeColorBlender();

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, colorBlender);
        this.fluidRenderer = new FluidRenderer(lightPipelineProvider, colorBlender);

        this.blockModels = client.getBakedModelManager().getBlockModels();
    }

    public BlockModels getBlockModels() {
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
        this.slice.copyData(context);
    }

    @Deprecated
    public WorldSlice getWorldSlice() {
        return this.slice;
    }
}
