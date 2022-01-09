package me.jellysquid.mods.sodium.opengl.pipeline;

import java.util.function.Consumer;

public interface PipelineManager {
    <ARRAY extends Enum<ARRAY>, PROGRAM> void bindPipeline(Pipeline<PROGRAM, ARRAY> pipeline, Consumer<PipelineState> gate);
}
