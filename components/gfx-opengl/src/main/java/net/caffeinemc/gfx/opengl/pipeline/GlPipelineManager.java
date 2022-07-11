package net.caffeinemc.gfx.opengl.pipeline;

import net.caffeinemc.gfx.api.pipeline.ComputePipeline;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;

import java.util.function.Consumer;

public interface GlPipelineManager {
    <PROGRAM> void bindComputePipeline(ComputePipeline<PROGRAM> renderPipeline, Consumer<PipelineState> gate);
    
    <ARRAY extends Enum<ARRAY>, PROGRAM> void bindRenderPipeline(RenderPipeline<PROGRAM, ARRAY> renderPipeline, Consumer<PipelineState> gate);
}
