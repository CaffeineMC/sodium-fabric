package me.jellysquid.mods.sodium.client.render.chunk.passes;

import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.CubeMeshBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelMeshBuilder;

import java.util.function.Supplier;

public final class ChunkMeshType<E extends ChunkMeshType.StorageBufferTarget> {
    public static final ChunkMeshType<ModelBufferTarget> MODEL = new ChunkMeshType<>(ModelBufferTarget.class, ModelMeshBuilder::new);
    public static final ChunkMeshType<CubeBufferTarget> CUBE = new ChunkMeshType<>(CubeBufferTarget.class, CubeMeshBuilder::new);

    private final Class<E> storageType;
    private final Supplier<ChunkMeshBuilder<E>> meshBuilderFactory;

    public ChunkMeshType(Class<E> storageType, Supplier<ChunkMeshBuilder<E>> meshBuilderFactory) {
        this.storageType = storageType;
        this.meshBuilderFactory = meshBuilderFactory;
    }

    public ChunkMeshBuilder<E> createMeshBuilder() {
        return this.meshBuilderFactory.get();
    }

    public Class<E> getStorageType() {
        return this.storageType;
    }

    public enum ModelBufferTarget implements StorageBufferTarget {
        VERTICES;

        @Override
        public int getExpectedSize() {
            if (this == ModelBufferTarget.VERTICES) {
                return 3072;
            }

            throw new IllegalArgumentException();
        }
    }

    public enum CubeBufferTarget implements StorageBufferTarget {
        QUADS,
        VERTICES;

        @Override
        public int getExpectedSize() {
            if (this == CubeBufferTarget.QUADS) {
                return 2040;
            } else if (this == CubeBufferTarget.VERTICES) {
                return 1536;
            }

            throw new IllegalArgumentException();
        }
    }

    public interface StorageBufferTarget {
        int getExpectedSize();
    }
}
