package net.caffeinemc.sodium.render.chunk.passes;

import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;

public class DefaultRenderPasses {
    public static final ChunkRenderPass SOLID = new ChunkRenderPass(PipelineDescription.defaults(), true, 0.0f, "terrain_opaque");
    public static final ChunkRenderPass CUTOUT_MIPPED = new ChunkRenderPass(PipelineDescription.defaults(), true, 0.5f, "terrain_cutout");
    public static final ChunkRenderPass CUTOUT = new ChunkRenderPass(PipelineDescription.defaults(), false, 0.1f, "terrain_cutout");
    public static final ChunkRenderPass TRANSLUCENT = new ChunkRenderPass(PipelineDescription.builder()
            .setBlendFunction(BlendFunc.separate(BlendFunc.SrcFactor.SRC_ALPHA, BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA,
                    BlendFunc.SrcFactor.ONE, BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA))
            .build(), true, 0.0f, "terrain_translucent");
    public static final ChunkRenderPass TRIPWIRE = new ChunkRenderPass(PipelineDescription.defaults(), true, 0.1f, "terrain_translucent");

    public static final ChunkRenderPass[] ALL = new ChunkRenderPass[] { SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT, TRIPWIRE };
}
