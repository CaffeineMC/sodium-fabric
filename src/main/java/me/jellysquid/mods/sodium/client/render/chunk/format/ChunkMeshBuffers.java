package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;

import java.util.Collections;
import java.util.Map;

public class ChunkMeshBuffers<E> {
    private final Map<E, NativeBuffer> storageBuffers;

    public ChunkMeshBuffers(Map<E, NativeBuffer> storageBuffers) {
        this.storageBuffers = Collections.unmodifiableMap(storageBuffers);
    }

    public Iterable<Map.Entry<E, NativeBuffer>> getStorageBuffers() {
        return this.storageBuffers.entrySet();
    }

    @Deprecated
    public void delete() {
        for (var buffer : this.storageBuffers.values()) {
            buffer.free();
        }
    }
}
