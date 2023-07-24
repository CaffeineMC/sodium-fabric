package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import org.apache.commons.lang3.Validate;

public class ChunkGraphicsState {
    private final GlBufferSegment vertexSegment;

    private final int originX, originY, originZ;

    private int baseVertex;
    private final int vertexCount;
    private final int[] sliceRanges;

    private final int sliceMask;

    public ChunkGraphicsState(RenderSection section, GlBufferSegment vertexSegment, ChunkMeshData data) {
        Validate.notNull(vertexSegment);

        this.vertexSegment = vertexSegment;
        this.vertexCount = data.getVertexCount();

        this.sliceRanges = new int[ModelQuadFacing.COUNT];

        int sliceMask = 0;
        int elementOffset = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            var entry = data.getPart(facing);

            if (entry == null) {
                continue;
            }

            var elementStart = (entry.vertexStart() >> 2) * 6;
            var elementCount = (entry.vertexCount() >> 2) * 6;

            if (elementOffset != elementStart) {
                throw new IllegalStateException("Model parts are not sorted by quad facing");
            }

            this.sliceRanges[facing.ordinal()] = elementCount;

            elementOffset += elementCount;
            sliceMask |= 1 << facing.ordinal();
        }

        this.sliceMask = sliceMask;

        this.originX = section.getChunkX() << 4;
        this.originY = section.getChunkY() << 4;
        this.originZ = section.getChunkZ() << 4;

        this.refresh();
    }

    public void refresh() {
        this.baseVertex = this.vertexSegment.getOffset();
    }

    public void delete() {
        this.vertexSegment.delete();
    }

    public int getSliceMask() {
        return this.sliceMask;
    }
    
    public int getSliceRange(int facing) {
        return this.sliceRanges[facing];
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public int getMinX() {
        return this.originX;
    }

    public int getMaxX() {
        return this.originX + 16;
    }

    public int getMinY() {
        return this.originY;
    }

    public int getMaxY() {
        return this.originY + 16;
    }

    public int getMinZ() {
        return this.originZ;
    }

    public int getMaxZ() {
        return this.originZ + 16;
    }

    public int getBaseVertex() {
        return this.baseVertex;
    }
}
