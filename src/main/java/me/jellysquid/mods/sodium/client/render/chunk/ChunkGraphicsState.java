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
    private final int[] modelParts;

    private final int nonEmptyModelParts;

    public ChunkGraphicsState(RenderSection section, GlBufferSegment vertexSegment, ChunkMeshData data) {
        Validate.notNull(vertexSegment);

        this.vertexSegment = vertexSegment;
        this.vertexCount = data.getVertexCount();

        this.modelParts = new int[ModelQuadFacing.COUNT];

        int flags = 0;
        int offset = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            var entry = data.getPart(facing);

            if (entry == null) {
                continue;
            }

            var count = entry.vertexCount();

            if (offset != entry.vertexStart()) {
                throw new IllegalStateException("Model parts are not sorted by quad facing");
            }

            this.modelParts[facing.ordinal()] = count;

            offset += count;
            flags |= 1 << facing.ordinal();
        }

        this.nonEmptyModelParts = flags;

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

    public int getNonEmptyModelParts() {
        return this.nonEmptyModelParts;
    }

    public int[] getModelParts() {
        return this.modelParts;
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
