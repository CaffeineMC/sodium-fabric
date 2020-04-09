package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.light.cache.HashLightDataCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.BlockRenderView;

public class GlobalRenderer {
    private static final GlobalRenderer INSTANCE = new GlobalRenderer();

    private final BlockRenderPipeline blockRenderer;
    private final HashLightDataCache lightCache;

    private GlobalRenderer() {
        MinecraftClient client = MinecraftClient.getInstance();

        this.lightCache = new HashLightDataCache();
        this.blockRenderer = new BlockRenderPipeline(client, this.lightCache);
    }

    public BlockRenderPipeline getBlockRenderer() {
        return this.blockRenderer;
    }

    public static GlobalRenderer getInstance(BlockRenderView world) {
        INSTANCE.lightCache.init(world);

        return INSTANCE;
    }

    public static void reset() {
        INSTANCE.lightCache.clear();
    }
}
