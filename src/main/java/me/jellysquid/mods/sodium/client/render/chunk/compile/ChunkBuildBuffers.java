package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.format.*;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayerManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.client.render.RenderLayer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final Map<BlockRenderLayer, Map<ChunkMeshType<?>, ChunkMeshBuilderDelegate<?>>> meshBuilders;
    private final BlockRenderLayerManager renderLayers;

    public ChunkBuildBuffers(BlockRenderLayerManager renderLayers) {
        this.renderLayers = renderLayers;

        this.meshBuilders = new Reference2ReferenceOpenHashMap<>();

        for (BlockRenderLayer renderLayer : renderLayers.getRenderLayers()) {
            Map<ChunkMeshType<?>, ChunkMeshBuilderDelegate<?>> meshBuilders = new Reference2ReferenceOpenHashMap<>();
            meshBuilders.put(ChunkMeshType.MODEL, this.createMeshBuilders(ChunkMeshType.MODEL));
            meshBuilders.put(ChunkMeshType.CUBE, this.createMeshBuilders(ChunkMeshType.CUBE));

            this.meshBuilders.put(renderLayer, meshBuilders);
        }
    }

    private <E extends ChunkMeshType.StorageBufferTarget> ChunkMeshBuilderDelegate<?> createMeshBuilders(ChunkMeshType<E> meshType) {
        EnumMap<ModelQuadFacing, ChunkMeshBuilder<E>> builders = new EnumMap<>(ModelQuadFacing.class);

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            builders.put(facing, meshType.createMeshBuilder());
        }

        return new ChunkMeshBuilderDelegate<>(builders);
    }

    public void reset() {
        for (BlockRenderLayer pass : this.renderLayers.getRenderLayers()) {
            var sinks = this.meshBuilders.get(pass);

            for (var sink : sinks.values()) {
                sink.reset();
            }
        }
    }

    public ChunkMeshBuilderDelegate<?> get(RenderLayer renderLayer, ChunkMeshType<?> meshType) {
        return this.meshBuilders.get(this.renderLayers.getAdapter(renderLayer))
                .get(meshType);
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public Map<ChunkMeshType<?>, ChunkMesh> createMeshes(BlockRenderLayer layer) {
        var builders = this.meshBuilders.get(layer);
        var meshes = new Reference2ReferenceOpenHashMap<ChunkMeshType<?>, ChunkMesh>();

        this.tryAddMesh(meshes, ChunkMeshType.CUBE, builders.get(ChunkMeshType.CUBE));
        this.tryAddMesh(meshes, ChunkMeshType.MODEL, builders.get(ChunkMeshType.MODEL));

        return meshes;
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E> & ChunkMeshType.StorageBufferTarget> void tryAddMesh(Map<ChunkMeshType<?>, ChunkMesh> output,
                                                                                    ChunkMeshType<E> meshType,
                                                                                    ChunkMeshBuilderDelegate<?> delegate) {
        var mesh = this.createMesh(meshType, (ChunkMeshBuilderDelegate<E>) delegate);

        if (mesh != null) {
            output.put(meshType, mesh);
        }
    }

    private <E extends Enum<E> & ChunkMeshType.StorageBufferTarget> ChunkMesh createMesh(ChunkMeshType<E> meshType, ChunkMeshBuilderDelegate<E> delegate) {
        Map<ModelQuadFacing, MeshRange> meshRanges = new EnumMap<>(ModelQuadFacing.class);
        int totalPrimitiveCount = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            var sink = delegate.getSink(facing);

            if (!sink.isEmpty()) {
                meshRanges.put(facing, new MeshRange(totalPrimitiveCount, sink.getPrimitiveCount()));
                totalPrimitiveCount += sink.getPrimitiveCount();
            }
        }

        if (totalPrimitiveCount == 0) {
            return null;
        }

        Map<E, NativeBuffer> meshBuffers = new EnumMap<>(meshType.getStorageType());

        for (E target : meshType.getStorageType().getEnumConstants()) {
            var buffers = new ArrayList<ByteBuffer>();

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                var sink = delegate.getSink(facing);

                if (sink.isEmpty()) {
                    continue;
                }

                buffers.add(sink.getBuffer(target));
            }

            meshBuffers.put(target, mergeBuffers(buffers));
        }

        return new ChunkMesh(new ChunkMeshBuffers<>(meshBuffers), meshRanges);
    }

    private static NativeBuffer mergeBuffers(Collection<ByteBuffer> buffers) {
        var size = buffers.stream()
                .mapToInt(Buffer::remaining)
                .sum();

        var mergedBuffer = NativeBuffer.create(size);
        var offset = 0;

        for (ByteBuffer buffer : buffers) {
            mergedBuffer.getDirectBuffer()
                    .put(offset, buffer, 0, buffer.remaining());

            offset += buffer.remaining();
        }

        return mergedBuffer;
    }

    public void destroy() {
        for (var entry : this.meshBuilders.entrySet()) {
            var builders = entry.getValue();

            for (var builder : builders.values()) {
                builder.destroy();
            }
        }
    }

    public Map<BlockRenderLayer, Map<ChunkMeshType<?>, ChunkMesh>> createMeshes() {
        Map<BlockRenderLayer, Map<ChunkMeshType<?>, ChunkMesh>> collection = new Reference2ReferenceOpenHashMap<>();

        for (BlockRenderLayer layer : this.meshBuilders.keySet()) {
            Map<ChunkMeshType<?>, ChunkMesh> meshes = this.createMeshes(layer);

            if (!meshes.isEmpty()) {
                collection.put(layer, meshes);
            }
        }

        return collection;
    }
}
