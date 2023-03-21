package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.apache.commons.lang3.Validate;

public class SectionRenderData {
    protected final GlBufferSegment buffer;

    public final int modelPosX;

    public final int modelPosY;

    public final int modelPosZ;

    public final int modelNegX;

    public final int modelNegY;

    public final int modelNegZ;

    public final int modelUnassigned;

    public SectionRenderData(GlBufferSegment buffer, BuiltSectionMeshParts meshData) {
        Validate.notNull(buffer);

        this.buffer = buffer;

        this.modelPosX = getPrimitiveCount(meshData, ModelQuadFacing.POS_X);
        this.modelPosY = getPrimitiveCount(meshData, ModelQuadFacing.POS_Y);
        this.modelPosZ = getPrimitiveCount(meshData, ModelQuadFacing.POS_Z);

        this.modelNegX = getPrimitiveCount(meshData, ModelQuadFacing.NEG_X);
        this.modelNegY = getPrimitiveCount(meshData, ModelQuadFacing.NEG_Y);
        this.modelNegZ = getPrimitiveCount(meshData, ModelQuadFacing.NEG_Z);

        this.modelUnassigned = getPrimitiveCount(meshData, ModelQuadFacing.UNASSIGNED);
    }

    private static int getPrimitiveCount(BuiltSectionMeshParts meshData, int facing) {
        VertexRange range = meshData.getParts()[facing];

        if (range == null) {
            return 0;
        }

        return range.vertexCount() >> 2;
    }

    public GlBufferSegment getBuffer() {
        return this.buffer;
    }
}
