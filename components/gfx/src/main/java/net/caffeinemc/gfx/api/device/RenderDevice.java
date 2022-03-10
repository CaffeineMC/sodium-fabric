package net.caffeinemc.gfx.api.device;

import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.BufferMapFlags;
import net.caffeinemc.gfx.api.buffer.BufferStorageFlags;
import net.caffeinemc.gfx.api.buffer.MappedBuffer;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface RenderDevice {
    Set<BufferStorageFlags> DEFAULT_STORAGE_FLAGS = EnumSet.noneOf(BufferStorageFlags.class);

    <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory);

    Buffer createBuffer(ByteBuffer data, Set<BufferStorageFlags> flags);

    default Buffer createBuffer(ByteBuffer data) {
        return this.createBuffer(data, DEFAULT_STORAGE_FLAGS);
    }

    Buffer createBuffer(long capacity, Set<BufferStorageFlags> flags);

    default Buffer createBuffer(long capacity) {
        return this.createBuffer(capacity, DEFAULT_STORAGE_FLAGS);
    }

    Buffer createBuffer(long capacity, Consumer<ByteBuffer> data, Set<BufferStorageFlags> flags);

    default Buffer createBuffer(long capacity, Consumer<ByteBuffer> data) {
        return this.createBuffer(capacity, data, DEFAULT_STORAGE_FLAGS);
    }

    MappedBuffer createMappedBuffer(long capacity, Set<BufferStorageFlags> storageFlags, Set<BufferMapFlags> mapFlags);

    <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(PipelineDescription state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArray);

    <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate);

    void deletePipeline(Pipeline<?, ?> pipeline);

    void uploadData(Buffer buffer, ByteBuffer data);

    interface PipelineGate<SHADER, VERTEX extends Enum<VERTEX>> {
        void run(RenderCommandList<VERTEX> commandList, SHADER programInterface, PipelineState pipelineState);
    }

    Sampler createSampler();

    void deleteSampler(Sampler sampler);

    void deleteVertexArray(VertexArray<?> array);

    void copyBuffer(long bytes, Buffer src, long readOffset, Buffer dst, long writeOffset);

    void deleteBuffer(Buffer buffer);

    void deleteProgram(Program<?> program);

    Fence createFence();

    RenderDeviceProperties properties();
}
