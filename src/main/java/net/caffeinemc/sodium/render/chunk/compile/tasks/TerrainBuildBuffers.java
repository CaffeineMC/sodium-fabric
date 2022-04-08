package net.caffeinemc.sodium.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.sodium.render.buffer.VertexData;
import net.caffeinemc.sodium.render.buffer.VertexRange;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.state.ChunkModel;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferBuilder;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.chunk.compile.buffers.DefaultChunkMeshBuilder;
import net.caffeinemc.sodium.render.chunk.compile.buffers.ChunkMeshBuilder;
import net.caffeinemc.sodium.render.chunk.state.BuiltChunkGeometry;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.util.NativeBuffer;
import net.minecraft.client.render.RenderLayer;
import org.apache.commons.lang3.ArrayUtils;

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

    public BuiltChunkGeometry buildGeometry() {
        var capacity = this.vertexBuffers.values()
                .stream()
                .flatMapToInt((vertexBuffers) -> Arrays.stream(vertexBuffers)
                        .mapToInt(VertexBufferBuilder::getCount))
                .sum();

        if (capacity <= 0) {
            return BuiltChunkGeometry.empty();
        }

        var vertexFormat = this.vertexType.getCustomVertexFormat();
        var vertexCount = 0;

        var chunkVertexBuffer = new NativeBuffer(capacity * vertexFormat.stride());
        var chunkVertexBufferBuilder = chunkVertexBuffer.getDirectBuffer().slice();

        var models = new ArrayList<ChunkModel>();

        for (var entry : this.vertexBuffers.entrySet()) {
            var sidedBuffers = entry.getValue();
            var ranges = new VertexRange[ChunkMeshFace.COUNT];

            for (ChunkMeshFace facing : ChunkMeshFace.VALUES) {
                var index = facing.ordinal();

                var sidedVertexBuffer = sidedBuffers[index];
                var sidedVertexCount = sidedVertexBuffer.getCount();

                if (sidedVertexCount == 0) {
                    continue;
                }

                chunkVertexBufferBuilder.put(sidedVertexBuffer.slice());
                ranges[index] = new VertexRange(vertexCount, sidedVertexCount);

                vertexCount += sidedVertexCount;
            }

            if (!ArrayUtils.isEmpty(ranges)) {
                models.add(new ChunkModel(entry.getKey(), ranges));
            }
        }

        VertexData vertexData = new VertexData(vertexFormat, chunkVertexBuffer);

        return new BuiltChunkGeometry(vertexData, models);
    }

    public void destroy() {
        for (VertexBufferBuilder[] builders : this.vertexBuffers.values()) {
            for (VertexBufferBuilder builder : builders) {
                builder.destroy();
            }
        }
    }
}
