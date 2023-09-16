package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.blaze3d.systems.VertexSorter;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.GroupBuilder.Quad;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;
import net.minecraft.util.math.ChunkSectionPos;

public class StaticNormalRelativeData extends PresentTranslucentData {
    public final VertexRange[] ranges;

    public StaticNormalRelativeData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos, buffer);
        this.ranges = ranges;
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_NORMAL_RELATIVE;
    }

    /**
     * The vertex sorter for each direction.
     * TODO: is there a better place to put this
     */
    private static final VertexSorter[] SORTERS = new VertexSorter[ModelQuadFacing.DIRECTIONS];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            SORTERS[i] = VertexSorters.sortByAxis(ModelQuadFacing.VALUES[i]);
        }
    }

    static StaticNormalRelativeData fromMesh(BuiltSectionMeshParts translucentMesh,
            List<Quad> quads, ChunkSectionPos sectionPos) {
        int vertexCount = 0;
        VertexRange[] ranges = translucentMesh.getVertexRanges();
        Vector3f[][] centers = new Vector3f[ModelQuadFacing.COUNT][];
        int[] centerCounters = new int[ModelQuadFacing.COUNT];

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            VertexRange range = ranges[i];
            if (range != null) {
                vertexCount += range.vertexCount();
                centers[i] = new Vector3f[range.vertexCount() / TranslucentData.VERTICES_PER_QUAD];
            }
        }

        for (Quad quad : quads) {
            var direction = quad.facing().ordinal();
            centers[direction][centerCounters[direction]++] = quad.center();
        }

        var buffer = new NativeBuffer(TranslucentData.vertexCountToIndexBytes(vertexCount));
        IntBuffer bufferBuilder = buffer.getDirectBuffer().asIntBuffer();

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            if (ranges[i] != null) {
                // TODO: there's no sorter for unaligned quads in normal relative mode
                TranslucentData.writeVertexIndexes(bufferBuilder, SORTERS[i].sort(centers[i]));
            }
        }

        return new StaticNormalRelativeData(sectionPos, buffer, ranges);
    }
}
