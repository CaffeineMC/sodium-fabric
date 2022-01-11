package me.jellysquid.mods.sodium.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.render.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.render.buffer.VertexRange;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferBuilder;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.DefaultChunkMeshBuilder;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPassManager;
import me.jellysquid.mods.sodium.util.NativeBuffer;
import net.minecraft.client.render.RenderLayer;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class TerrainBuildBuffers {
    private final Map<ChunkRenderPass, ChunkMeshBuilder> delegates;
    private final Map<ChunkRenderPass, VertexBufferBuilder[]> vertexBuffers;

    private final TerrainVertexType vertexType;

    private final ChunkRenderPassManager renderPassManager;

    public TerrainBuildBuffers(TerrainVertexType vertexType, ChunkRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new Reference2ReferenceOpenHashMap<>();

        this.vertexBuffers = new Reference2ReferenceOpenHashMap<>();

        for (var renderPass : renderPassManager.getAllRenderPasses()) {
            var vertexBuffers = new VertexBufferBuilder[ChunkMeshFace.COUNT];

            for (int i = 0; i < vertexBuffers.length; i++) {
                vertexBuffers[i] = new VertexBufferBuilder(this.vertexType.getBufferVertexFormat(), 512 * 1024);
            }

            this.vertexBuffers.put(renderPass, vertexBuffers);
        }
    }

    public void init(ChunkRenderData.Builder renderData) {
        for (var renderPass : this.renderPassManager.getAllRenderPasses()) {
            var buffers = this.vertexBuffers.get(renderPass);
            var sinks = new TerrainVertexSink[buffers.length];

            for (int i = 0; i < sinks.length; i++) {
                var buffer = buffers[i];
                buffer.reset();

                sinks[i] = this.vertexType.createBufferWriter(buffer);
            }

            this.delegates.put(renderPass, new DefaultChunkMeshBuilder(sinks, renderData));
        }
    }

    /**
     * Return the {@link ChunkMeshBuilder} for the given {@link RenderLayer} as mapped by the
     * {@link ChunkRenderPassManager} for this render context.
     */
    public ChunkMeshBuilder get(RenderLayer layer) {
        return this.delegates.get(this.renderPassManager.getRenderPassForLayer(layer));
    }

    public Map<ChunkRenderPass, ChunkMesh> createMeshes() {
        var map = new Reference2ReferenceOpenHashMap<ChunkRenderPass, ChunkMesh>();

        for (var renderPass : this.renderPassManager.getAllRenderPasses()) {
            var mesh = this.createMesh(renderPass);

            if (mesh != null) {
                map.put(renderPass, mesh);
            }
        }

        return map;
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    private ChunkMesh createMesh(ChunkRenderPass pass) {
        var bufferBuilders = this.vertexBuffers.get(pass);
        var totalVertexCount = Arrays.stream(bufferBuilders)
                .mapToInt(VertexBufferBuilder::getCount)
                .sum();

        if (totalVertexCount <= 0) {
            return null;
        }

        Map<ChunkMeshFace, VertexRange> ranges = new EnumMap<>(ChunkMeshFace.class);
        NativeBuffer mergedVertexBuffer = new NativeBuffer(totalVertexCount * this.vertexType.getBufferVertexFormat().getStride());

        ByteBuffer mergedBufferBuilder = mergedVertexBuffer.getDirectBuffer().slice();
        int vertexCount = 0;

        for (ChunkMeshFace facing : ChunkMeshFace.VALUES) {
            var sidedVertexBuffer = bufferBuilders[facing.ordinal()];
            var sidedVertexCount = sidedVertexBuffer.getCount();

            if (sidedVertexCount == 0) {
                continue;
            }

            mergedBufferBuilder.put(sidedVertexBuffer.slice());
            ranges.put(facing, new VertexRange(vertexCount, sidedVertexCount));
            vertexCount += sidedVertexCount;
        }

        IndexedVertexData vertexData = new IndexedVertexData(this.vertexType.getCustomVertexFormat(), mergedVertexBuffer);

        return new ChunkMesh(vertexData, ranges);
    }
    public void destroy() {
        for (VertexBufferBuilder[] builders : this.vertexBuffers.values()) {
            for (VertexBufferBuilder builder : builders) {
                builder.destroy();
            }
        }
    }
}
