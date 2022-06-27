package net.caffeinemc.sodium.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Collection;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.sodium.SodiumClientMod;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers
 * already used by the base game.
 */
public class ChunkRenderPassManager {
    private static final ChunkRenderPass SOLID = new ChunkRenderPass(
            PipelineDescription.defaults(),
            true,
            0.0f,
            new Identifier("sodium", "solid")
    );
    private static final ChunkRenderPass CUTOUT_MIPPED = new ChunkRenderPass(
            PipelineDescription.defaults(),
            true,
            0.5f,
            new Identifier("sodium", "cutout_mipped")
    );
    private static final ChunkRenderPass CUTOUT = new ChunkRenderPass(
            PipelineDescription.defaults(),
            false,
            0.1f,
            new Identifier("sodium", "cutout")
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
            0.0f,
            new Identifier("sodium", "translucent")
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
            0.1f,
            new Identifier("sodium", "tripwire")
    );
    
    private final Reference2ReferenceMap<RenderLayer, ChunkRenderPass> mappings;
    
    private ChunkRenderPassManager() {
        this.mappings = new Reference2ReferenceOpenHashMap<>();
    }
    
    /**
     * Note: This won't take effect until after a renderer reload.
     */
    public void addMapping(RenderLayer layer, ChunkRenderPass type) {
        if (this.mappings.putIfAbsent(layer, type) != null) {
            SodiumClientMod.logger().info("Render layer " + layer.toString() + " bound to render pass " + type);
        }
    }
    
    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering behavior of
     * vanilla.
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
    
    public ChunkRenderPass getRenderPassForLayer(RenderLayer layer) {
        return this.mappings.get(layer);
    }
    
    public Collection<ChunkRenderPass> getAllRenderPasses() {
        return this.mappings.values();
    }
    
    public int getRenderPassCount() {
        return this.mappings.size();
    }
}
