package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.LightDataCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class GlobalRenderer {
    private static final ThreadLocal<GlobalRenderer> GLOBAL = ThreadLocal.withInitial(GlobalRenderer::new);

    private final BlockRenderPipeline blockRenderer;
    private final LightDataCache lightCache;

    private GlobalRenderer() {
        MinecraftClient client = MinecraftClient.getInstance();

        this.lightCache = new LightDataCache(5);
        this.blockRenderer = new BlockRenderPipeline(client, this.lightCache);
    }

    public BlockRenderPipeline getBlockRenderer() {
        return this.blockRenderer;
    }

    public static GlobalRenderer getInstance(BlockRenderView world, BlockPos pos) {
        GlobalRenderer renderer = GLOBAL.get();
        renderer.lightCache.init(world, pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2);

        return renderer;
    }
}
