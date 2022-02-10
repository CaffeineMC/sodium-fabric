package me.jellysquid.mods.sodium.render.vertex.type;

import me.jellysquid.mods.sodium.render.vertex.VertexSink;

/**
 * A blittable {@link VertexType} which supports direct copying into a {@link com.mojang.blaze3d.vertex.BufferBuilder}
 * provided the buffer's vertex format matches that required by the {@link VertexSink}.
 *
 * @param <T> The {@link VertexSink} type this factory produces
 */
public interface BufferVertexType<T extends VertexSink> extends VertexType<T> {
    BufferVertexFormat getBufferVertexFormat();
}
