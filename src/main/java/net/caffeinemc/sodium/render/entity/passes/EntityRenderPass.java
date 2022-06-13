package net.caffeinemc.sodium.render.entity.passes;

import net.caffeinemc.gfx.api.pipeline.PipelineDescription;

// TODO: maybe put main and instance vertex formats here?
public record EntityRenderPass(PipelineDescription pipelineDescription, boolean requiresTranslucencySort) {
}
