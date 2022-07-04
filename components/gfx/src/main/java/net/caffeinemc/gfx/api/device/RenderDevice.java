package net.caffeinemc.gfx.api.device;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.DynamicBuffer;
import net.caffeinemc.gfx.api.device.commands.PipelineGate;
import net.caffeinemc.gfx.api.pipeline.Pipeline;

import java.nio.ByteBuffer;

public interface RenderDevice extends ResourceFactory, ResourceDestructors {
    <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate);

    void updateBuffer(DynamicBuffer buffer, int offset, ByteBuffer data);

    void copyBuffer(Buffer readBuffer, Buffer writeBuffer, long readOffset, long writeOffset, long bytes);

    RenderDeviceProperties properties();
    
    RenderConfiguration configuration();
}
