package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.pipeline.PipelineState;

public interface ComputePipelineGate<SHADER> {
    void run(ComputeCommandList commandList, SHADER programInterface, PipelineState pipelineState);
}