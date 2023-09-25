package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * With this sort type the section's translucent quads can be rendered in any
 * order. However, they do need to be rendered with some index buffer, so that
 * vertices are assembled into quads. Since the sort order doesn't matter, all
 * sections with this sort type can share the same data in the index buffer.
 * 
 * TODO: share the buffer on the CPU and on the GPU. This is essentially the
 * same as SharedQuadIndexBuffer but it has to be compatible with sections in
 * the same region using custom index buffers.
 */
public class AnyOrderData extends SplitDirectionData {
    AnyOrderData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos, buffer, ranges);
    }

    @Override
    public SortType getSortType() {
        return SortType.NONE;
    }

    static AnyOrderData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos) {
        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        var indexBuffer = buffer.getDirectBuffer().asIntBuffer();
        for (int i = 0; i < quads.length; i++) {
            TranslucentData.putQuadVertexIndexes(indexBuffer, i);
        }
        return new AnyOrderData(sectionPos, buffer, translucentMesh.getVertexRanges());
    }
}
