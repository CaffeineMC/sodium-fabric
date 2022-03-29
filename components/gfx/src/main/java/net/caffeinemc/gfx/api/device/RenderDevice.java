package net.caffeinemc.gfx.api.device;

import net.caffeinemc.gfx.api.buffer.AllocatedBuffer;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.commands.PipelineGate;
import net.caffeinemc.gfx.api.pipeline.Pipeline;

import java.nio.ByteBuffer;

public interface RenderDevice extends ResourceFactory, ResourceDestructors {
    <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate);

    Buffer createBuffer(AllocatedBuffer buffer, int offset, int length);

    void updateBuffer(Buffer buffer, ByteBuffer data);

    void copyBuffer(Buffer readBuffer, long readOffset, Buffer writeBuffer, long writeOffset, long bytes);

    RenderDeviceProperties properties();
}
