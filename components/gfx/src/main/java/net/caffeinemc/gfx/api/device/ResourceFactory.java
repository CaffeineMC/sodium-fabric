package net.caffeinemc.gfx.api.device;

import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.buffer.*;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ResourceFactory {
    <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory);

    ImmutableBuffer createBuffer(ByteBuffer data, Set<ImmutableBufferFlags> flags);

    ImmutableBuffer createBuffer(long capacity, Set<ImmutableBufferFlags> flags);

    ImmutableBuffer createBuffer(long capacity, Consumer<ByteBuffer> data, Set<ImmutableBufferFlags> flags);

    DynamicBuffer createDynamicBuffer(long capacity, Set<DynamicBufferFlags> flags);

    MappedBuffer createMappedBuffer(long capacity, Set<MappedBufferFlags> flags);

    <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(PipelineDescription state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArray);

    Sampler createSampler();

    Fence createFence();
}
