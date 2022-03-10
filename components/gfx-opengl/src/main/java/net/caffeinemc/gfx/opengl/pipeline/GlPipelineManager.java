package net.caffeinemc.gfx.opengl.pipeline;

import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;

import java.util.function.Consumer;

public interface GlPipelineManager {
    <ARRAY extends Enum<ARRAY>, PROGRAM> void bindPipeline(Pipeline<PROGRAM, ARRAY> pipeline, Consumer<PipelineState> gate);
}
