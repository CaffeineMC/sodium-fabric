package me.jellysquid.mods.sodium.client.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.client.render.RenderLayer;

import java.util.List;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
public class BlockRenderLayerManager {
    private final Reference2ReferenceMap<RenderLayer, BlockRenderLayer> mappings = new Reference2ReferenceOpenHashMap<>();
    private final ReferenceSet<BlockRenderLayer> layers = new ReferenceOpenHashSet<>();

    private void addMapping(RenderLayer layer, BlockRenderLayer adapter) {
        if (this.mappings.putIfAbsent(layer, adapter) != null) {
            throw new IllegalArgumentException("Layer target already defined for " + layer);
        }

        this.layers.add(adapter);
    }

    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static BlockRenderLayerManager createDefaultMappings() {
        BlockRenderLayerManager mapper = new BlockRenderLayerManager();
        mapper.addMapping(RenderLayer.getSolid(), BlockRenderLayer.SOLID);
        mapper.addMapping(RenderLayer.getCutoutMipped(), BlockRenderLayer.CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), BlockRenderLayer.CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), BlockRenderLayer.TRANSLUCENT);
        mapper.addMapping(RenderLayer.getTripwire(), BlockRenderLayer.TRIPWIRE);

        return mapper;
    }

    public BlockRenderLayer getAdapter(RenderLayer layer) {
        BlockRenderLayer pass = this.mappings.get(layer);

        if (pass == null) {
            throw new NullPointerException("No render pass exists for layer: " + layer);
        }

        return pass;
    }

    public Iterable<BlockRenderLayer> getRenderLayers() {
        return this.layers;
    }
}
