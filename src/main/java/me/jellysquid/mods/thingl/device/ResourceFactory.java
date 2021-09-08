package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.thingl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.thingl.buffer.GlMutableBuffer;
import me.jellysquid.mods.thingl.shader.GlProgram;
import me.jellysquid.mods.thingl.shader.GlShader;
import me.jellysquid.mods.thingl.shader.ShaderBindingContext;
import me.jellysquid.mods.thingl.shader.ShaderType;
import me.jellysquid.mods.thingl.sync.GlFence;
import me.jellysquid.mods.thingl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.thingl.tessellation.GlTessellation;
import me.jellysquid.mods.thingl.tessellation.TessellationBinding;
import me.jellysquid.mods.thingl.texture.GlSampler;
import me.jellysquid.mods.thingl.texture.GlTexture;
import me.jellysquid.mods.thingl.util.EnumBitField;

import java.util.function.Function;

public interface ResourceFactory {
    GlShader createShader(ShaderType type, String source);

    <T> GlProgram<T> createProgram(GlShader[] shaders, Function<ShaderBindingContext, T> interfaceFactory);

    GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings);

    GlMutableBuffer createMutableBuffer();

    GlImmutableBuffer createImmutableBuffer(long bufferSize, EnumBitField<GlBufferStorageFlags> flags);

    GlSampler createSampler();

    GlFence createFence();

    GlTexture createTexture();
}
