package net.caffeinemc.gfx.api.device;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.buffer.*;
import net.caffeinemc.gfx.api.pipeline.ComputePipeline;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.api.pipeline.RenderPipelineDescription;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.texture.parameters.AddressMode;
import net.caffeinemc.gfx.api.texture.parameters.FilterMode;
import net.caffeinemc.gfx.api.texture.parameters.MipmapMode;
import org.jetbrains.annotations.Nullable;

// TODO: add orphanBuffer and use it with SequenceIndexBuffer
public interface ResourceFactory {
    <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory);

    ImmutableBuffer createBuffer(ByteBuffer data, Set<ImmutableBufferFlags> flags);

    ImmutableBuffer createBuffer(long capacity, Set<ImmutableBufferFlags> flags);

    ImmutableBuffer createBuffer(long capacity, Consumer<ByteBuffer> preUnmapConsumer, Set<ImmutableBufferFlags> flags);

    DynamicBuffer createDynamicBuffer(long capacity, Set<DynamicBufferFlags> flags);

    MappedBuffer createMappedBuffer(long capacity, Set<MappedBufferFlags> flags);

    MappedBuffer createMappedBuffer(long capacity, Consumer<Buffer> preMapConsumer, Set<MappedBufferFlags> flags);
    
    <PROGRAM> ComputePipeline<PROGRAM> createComputePipeline(Program<PROGRAM> program);

    <PROGRAM, ARRAY extends Enum<ARRAY>> RenderPipeline<PROGRAM, ARRAY> createRenderPipeline(RenderPipelineDescription state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArray);

    /**
     * If any of the parameters provided are null, the default value will be used.
     */
    Sampler createSampler(
            @Nullable FilterMode minFilter,
            @Nullable MipmapMode mipmapMode,
            @Nullable FilterMode magFilter,
            @Nullable AddressMode addressModeU,
            @Nullable AddressMode addressModeV,
            @Nullable AddressMode addressModeW
    );

    Fence createFence();
}
