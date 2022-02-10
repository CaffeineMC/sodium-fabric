package me.jellysquid.mods.sodium.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.RenderType;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
public class ChunkRenderPassManager {
    private final Reference2ReferenceMap<RenderType, ChunkRenderPass> mappings = new Reference2ReferenceOpenHashMap<>();

    private void addMapping(RenderType layer, ChunkRenderPass type) {
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
        mapper.addMapping(RenderType.solid(), DefaultRenderPasses.SOLID);
        mapper.addMapping(RenderType.cutoutMipped(), DefaultRenderPasses.CUTOUT_MIPPED);
        mapper.addMapping(RenderType.cutout(), DefaultRenderPasses.CUTOUT);
        mapper.addMapping(RenderType.translucent(), DefaultRenderPasses.TRANSLUCENT);
        mapper.addMapping(RenderType.tripwire(), DefaultRenderPasses.TRIPWIRE);

        return mapper;
    }

    public ChunkRenderPass getRenderPassForLayer(RenderType layer) {
        return this.mappings.get(layer);
    }

    public Iterable<ChunkRenderPass> getAllRenderPasses() {
        return this.mappings.values();
    }
}
