package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import java.util.Arrays;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Static normal relative sorting orders quads by the dot product of their
 * normal and position. (referred to as "distance" throughout the code)
 */
public class StaticNormalRelativeData extends SplitDirectionData {
    public StaticNormalRelativeData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos, buffer, ranges);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_NORMAL_RELATIVE;
    }

    private static StaticNormalRelativeData fromDoubleUnaligned(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos) {
        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        IntBuffer bufferBuilder = buffer.getDirectBuffer().asIntBuffer();

        long[] sortData = new long[quads.length];

        for (int quadIndex = 0; quadIndex < quads.length; quadIndex++) {
            int dotProductComponent = MathUtil.floatToComparableInt(quads[quadIndex].getDotProduct());
            sortData[quadIndex] = (long) dotProductComponent << 32 | quadIndex;
        }

        Arrays.sort(sortData);

        for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(bufferBuilder, (int) sortData[i]);
        }

        return new StaticNormalRelativeData(sectionPos, buffer, translucentMesh.getVertexRanges());
    }

    /**
     * Important: The vertex indexes must start at zero for each facing.
     */
    private static StaticNormalRelativeData fromMixed(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos) {
        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        IntBuffer bufferBuilder = buffer.getDirectBuffer().asIntBuffer();

        var ranges = translucentMesh.getVertexRanges();
        var maxRangeSize = 0;
        for (var range : ranges) {
            if (range != null) {
                maxRangeSize = Math.max(maxRangeSize, range.vertexCount());
            }
        }

        long[] sortData = new long[TranslucentData.vertexCountToQuadCount(maxRangeSize)];

        int quadIndex = 0;
        for (var range : ranges) {
            if (range == null) {
                continue;
            }

            int count = TranslucentData.vertexCountToQuadCount(range.vertexCount());

            for (int i = 0; i < count; i++) {
                var quad = quads[quadIndex++];
                int dotProductComponent = MathUtil.floatToComparableInt(quad.getDotProduct());
                sortData[i] = (long) dotProductComponent << 32 | i;
            }

            if (count > 1) {
                Arrays.sort(sortData, 0, count);
            }

            for (int i = 0; i < count; i++) {
                TranslucentData.writeQuadVertexIndexes(bufferBuilder, (int) sortData[i]);
            }
        }

        return new StaticNormalRelativeData(sectionPos, buffer, ranges);
    }

    public static StaticNormalRelativeData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos, boolean isDoubleUnaligned) {
        if (isDoubleUnaligned) {
            return fromDoubleUnaligned(translucentMesh, quads, sectionPos);
        } else {
            return fromMixed(translucentMesh, quads, sectionPos);
        }
    }
}
