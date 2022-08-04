package net.caffeinemc.sodium.render.entity.passes;

import net.caffeinemc.gfx.api.pipeline.RenderPipelineDescription;

// TODO: maybe put main and instance vertex formats here?
public record EntityRenderPass(RenderPipelineDescription pipelineDescription, boolean requiresTranslucencySort) {
}
