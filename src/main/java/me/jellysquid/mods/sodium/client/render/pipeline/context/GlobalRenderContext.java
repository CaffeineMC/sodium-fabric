package me.jellysquid.mods.sodium.client.render.pipeline.context;

import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.cache.HashLightDataCache;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.RenderContextCommon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.BlockRenderView;

import java.util.WeakHashMap;

public class GlobalRenderContext {
    private static final WeakHashMap<BlockRenderView, GlobalRenderContext> INSTANCES = new WeakHashMap<>();

    private final BlockRenderer blockRenderer;
    private final HashLightDataCache lightCache;

    private GlobalRenderContext(BlockRenderView world) {
        MinecraftClient client = MinecraftClient.getInstance();

        this.lightCache = new HashLightDataCache(world);

        BiomeColorBlender biomeColorBlender = RenderContextCommon.createBiomeColorBlender();
        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightCache);

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, biomeColorBlender);
    }

    public BlockRenderer getBlockRenderer() {
        return this.blockRenderer;
    }

    public static GlobalRenderContext getInstance(BlockRenderView world) {
        return INSTANCES.computeIfAbsent(world, GlobalRenderContext::createInstance);
    }

    private static GlobalRenderContext createInstance(BlockRenderView world) {
        return new GlobalRenderContext(world);
    }

    public static void reset() {
        for (GlobalRenderContext context : INSTANCES.values()) {
            context.lightCache.clear();
        }
    }
}
