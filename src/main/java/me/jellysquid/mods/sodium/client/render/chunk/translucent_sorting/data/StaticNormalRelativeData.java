package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import java.util.Arrays;

import com.mojang.blaze3d.systems.VertexSorter;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;
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

    private static final VertexSorter[] SORTERS = new VertexSorter[ModelQuadFacing.DIRECTIONS];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            SORTERS[i] = VertexSorters.sortByAxis(ModelQuadFacing.VALUES[i]);
        }
    }

    private static StaticNormalRelativeData fromDoubleUnaligned(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos) {
        long[] sortData = new long[quads.length];

        for (int i = 0; i < quads.length; i++) {
            sortData[i] = (long) quads[i].getDotProduct() << 32 | i;
        }

        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        IntBuffer bufferBuilder = buffer.getDirectBuffer().asIntBuffer();

        Arrays.sort(sortData);

        for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(bufferBuilder, (int) sortData[i]);
        }

        return new StaticNormalRelativeData(sectionPos, buffer, translucentMesh.getVertexRanges());
    }

    private static StaticNormalRelativeData fromMixed(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos) {
        var ranges = translucentMesh.getVertexRanges();
        long[] sortData = new long[quads.length];

        for (int quadIndex = 0; quadIndex < quads.length; quadIndex++) {
            var quad = quads[quadIndex];
            sortData[quadIndex] = (long) quad.getDotProduct() << 32 | quadIndex;
        }

        var buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        IntBuffer bufferBuilder = buffer.getDirectBuffer().asIntBuffer();

        for (var range : ranges) {
            if (range != null) {
                int start = TranslucentData.vertexCountToQuadCount(range.vertexStart());
                int count = TranslucentData.vertexCountToQuadCount(range.vertexCount());
                if (count > 1) {
                    Arrays.sort(sortData, start, start + count);
                }
            }
        }

        for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(bufferBuilder, (int) sortData[i]);
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
