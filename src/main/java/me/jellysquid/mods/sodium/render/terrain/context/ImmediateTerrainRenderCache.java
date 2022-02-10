package me.jellysquid.mods.sodium.render.terrain.context;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.render.terrain.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.render.terrain.light.cache.HashLightDataCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.BlockAndTintGetter;
import me.jellysquid.mods.sodium.render.terrain.color.blender.ColorBlender;
import me.jellysquid.mods.sodium.render.terrain.BlockRenderer;
import java.util.Map;

public class ImmediateTerrainRenderCache extends TerrainRenderCache {
    private static final Map<BlockAndTintGetter, ImmediateTerrainRenderCache> INSTANCES = new Reference2ObjectOpenHashMap<>();

    private final BlockRenderer blockRenderer;
    private final HashLightDataCache lightCache;

    private ImmediateTerrainRenderCache(BlockAndTintGetter world) {
        Minecraft client = Minecraft.getInstance();

        this.lightCache = new HashLightDataCache(world);

        ColorBlender colorBlender = this.createBiomeColorBlender();
        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightCache);

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, colorBlender);
    }

    public BlockRenderer getBlockRenderer() {
        return this.blockRenderer;
    }

    private void resetCache() {
        this.lightCache.clearCache();
    }

    public static ImmediateTerrainRenderCache getInstance(BlockAndTintGetter world) {
        ImmediateTerrainRenderCache instance = INSTANCES.get(world);

        if (instance == null) {
            throw new IllegalStateException("No global renderer exists");
        }

        return instance;
    }

    public static void destroyRenderContext(BlockAndTintGetter world) {
        if (INSTANCES.remove(world) == null) {
            throw new IllegalStateException("No render context exists for world: " + world);
        }
    }

    public static void createRenderContext(BlockAndTintGetter world) {
        if (INSTANCES.containsKey(world)) {
            throw new IllegalStateException("Render context already exists for world: " + world);
        }

        INSTANCES.put(world, new ImmediateTerrainRenderCache(world));
    }

    public static void resetCaches() {
        for (ImmediateTerrainRenderCache context : INSTANCES.values()) {
            context.resetCache();
        }
    }
}
