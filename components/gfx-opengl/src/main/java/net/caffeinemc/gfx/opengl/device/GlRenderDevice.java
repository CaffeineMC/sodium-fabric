package net.caffeinemc.gfx.opengl.device;

import net.caffeinemc.gfx.api.buffer.*;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import net.caffeinemc.gfx.api.device.commands.PipelineGate;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.buffer.GlAbstractBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlDynamicBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlImmutableBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlMappedBuffer;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.pipeline.GlPipeline;
import net.caffeinemc.gfx.opengl.pipeline.GlPipelineManager;
import net.caffeinemc.gfx.opengl.texture.GlSampler;
import net.caffeinemc.gfx.opengl.shader.GlProgram;
import net.caffeinemc.gfx.opengl.sync.GlFence;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.RenderDeviceProperties;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MathUtil;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class GlRenderDevice implements RenderDevice {
    private final GlPipelineManager pipelineManager;
    private final RenderDeviceProperties properties;

    public GlRenderDevice(GlPipelineManager pipelineManager) {
        // TODO: move this into platform code
        this.pipelineManager = pipelineManager;
        this.properties = getDeviceProperties();
    }

    private static RenderDeviceProperties getDeviceProperties() {
        var uniformBufferAlignment = GL45C.glGetInteger(GL45C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);

        if (!MathUtil.mathIsPoT(uniformBufferAlignment)) {
            throw new RuntimeException("GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT is not a power-of-two (found value of %s)"
                    .formatted(uniformBufferAlignment));
        }

        var storageBufferAlignment = GL45C.glGetInteger(GL45C.GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT);

        if (!MathUtil.mathIsPoT(storageBufferAlignment)) {
            throw new RuntimeException("GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT is not a power-of-two (found value of %s)"
                    .formatted(storageBufferAlignment));
        }

        return new RenderDeviceProperties(
                uniformBufferAlignment,
                storageBufferAlignment
        );
    }

    @Override
    public void copyBuffer(Buffer srcBuffer, Buffer dstBuffer, long srcOffset, long dstOffset, long length) {
        this.copyBuffer0((GlAbstractBuffer) srcBuffer, (GlAbstractBuffer) dstBuffer, srcOffset, dstOffset, length);
    }

    private void copyBuffer0(GlAbstractBuffer srcBuffer, GlAbstractBuffer dstBuffer, long srcOffset, long dstOffset, long length) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(srcOffset >= 0, "Source offset must be greater than or equal to zero");
            Validate.isTrue(dstOffset >= 0, "Destination offset must be greater than or equal to zero");

            Validate.isTrue(srcOffset + length <= srcBuffer.capacity(), "Source buffer range is out-of-bounds");
            Validate.isTrue(dstOffset + length <= dstBuffer.capacity(), "Destination buffer range is out-of-bounds");
        }

        GL45C.glCopyNamedBufferSubData(srcBuffer.handle(), dstBuffer.handle(), srcOffset, dstOffset, length);
    }

    @Override
    public void deleteBuffer(Buffer buffer) {
        this.deleteBuffer0((GlAbstractBuffer) buffer);
    }

    private void deleteBuffer0(GlAbstractBuffer buffer) {
        int handle = buffer.handle();
        buffer.invalidateHandle();

        GL20C.glDeleteBuffers(handle);
    }

    @Override
    public void deleteProgram(Program<?> program) {
        this.deleteProgram0((GlProgram<?>) program);
    }

    private void deleteProgram0(GlProgram<?> program) {
        GL20C.glDeleteProgram(program.handle());
        program.invalidateHandle();
    }

    @Override
    public Fence createFence() {
        return new GlFence(GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
    }

    @Override
    public RenderDeviceProperties properties() {
        return this.properties;
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(PipelineDescription state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArrayDescription) {
        var vertexArray = new GlVertexArray<>(GL45C.glCreateVertexArrays(), vertexArrayDescription);

        return new GlPipeline<>(state, program, vertexArray);
    }

    @Override
    public <T> Program<T> createProgram(ShaderDescription desc, Function<ShaderBindingContext, T> interfaceFactory) {
        return new GlProgram<>(desc, interfaceFactory);
    }

    @Override
    public ImmutableBuffer createBuffer(ByteBuffer data, Set<ImmutableBufferFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, data, getBufferStorageBits(flags));

        return new GlImmutableBuffer(handle, data.capacity(), flags);
    }

    @Override
    public ImmutableBuffer createBuffer(long capacity, Set<ImmutableBufferFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, getBufferStorageBits(flags));

        return new GlImmutableBuffer(handle, capacity, flags);
    }

    @Override
    public ImmutableBuffer createBuffer(long capacity, Consumer<ByteBuffer> consumer, Set<ImmutableBufferFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, GL45C.GL_MAP_WRITE_BIT | getBufferStorageBits(flags));

        var mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity,
                GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT | GL45C.GL_MAP_WRITE_BIT);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer for writing");
        }

        consumer.accept(mapping);

        if (!GL45C.glUnmapNamedBuffer(handle)) {
            throw new RuntimeException("Failed to unmap buffer after writing data (contents corrupt?)");
        }

        return new GlImmutableBuffer(handle, capacity, flags);
    }

    @Override
    public DynamicBuffer createDynamicBuffer(long capacity, Set<DynamicBufferFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, getDynamicBufferStorageBits(flags));

        return new GlDynamicBuffer(handle, capacity, flags);
    }

    @Override
    public MappedBuffer createMappedBuffer(long capacity, Set<MappedBufferFlags> flags) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(flags.contains(MappedBufferFlags.READ) || flags.contains(MappedBufferFlags.WRITE),
                    "Read-only, write-only, or read-write flags must be specified");
        }

        var storage = GL45C.GL_MAP_PERSISTENT_BIT | getMappedBufferStorageBits(flags);
        var access = GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT | getMappedBufferAccessBits(flags);

        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, storage);

        ByteBuffer data = GL45C.glMapNamedBufferRange(handle, 0, capacity, access);

        if (data == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        return new GlMappedBuffer(handle, data, flags);
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate) {
        this.pipelineManager.bindPipeline(pipeline, (state) -> {
            gate.run(new ImmediateRenderCommandList<>((GlVertexArray<ARRAY>) pipeline.getVertexArray()), pipeline.getProgram().getInterface(), state);
        });
    }

    @Override
    public void deletePipeline(Pipeline<?, ?> pipeline) {
        this.deleteVertexArray(pipeline.getVertexArray());
    }

    @Override
    public void updateBuffer(DynamicBuffer buffer, int offset, ByteBuffer data) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(offset >= 0, "Offset must be greater than or equal to zero");
            Validate.isTrue(data != null && data.remaining() > 0, "Data must not be null");
            Validate.isTrue(offset + data.remaining() > buffer.capacity(), "Range is out of bounds");
        }

        GL45C.glNamedBufferSubData(GlAbstractBuffer.handle(buffer), offset, data);
    }

    @Override
    public Sampler createSampler() {
        return new GlSampler();
    }

    @Override
    public void deleteSampler(Sampler sampler) {
        this.deleteSampler0((GlSampler) sampler);
    }

    private void deleteSampler0(GlSampler sampler) {
        GL45C.glDeleteSamplers(sampler.handle());
        sampler.invalidateHandle();
    }

    @Override
    public void deleteVertexArray(VertexArray<?> array) {
        this.deleteVertexArray0((GlVertexArray<?>) array);
    }

    private void deleteVertexArray0(GlVertexArray<?> array) {
        GL30C.glDeleteVertexArrays(array.handle());
        array.invalidateHandle();
    }

    private static class ImmediateRenderCommandList<T extends Enum<T>> implements RenderCommandList<T> {
        private final int array;

        private Buffer elementBuffer;
        private Buffer commandBuffer;

        private final Buffer[] vertexBuffers;

        public ImmediateRenderCommandList(GlVertexArray<T> array) {
            this.array = array.handle();
            this.vertexBuffers = new Buffer[array.getBufferTargets().length];
        }

        @Override
        public void bindElementBuffer(Buffer buffer) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(buffer, "Buffer must be non-null");
            }

            if (this.elementBuffer != buffer) {
                GL45C.glVertexArrayElementBuffer(this.array, GlAbstractBuffer.handle(buffer));

                this.elementBuffer = buffer;
            }
        }

        @Override
        public void bindVertexBuffer(T target, Buffer buffer, int offset, int stride) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(buffer, "Buffer must be non-null");
                Validate.isTrue(offset >= 0, "Buffer offset must be greater than or equal to zero");
                Validate.isTrue(stride > 0, "Buffer stride must be must be positive");
                Validate.isTrue((offset + stride) <= buffer.capacity(),
                        "Buffer must contain at least one element of <stride> bytes");
            }

            int index = target.ordinal();

            if (this.vertexBuffers[index] != buffer) {
                GL45C.glVertexArrayVertexBuffer(this.array, index,
                        GlAbstractBuffer.handle(buffer), offset, stride);

                this.vertexBuffers[index] = buffer;
            }
        }

        @Override
        public void bindCommandBuffer(Buffer buffer) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(buffer, "Buffer must be non-null");
            }

            if (this.commandBuffer != buffer) {
                GL45C.glBindBuffer(GL45C.GL_DRAW_INDIRECT_BUFFER, GlAbstractBuffer.handle(buffer));

                this.commandBuffer = buffer;
            }
        }

        @Override
        public void multiDrawElementsIndirect(PrimitiveType primitiveType, ElementFormat elementType, int indirectOffset, int indirectCount) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(this.elementBuffer, "Element buffer target not bound");
                Validate.notNull(this.commandBuffer, "Command buffer target not bound");
                Validate.noNullElements(this.vertexBuffers, "One or more vertex buffer targets are not bound");

                Validate.isTrue(indirectOffset >= 0, "Command offset must be greater than or equal to zero");
                Validate.isTrue(indirectCount > 0, "Command count must be positive");
                Validate.isTrue(indirectOffset + (indirectCount * 20L) <= this.commandBuffer.capacity(),
                        "Command buffer range is out of bounds");
            }

            GL43C.glMultiDrawElementsIndirect(GlEnum.from(primitiveType), GlEnum.from(elementType), indirectOffset, indirectCount, 0);
        }
    }

    private static int getBufferStorageBits(Set<ImmutableBufferFlags> flags) {
        int bits = 0;

        if (flags.contains(ImmutableBufferFlags.CLIENT_STORAGE)) {
            bits |= GL45C.GL_CLIENT_STORAGE_BIT;
        }

        return bits;
    }

    private static int getDynamicBufferStorageBits(Set<DynamicBufferFlags> flags) {
        int bits = 0;

        if (flags.contains(DynamicBufferFlags.CLIENT_STORAGE)) {
            bits |= GL45C.GL_CLIENT_STORAGE_BIT;
        }

        return bits;
    }

    private static int getMappedBufferStorageBits(Set<MappedBufferFlags> flags) {
        int storage = 0;

        if (flags.contains(MappedBufferFlags.READ)) {
            storage |= GL45C.GL_MAP_READ_BIT;
        }

        if (flags.contains(MappedBufferFlags.WRITE)) {
            storage |= GL45C.GL_MAP_WRITE_BIT;
        }

        if (!flags.contains(MappedBufferFlags.EXPLICIT_FLUSH)) {
            storage |= GL45C.GL_MAP_COHERENT_BIT;
        }

        return storage;
    }

    private static int getMappedBufferAccessBits(Set<MappedBufferFlags> flags) {
        int access = 0;

        if (flags.contains(MappedBufferFlags.READ)) {
            access |= GL45C.GL_MAP_READ_BIT;
        }

        if (flags.contains(MappedBufferFlags.WRITE)) {
            access |= GL45C.GL_MAP_WRITE_BIT;
        }

        if (flags.contains(MappedBufferFlags.EXPLICIT_FLUSH)) {
            access |= GL45C.GL_MAP_FLUSH_EXPLICIT_BIT;
        } else {
            access |= GL45C.GL_MAP_COHERENT_BIT;
        }

        return access;
    }
}
