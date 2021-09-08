package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.buffer.BufferStorageFlags;
import me.jellysquid.mods.thingl.buffer.ImmutableBuffer;
import me.jellysquid.mods.thingl.buffer.MutableBuffer;
import me.jellysquid.mods.thingl.shader.*;
import me.jellysquid.mods.thingl.sync.Fence;
import me.jellysquid.mods.thingl.tessellation.PrimitiveType;
import me.jellysquid.mods.thingl.tessellation.Tessellation;
import me.jellysquid.mods.thingl.tessellation.TessellationBinding;
import me.jellysquid.mods.thingl.texture.Sampler;
import me.jellysquid.mods.thingl.texture.Texture;
import me.jellysquid.mods.thingl.util.EnumBitField;

import java.util.function.Function;

public interface ResourceFactory {
    Shader createShader(ShaderType type, String source);

    <T> Program<T> createProgram(Shader[] shaders, Function<ShaderBindingContext, T> interfaceFactory);

    Tessellation createTessellation(PrimitiveType primitiveType, TessellationBinding[] bindings);

    MutableBuffer createMutableBuffer();

    ImmutableBuffer createImmutableBuffer(long bufferSize, EnumBitField<BufferStorageFlags> flags);

    Sampler createSampler();

    Fence createFence();

    Texture createTexture();
}
