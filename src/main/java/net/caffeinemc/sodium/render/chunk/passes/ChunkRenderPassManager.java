package net.caffeinemc.sodium.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceSortedMap;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.sodium.SodiumClientMod;
import net.minecraft.client.render.RenderLayer;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers
 * already used by the base game.
 */
public class ChunkRenderPassManager {
    private static final ChunkRenderPass SOLID = new ChunkRenderPass(
            PipelineDescription.defaults(),
            true,
            0.0f
    );
    private static final ChunkRenderPass CUTOUT_MIPPED = new ChunkRenderPass(
            PipelineDescription.defaults(),
            true,
            0.5f
    );
    private static final ChunkRenderPass CUTOUT = new ChunkRenderPass(
            PipelineDescription.defaults(),
            false,
            0.1f
    );
    private static final ChunkRenderPass TRANSLUCENT = new ChunkRenderPass(
            PipelineDescription.builder()
                               .setBlendFunction(BlendFunc.separate(
                                       BlendFunc.SrcFactor.SRC_ALPHA,
                                       BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA,
                                       BlendFunc.SrcFactor.ONE,
                                       BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA
                               ))
                               .build(),
            true,
            0.0f
    );
    private static final ChunkRenderPass TRIPWIRE = new ChunkRenderPass(
            PipelineDescription.builder()
                               .setBlendFunction(BlendFunc.separate(
                                       BlendFunc.SrcFactor.SRC_ALPHA,
                                       BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA,
                                       BlendFunc.SrcFactor.ONE,
                                       BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA
                               ))
                               .build(),
            true,
            0.1f
    );
    
    private final Reference2ReferenceSortedMap<RenderLayer, ChunkRenderPass> layerMappings;
    private ChunkRenderPass[] idMappings;
    
    public ChunkRenderPassManager() {
        this.layerMappings = new Reference2ReferenceLinkedOpenHashMap<>();
    }
    
    /**
     * Creates a manager with a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static ChunkRenderPassManager createDefaultMappings() {
        ChunkRenderPassManager mapper = new ChunkRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), SOLID);
        mapper.addMapping(RenderLayer.getCutoutMipped(), CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), TRANSLUCENT);
        mapper.addMapping(RenderLayer.getTripwire(), TRIPWIRE);
        
        return mapper;
    }
    
    /**
     * Note: This won't take effect until after a renderer reload.
     */
    public void addMapping(RenderLayer layer, ChunkRenderPass renderPass) {
        int nextId = this.layerMappings.size();
        renderPass.setId(nextId);
        
        ChunkRenderPass oldRenderPass = this.layerMappings.put(layer, renderPass);
        this.idMappings = this.layerMappings.values().toArray(new ChunkRenderPass[0]);
        
        if (oldRenderPass != null) {
            SodiumClientMod.logger().info("Render layer %s bound to render pass %s, overriding %s".formatted(
                    layer.toString(),
                    renderPass,
                    oldRenderPass
            ));
        }
    }
    
    public ChunkRenderPass getRenderPassForLayer(RenderLayer layer) {
        return this.layerMappings.get(layer);
    }
    
    public ChunkRenderPass[] getAllRenderPasses() {
        return this.idMappings;
    }
    
    public int getRenderPassCount() {
        return this.idMappings.length;
    }
}
