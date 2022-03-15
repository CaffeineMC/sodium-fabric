package net.caffeinemc.sodium.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.render.RenderLayer;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
public class ChunkRenderPassManager {
    private final Reference2ReferenceMap<RenderLayer, ChunkRenderPass> mappings = new Reference2ReferenceOpenHashMap<>();

    private void addMapping(RenderLayer layer, ChunkRenderPass type) {
        if (this.mappings.putIfAbsent(layer, type) != null) {
            throw new IllegalArgumentException("Layer target already defined for " + layer);
        }
    }

    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static ChunkRenderPassManager createDefaultMappings() {
        ChunkRenderPassManager mapper = new ChunkRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), DefaultRenderPasses.SOLID);
        mapper.addMapping(RenderLayer.getCutoutMipped(), DefaultRenderPasses.CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), DefaultRenderPasses.CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), DefaultRenderPasses.TRANSLUCENT);
        mapper.addMapping(RenderLayer.getTripwire(), DefaultRenderPasses.TRIPWIRE);

        return mapper;
    }

    public ChunkRenderPass getRenderPassForLayer(RenderLayer layer) {
        return this.mappings.get(layer);
    }

    public Iterable<ChunkRenderPass> getAllRenderPasses() {
        return this.mappings.values();
    }
}
