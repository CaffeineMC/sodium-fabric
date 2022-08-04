package net.caffeinemc.gfx.api.device;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.DynamicBuffer;
import net.caffeinemc.gfx.api.device.commands.ComputePipelineGate;
import net.caffeinemc.gfx.api.device.commands.RenderPipelineGate;
import net.caffeinemc.gfx.api.pipeline.ComputePipeline;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;

import java.nio.ByteBuffer;

public interface RenderDevice extends ResourceFactory, ResourceDestructors {
    <PROGRAM, ARRAY extends Enum<ARRAY>> void useRenderPipeline(RenderPipeline<PROGRAM, ARRAY> pipeline, RenderPipelineGate<PROGRAM, ARRAY> gate);
    
    <PROGRAM> void useComputePipeline(ComputePipeline<PROGRAM> pipeline, ComputePipelineGate<PROGRAM> gate);

    void updateBuffer(DynamicBuffer buffer, int offset, ByteBuffer data);

    void copyBuffer(Buffer readBuffer, Buffer writeBuffer, long readOffset, long writeOffset, long length);

    RenderDeviceProperties properties();
    
    RenderConfiguration configuration();
}
