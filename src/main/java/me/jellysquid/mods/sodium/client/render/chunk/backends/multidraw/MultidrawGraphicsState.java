package me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;

import java.util.Map;

public class MultidrawGraphicsState extends ChunkGraphicsState {
    private final ChunkRegion<MultidrawGraphicsState> region;

    private final GlBufferSegment vertexSegment;
    private final GlBufferSegment indexSegment;

    private final ElementRange[] parts;

    public MultidrawGraphicsState(ChunkRenderContainer<?> container, ChunkRegion<MultidrawGraphicsState> region, GlBufferSegment vertexSegment, GlBufferSegment indexSegment, ChunkMeshData meshData) {
        super(container);

        this.region = region;
        this.vertexSegment = vertexSegment;
        this.indexSegment = indexSegment;

        this.parts = new ElementRange[ModelQuadFacing.COUNT];

        for (Map.Entry<ModelQuadFacing, ElementRange> entry : meshData.getSlices()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
        }
    }

    @Override
    public void delete(CommandList commandList) {
        this.vertexSegment.delete();
        this.indexSegment.delete();
    }

    public ChunkRegion<MultidrawGraphicsState> getRegion() {
        return this.region;
    }

    public ElementRange getModelPart(int facing) {
        return this.parts[facing];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public GlBufferSegment getIndexSegment() {
        return this.indexSegment;
    }
}
