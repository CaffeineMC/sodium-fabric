package me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.RenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.RenderRegion;

import java.util.Map;

public class ChunkGraphicsState {
    private final int x, y, z;

    private final RenderRegion region;

    private final GlBufferSegment vertexSegment;
    private final GlBufferSegment indexSegment;

    private final ElementRange[] parts;

    public ChunkGraphicsState(RenderChunk container, RenderRegion region, GlBufferSegment vertexSegment, GlBufferSegment indexSegment, ChunkMeshData meshData) {
        this.x = container.getRenderX();
        this.y = container.getRenderY();
        this.z = container.getRenderZ();

        this.region = region;
        this.vertexSegment = vertexSegment;
        this.indexSegment = indexSegment;

        this.parts = new ElementRange[ModelQuadFacing.COUNT];

        for (Map.Entry<ModelQuadFacing, ElementRange> entry : meshData.getSlices()) {
            this.parts[entry.getKey().ordinal()] = entry.getValue();
        }
    }

    public void delete() {
        this.vertexSegment.delete();
        this.indexSegment.delete();
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public ElementRange getModelPart(ModelQuadFacing facing) {
        return this.parts[facing.ordinal()];
    }

    public GlBufferSegment getVertexSegment() {
        return this.vertexSegment;
    }

    public GlBufferSegment getIndexSegment() {
        return this.indexSegment;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }
}
