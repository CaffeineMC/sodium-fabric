package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.buffer.*;
import me.jellysquid.mods.thingl.shader.GlProgram;
import me.jellysquid.mods.thingl.shader.GlShader;
import me.jellysquid.mods.thingl.shader.ShaderBindingContext;
import me.jellysquid.mods.thingl.shader.ShaderType;
import me.jellysquid.mods.thingl.sync.GlFence;
import me.jellysquid.mods.thingl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.thingl.tessellation.GlTessellation;
import me.jellysquid.mods.thingl.tessellation.TessellationBinding;
import me.jellysquid.mods.thingl.util.EnumBitField;
import net.minecraft.util.Identifier;

import java.nio.ByteBuffer;
import java.util.function.Function;

public interface CommandList extends AutoCloseable {
    GlMutableBuffer createMutableBuffer();

    GlImmutableBuffer createImmutableBuffer(long bufferSize, EnumBitField<GlBufferStorageFlags> flags);

    GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings);

    GlShader createShader(ShaderType type, String source);

    <T> GlProgram<T> createProgram(GlShader[] shaders, Function<ShaderBindingContext, T> interfaceFactory);

    void bindVertexArray(GlVertexArray array);

    void uploadData(GlMutableBuffer glBuffer, ByteBuffer byteBuffer, GlBufferUsage usage);

    void copyBufferSubData(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes);

    void bindBuffer(GlBufferTarget target, GlBuffer buffer);

    void allocateStorage(GlMutableBuffer buffer, long bufferSize, GlBufferUsage usage);

    void deleteBuffer(GlBuffer buffer);

    void deleteVertexArray(GlVertexArray vertexArray);

    void flush();

    DrawCommandList beginTessellating(GlTessellation tessellation);

    void deleteTessellation(GlTessellation tessellation);

    @Override
    default void close() {
        this.flush();
    }

    GlBufferMapping mapBuffer(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags);

    void unmap(GlBufferMapping map);

    void flushMappedRange(GlBufferMapping map, int offset, int length);

    GlFence createFence();
}
