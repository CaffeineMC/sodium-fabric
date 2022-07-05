package net.caffeinemc.gfx.opengl.device;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.buffer.*;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.RenderDeviceProperties;
import net.caffeinemc.gfx.api.device.commands.PipelineGate;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderBindingContext;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.texture.parameters.AddressMode;
import net.caffeinemc.gfx.api.texture.parameters.FilterMode;
import net.caffeinemc.gfx.api.texture.parameters.MipmapMode;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.buffer.GlBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlDynamicBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlImmutableBuffer;
import net.caffeinemc.gfx.opengl.buffer.GlMappedBuffer;
import net.caffeinemc.gfx.opengl.pipeline.GlPipeline;
import net.caffeinemc.gfx.opengl.pipeline.GlPipelineManager;
import net.caffeinemc.gfx.opengl.shader.GlProgram;
import net.caffeinemc.gfx.opengl.sync.GlFence;
import net.caffeinemc.gfx.opengl.texture.GlSampler;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.ARBIndirectParameters;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MathUtil;

public class GlRenderDevice implements RenderDevice {
    private final GlPipelineManager pipelineManager;
    private final RenderDeviceProperties properties;
    private final RenderConfiguration renderConfiguration;

    public GlRenderDevice(Function<RenderDeviceProperties, GlPipelineManager> pipelineManagerFactory, RenderConfiguration renderConfiguration) {
        // TODO: move this into platform code
        this.properties = getDeviceProperties();
        this.renderConfiguration = renderConfiguration;
        this.pipelineManager = pipelineManagerFactory.apply(this.properties);
        
        if (renderConfiguration.apiDebug) {
            GlDebug.enableDebugMessages();
        }
    }

    private static RenderDeviceProperties getDeviceProperties() {
        var glCaps = GL.getCapabilities();

        int uniformBufferAlignment = GL45C.glGetInteger(GL45C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
        if (!MathUtil.mathIsPoT(uniformBufferAlignment)) {
            throw new RuntimeException("GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT is not a power-of-two (found value of %s)"
                    .formatted(uniformBufferAlignment));
        }

        int storageBufferAlignment = GL45C.glGetInteger(GL45C.GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT);
        if (!MathUtil.mathIsPoT(storageBufferAlignment)) {
            throw new RuntimeException("GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT is not a power-of-two (found value of %s)"
                    .formatted(storageBufferAlignment));
        }

        int maxCombinedTextureImageUnits = GL45C.glGetInteger(GL45C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);

        String vendorName = GL45C.glGetString(GL45C.GL_VENDOR);
        String deviceName = GL45C.glGetString(GL45C.GL_RENDERER);
        String apiVersion = GL45C.glGetString(GL45C.GL_VERSION);

        boolean isVendorIntel = vendorName != null && vendorName.toLowerCase(Locale.ROOT).contains("intel");
        boolean hasIndirectCountSupport = glCaps.GL_ARB_indirect_parameters;
        boolean forceIndirectCount = isVendorIntel && hasIndirectCountSupport;

        boolean hasShaderDrawParametersSupport = glCaps.GL_ARB_shader_draw_parameters;

        return new RenderDeviceProperties(
                vendorName,
                deviceName,
                "OpenGL",
                apiVersion,
                new RenderDeviceProperties.Values(
                        uniformBufferAlignment,
                        storageBufferAlignment,
                        maxCombinedTextureImageUnits
                ),
                new RenderDeviceProperties.Capabilities(
                        hasIndirectCountSupport,
                        hasShaderDrawParametersSupport

                ),
                new RenderDeviceProperties.DriverWorkarounds(
                        forceIndirectCount
                )
        );
    }

    @Override
    public void copyBuffer(Buffer srcBuffer, Buffer dstBuffer, long srcOffset, long dstOffset, long length) {
        this.copyBuffer0((GlBuffer) srcBuffer, (GlBuffer) dstBuffer, srcOffset, dstOffset, length);
    }

    private void copyBuffer0(GlBuffer srcBuffer, GlBuffer dstBuffer, long srcOffset, long dstOffset, long length) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(srcOffset >= 0, "Source offset must be greater than or equal to zero");
            Validate.isTrue(dstOffset >= 0, "Destination offset must be greater than or equal to zero");

            Validate.isTrue(srcOffset + length <= srcBuffer.capacity(), "Source buffer range is out-of-bounds");
            Validate.isTrue(dstOffset + length <= dstBuffer.capacity(), "Destination buffer range is out-of-bounds");
        }

