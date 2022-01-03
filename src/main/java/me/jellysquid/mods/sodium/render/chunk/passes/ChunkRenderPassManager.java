package me.jellysquid.mods.sodium.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import net.minecraft.client.render.RenderLayer;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
public class ChunkRenderPassManager {
    private final Reference2IntArrayMap<RenderLayer> mappingsId = new Reference2IntArrayMap<>();

    public ChunkRenderPassManager() {
        this.mappingsId.defaultReturnValue(-1);
    }

    public int getRenderPassId(RenderLayer layer) {
        int pass = this.mappingsId.getInt(layer);

        if (pass < 0) {
            throw new NullPointerException("No render pass exists for layer: " + layer);
        }

        return pass;
    }

    private void addMapping(RenderLayer layer, ChunkRenderPass type) {
        if (this.mappingsId.put(layer, type.ordinal()) >= 0) {
            throw new IllegalArgumentException("Layer target already defined for " + layer);
        }
    }

    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static ChunkRenderPassManager createDefaultMappings() {
        ChunkRenderPassManager mapper = new ChunkRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), ChunkRenderPass.SOLID);
        mapper.addMapping(RenderLayer.getCutoutMipped(), ChunkRenderPass.CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), ChunkRenderPass.CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), ChunkRenderPass.TRANSLUCENT);
        mapper.addMapping(RenderLayer.getTripwire(), ChunkRenderPass.TRIPWIRE);

        return mapper;
    }

    public ChunkRenderPass getRenderPassForLayer(RenderLayer layer) {
        return this.getRenderPass(this.getRenderPassId(layer));
    }

    public ChunkRenderPass getRenderPass(int i) {
        return ChunkRenderPass.VALUES[i];
    }
}
