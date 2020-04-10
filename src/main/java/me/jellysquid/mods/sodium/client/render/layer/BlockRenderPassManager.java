package me.jellysquid.mods.sodium.client.render.layer;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import net.minecraft.client.render.RenderLayer;

public class BlockRenderPassManager {
    private final Reference2ReferenceArrayMap<RenderLayer, BlockRenderPass> mappings = new Reference2ReferenceArrayMap<>();

    public BlockRenderPassManager() {
        this.mappings.defaultReturnValue(BlockRenderPass.FALLBACK);
    }

    public BlockRenderPass get(RenderLayer layer) {
        return this.mappings.get(layer);
    }

    private void addMapping(RenderLayer layer, BlockRenderPass type) {
        if (this.mappings.put(layer, type) != BlockRenderPass.FALLBACK) {
            throw new IllegalArgumentException("Layer target already defined for " + layer);
        }
    }

    public static BlockRenderPassManager vanilla() {
        BlockRenderPassManager mapper = new BlockRenderPassManager();

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            mapper.addMapping(layer, new BlockRenderPass(layer, layer == RenderLayer.getTranslucent()));
        }

        return mapper;
    }

    public static BlockRenderPassManager consolidated() {
        BlockRenderPass solid = new BlockRenderPass(RenderLayer.getCutout(), false);
        BlockRenderPass solidMipped = new BlockRenderPass(RenderLayer.getCutoutMipped(), false);
        BlockRenderPass translucent = new BlockRenderPass(RenderLayer.getTranslucent(), true);

        BlockRenderPassManager mapper = new BlockRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), solidMipped);
        mapper.addMapping(RenderLayer.getCutoutMipped(), solidMipped);
        mapper.addMapping(RenderLayer.getCutout(), solid);
        mapper.addMapping(RenderLayer.getTranslucent(), translucent);

        return mapper;
    }
}
