package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.PipelineState;

public interface PipelineGate<SHADER, VERTEX extends Enum<VERTEX>> {
    void run(RenderCommandList<VERTEX> commandList, SHADER programInterface, PipelineState pipelineState);
}