        GL45C.glCopyNamedBufferSubData(srcBuffer.getHandle(), dstBuffer.getHandle(), srcOffset, dstOffset, length);
    }

    @Override
    public void deleteBuffer(Buffer buffer) {
        this.deleteBuffer0((GlBuffer) buffer);
    }

    private void deleteBuffer0(GlBuffer buffer) {
        int handle = buffer.getHandle();
        buffer.invalidateHandle();

        GL20C.glDeleteBuffers(handle);
    }

    @Override
    public void deleteProgram(Program<?> program) {
        this.deleteProgram0((GlProgram<?>) program);
    }

    private void deleteProgram0(GlProgram<?> program) {
        GL20C.glDeleteProgram(program.getHandle());
        program.invalidateHandle();
    }

    @Override
    public Fence createFence() {
        return new GlFence();
    }

    @Override
    public RenderDeviceProperties properties() {
        return this.properties;
    }
    
    @Override
    public RenderConfiguration configuration() {
        return this.renderConfiguration;
    }
    
    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> Pipeline<PROGRAM, ARRAY> createPipeline(PipelineDescription state, Program<PROGRAM> program, VertexArrayDescription<ARRAY> vertexArrayDescription) {
        var vertexArray = new GlVertexArray<>(vertexArrayDescription);

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
    public ImmutableBuffer createBuffer(long capacity, Consumer<ByteBuffer> preUnmapConsumer, Set<ImmutableBufferFlags> flags) {
        var handle = GL45C.glCreateBuffers();
        GL45C.glNamedBufferStorage(handle, capacity, GL45C.GL_MAP_WRITE_BIT | getBufferStorageBits(flags));

        var mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity,
                GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT | GL45C.GL_MAP_WRITE_BIT);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer for writing");
        }

        preUnmapConsumer.accept(mapping);

        if (!GL45C.glUnmapNamedBuffer(handle)) {
            // TODO: retry if this happens
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

        var handle = GL45C.glCreateBuffers();

        var storage = GL45C.GL_MAP_PERSISTENT_BIT | getMappedBufferStorageBits(flags);
        GL45C.glNamedBufferStorage(handle, capacity, storage);

        var access = GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT | GL45C.GL_MAP_INVALIDATE_BUFFER_BIT | getMappedBufferAccessBits(flags);
        ByteBuffer mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity, access);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        return new GlMappedBuffer(handle, mapping, flags);
    }

    @Override
    public MappedBuffer createMappedBuffer(long capacity, Consumer<Buffer> preMapConsumer, Set<MappedBufferFlags> flags) {
        if (RenderConfiguration.API_CHECKS) {
            Validate.isTrue(flags.contains(MappedBufferFlags.READ) || flags.contains(MappedBufferFlags.WRITE),
                    "Read-only, write-only, or read-write flags must be specified");
        }

        var handle = GL45C.glCreateBuffers();

        var storage = GL45C.GL_MAP_PERSISTENT_BIT | getMappedBufferStorageBits(flags);
        GL45C.glNamedBufferStorage(handle, capacity, storage);

        // just make a temporary generic buffer
        preMapConsumer.accept(new GlBuffer(handle, capacity));
        
        //// Do the synchronization for the buffer ourselves
        // TODO: add a memory barrier function to RenderDevice
        // do we need GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT?
        GL45C.glMemoryBarrier(GL45C.GL_BUFFER_UPDATE_BARRIER_BIT);
        this.createFence().sync(true);

        // If we were to use GL_MAP_INVALIDATE_BIT on this, it would invalidate all the stuff we just wrote to it.
        var access = GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_UNSYNCHRONIZED_BIT | getMappedBufferAccessBits(flags);
        ByteBuffer mapping = GL45C.glMapNamedBufferRange(handle, 0, capacity, access);

        if (mapping == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        return new GlMappedBuffer(handle, mapping, flags);
    }

    @Override
    public <PROGRAM, ARRAY extends Enum<ARRAY>> void usePipeline(Pipeline<PROGRAM, ARRAY> pipeline, PipelineGate<PROGRAM, ARRAY> gate) {
        this.pipelineManager.bindPipeline(pipeline, (state) ->
            gate.run(new ImmediateRenderCommandList<>((GlVertexArray<ARRAY>) pipeline.getVertexArray()), pipeline.getProgram().getInterface(), state)
        );
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

        GL45C.glNamedBufferSubData(GlBuffer.getHandle(buffer), offset, data);
    }

    @Override
    public Sampler createSampler(
            @Nullable FilterMode minFilter,
            @Nullable MipmapMode mipmapMode,
            @Nullable FilterMode magFilter,
            @Nullable AddressMode addressModeU,
            @Nullable AddressMode addressModeV,
            @Nullable AddressMode addressModeW
    ) {
        return new GlSampler(
                minFilter,
                mipmapMode,
                magFilter,
                addressModeU,
                addressModeV,
                addressModeW
        );
    }

    @Override
    public void deleteSampler(Sampler sampler) {
        this.deleteSampler0((GlSampler) sampler);
    }

    private void deleteSampler0(GlSampler sampler) {
        GL45C.glDeleteSamplers(sampler.getHandle());
        sampler.invalidateHandle();
    }

    @Override
    public void deleteVertexArray(VertexArray<?> array) {
        this.deleteVertexArray0((GlVertexArray<?>) array);
    }

    private void deleteVertexArray0(GlVertexArray<?> array) {
        GL30C.glDeleteVertexArrays(array.getHandle());
        array.invalidateHandle();
    }

    private static class ImmediateRenderCommandList<T extends Enum<T>> implements RenderCommandList<T> {
        protected final int array;

        protected Buffer elementBuffer;
        protected Buffer commandBuffer;
        protected Buffer parameterBuffer;

        protected final Buffer[] vertexBuffers;

        public ImmediateRenderCommandList(GlVertexArray<T> array) {
            this.array = array.getHandle();
            this.vertexBuffers = new Buffer[array.getBufferTargets().length];
        }

        @Override
        public void bindElementBuffer(Buffer buffer) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(buffer, "Buffer must be non-null");
            }

            if (this.elementBuffer != buffer) {
                GL45C.glVertexArrayElementBuffer(this.array, GlBuffer.getHandle(buffer));

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
                        GlBuffer.getHandle(buffer), offset, stride);

                this.vertexBuffers[index] = buffer;
            }
        }

        @Override
        public void bindCommandBuffer(Buffer buffer) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(buffer, "Buffer must be non-null");
            }

            if (this.commandBuffer != buffer) {
                GL45C.glBindBuffer(GL45C.GL_DRAW_INDIRECT_BUFFER, GlBuffer.getHandle(buffer));

                this.commandBuffer = buffer;
            }
        }

        @Override
        public void bindParameterBuffer(Buffer buffer) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(buffer, "Buffer must be non-null");
            }

            if (this.parameterBuffer != buffer) {
                GL45C.glBindBuffer(ARBIndirectParameters.GL_PARAMETER_BUFFER_ARB, GlBuffer.getHandle(buffer));

                this.parameterBuffer = buffer;
            }
        }

        @Override
        public void multiDrawElementsIndirect(PrimitiveType primitiveType, ElementFormat elementType, long indirectOffset, int indirectCount, int stride) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(this.elementBuffer, "Element buffer target not bound");
                Validate.notNull(this.commandBuffer, "Command buffer target not bound");
                Validate.noNullElements(this.vertexBuffers, "One or more vertex buffer targets are not bound");

                Validate.isTrue(indirectOffset >= 0, "Command offset must be greater than or equal to zero");
                Validate.isTrue(indirectCount > 0, "Command count must be positive");
                Validate.isTrue(indirectOffset + (indirectCount * 20L) <= this.commandBuffer.capacity(),
                        "Command buffer range is out of bounds");
                Validate.isTrue(stride >= 0, "Stride must be greater than or equal to 0");
            }

            GL43C.glMultiDrawElementsIndirect(GlEnum.from(primitiveType), GlEnum.from(elementType), indirectOffset, indirectCount, stride);
        }

        @Override
        public void multiDrawElementsIndirectCount(PrimitiveType primitiveType, ElementFormat elementType, long indirectOffset, long countOffset, int maxCount, int stride) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.notNull(this.elementBuffer, "Element buffer target not bound");
                Validate.notNull(this.commandBuffer, "Command buffer target not bound");
                Validate.noNullElements(this.vertexBuffers, "One or more vertex buffer targets are not bound");

                Validate.isTrue(indirectOffset >= 0, "Command offset must be greater than or equal to zero");
                Validate.isTrue(maxCount > 0, "Maximum command count must be greater than zero");
                Validate.isTrue(countOffset % 4 == 0, "Count offset is not a multiple of 4");
                Validate.isTrue(stride >= 0, "Stride must be greater than or equal to 0");
            }

            ARBIndirectParameters.glMultiDrawElementsIndirectCountARB(
                    GlEnum.from(primitiveType),
                    GlEnum.from(elementType),
                    indirectOffset,
                    countOffset,
                    maxCount,
                    stride
            );
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
        int bits = 0;

        if (flags.contains(MappedBufferFlags.READ)) {
            bits |= GL45C.GL_MAP_READ_BIT;
        }

        if (flags.contains(MappedBufferFlags.WRITE)) {
            bits |= GL45C.GL_MAP_WRITE_BIT;
            // TODO: the spec and the wiki conflict on how this should be used.
            // the spec says that this is needed for ANY client modification, but the wiki says that this is only needed
            // for if you're going to be using glBufferSubData. for now, i'm going to exclude it.
//            bits |= GL45C.GL_DYNAMIC_STORAGE_BIT;
        }

        if (!flags.contains(MappedBufferFlags.EXPLICIT_FLUSH)) {
            bits |= GL45C.GL_MAP_COHERENT_BIT;
        }

        if (flags.contains(MappedBufferFlags.CLIENT_STORAGE)) {
            bits |= GL45C.GL_CLIENT_STORAGE_BIT;
        }

        return bits;
    }

    private static int getMappedBufferAccessBits(Set<MappedBufferFlags> flags) {
        int bits = 0;

        if (flags.contains(MappedBufferFlags.READ)) {
            bits |= GL45C.GL_MAP_READ_BIT;
        }

        if (flags.contains(MappedBufferFlags.WRITE)) {
            bits |= GL45C.GL_MAP_WRITE_BIT;
        }

        if (flags.contains(MappedBufferFlags.EXPLICIT_FLUSH)) {
            bits |= GL45C.GL_MAP_FLUSH_EXPLICIT_BIT;
        } else {
            bits |= GL45C.GL_MAP_COHERENT_BIT;
        }

        return bits;
    }
}
