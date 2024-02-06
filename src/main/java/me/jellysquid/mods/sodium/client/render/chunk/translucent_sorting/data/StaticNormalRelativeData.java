package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import java.util.Arrays;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.sorting.RadixSort;
import net.minecraft.core.SectionPos;

/**
 * Static normal relative sorting orders quads by the dot product of their
 * normal and position. (referred to as "distance" throughout the code)
 * 
 * Unlike sorting by distance, which is descending for translucent rendering to
 * be correct, sorting by dot product is ascending instead.
 */
public class StaticNormalRelativeData extends SplitDirectionData {
    public StaticNormalRelativeData(SectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos, buffer, ranges);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_NORMAL_RELATIVE;
    }

    private static StaticNormalRelativeData fromDoubleUnaligned(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, SectionPos sectionPos) {
        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        IntBuffer indexBuffer = buffer.getDirectBuffer().asIntBuffer();

        if (quads.length <= 1) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
        } else if (RadixSort.useRadixSort(quads.length)) {
            final var keys = new int[quads.length];

            for (int q = 0; q < quads.length; q++) {
                keys[q] = MathUtil.floatToComparableInt(quads[q].getDotProduct());
            }

            var indices = RadixSort.sort(keys);

            for (int i = 0; i < quads.length; i++) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, indices[i]);
            }
        } else {
            final var sortData = new long[quads.length];

            for (int q = 0; q < quads.length; q++) {
                int dotProductComponent = MathUtil.floatToComparableInt(quads[q].getDotProduct());
                sortData[q] = (long) dotProductComponent << 32 | q;
            }

            Arrays.sort(sortData);

            for (int i = 0; i < quads.length; i++) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, (int) sortData[i]);
            }
        }

        return new StaticNormalRelativeData(sectionPos, buffer, translucentMesh.getVertexRanges());
    }

    /**
     * Important: The vertex indexes must start at zero for each facing.
     */
    private static StaticNormalRelativeData fromMixed(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, SectionPos sectionPos) {
        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        IntBuffer indexBuffer = buffer.getDirectBuffer().asIntBuffer();

        var ranges = translucentMesh.getVertexRanges();
        var maxQuadCount = 0;
        boolean anyNeedsSortData = false;
        for (var range : ranges) {
            if (range != null) {
                var quadCount = TranslucentData.vertexCountToQuadCount(range.vertexCount());
                maxQuadCount = Math.max(maxQuadCount, quadCount);
                anyNeedsSortData |= !RadixSort.useRadixSort(quadCount) && quadCount > 1;
            }
        }

        long[] sortData = null;
        if (anyNeedsSortData) {
            sortData = new long[maxQuadCount];
        }

        int quadIndex = 0;
        for (var range : ranges) {
            if (range == null) {
                continue;
            }

            int vertexCount = range.vertexCount();
            if (vertexCount == 0) {
                continue;
            }

            int count = TranslucentData.vertexCountToQuadCount(vertexCount);

            if (count == 1) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
                quadIndex++;
            } else if (RadixSort.useRadixSort(count)) {
                final var keys = new int[count];

                for (int q = 0; q < count; q++) {
                    keys[q] = MathUtil.floatToComparableInt(quads[quadIndex++].getDotProduct());
                }

                var indices = RadixSort.sort(keys);

                for (int i = 0; i < count; i++) {
                    TranslucentData.writeQuadVertexIndexes(indexBuffer, indices[i]);
                }
            } else {
                for (int i = 0; i < count; i++) {
                    var quad = quads[quadIndex++];
                    int dotProductComponent = MathUtil.floatToComparableInt(quad.getDotProduct());
                    sortData[i] = (long) dotProductComponent << 32 | i;
                }

                if (count > 1) {
                    Arrays.sort(sortData, 0, count);
                }

                for (int i = 0; i < count; i++) {
                    TranslucentData.writeQuadVertexIndexes(indexBuffer, (int) sortData[i]);
                }
            }
        }

        return new StaticNormalRelativeData(sectionPos, buffer, ranges);
    }

    public static StaticNormalRelativeData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, SectionPos sectionPos, boolean isDoubleUnaligned) {
        if (isDoubleUnaligned) {
            return fromDoubleUnaligned(translucentMesh, quads, sectionPos);
        } else {
            return fromMixed(translucentMesh, quads, sectionPos);
        }
    }
}
