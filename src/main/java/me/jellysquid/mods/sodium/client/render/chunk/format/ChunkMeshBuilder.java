package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;

import java.nio.ByteBuffer;

public interface ChunkMeshBuilder<E extends ChunkMeshType.StorageBufferTarget> extends ModelQuadSink {
    void reset();

    void destroy();

    ByteBuffer getBuffer(E target);

    int getPrimitiveCount();

    default boolean isEmpty() {
        return this.getPrimitiveCount() == 0;
    }
}
