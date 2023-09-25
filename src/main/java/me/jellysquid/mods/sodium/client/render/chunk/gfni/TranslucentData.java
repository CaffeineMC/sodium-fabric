package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class TranslucentData {
    public static final int INDICES_PER_QUAD = 6;
    public static final int VERTICES_PER_QUAD = 4;
    public static final int BYTES_PER_INDEX = 4;

    public final ChunkSectionPos sectionPos;

    TranslucentData(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    public abstract SortType getSortType();

    public void delete() {
    }

    public void sort(Vector3fc cameraPos) {
        // there should be no sort calls to data that doesn't need sorting
        throw new UnsupportedOperationException();
    }

    static int vertexCountToIndexBytes(int vertexCount) {
        // convert vertex count to quads, and then to indices, and then to bytes
        return vertexCount / VERTICES_PER_QUAD * INDICES_PER_QUAD * BYTES_PER_INDEX;
    }

    static int quadCountToIndexBytes(int quadCount) {
        return quadCount * INDICES_PER_QUAD * BYTES_PER_INDEX;
    }

    static void putMappedQuadVertexIndexes(IntBuffer intBuffer, int quadIndex, int[] indexMapping) {
        if (indexMapping == null) {
            putQuadVertexIndexes(intBuffer, quadIndex);
        } else {
            putQuadVertexIndexes(intBuffer, indexMapping[quadIndex]);
        }
    }

    static void putQuadVertexIndexes(IntBuffer intBuffer, int quadIndex) {
        int vertexOffset = quadIndex * VERTICES_PER_QUAD;

        intBuffer.put(vertexOffset + 0);
        intBuffer.put(vertexOffset + 1);
        intBuffer.put(vertexOffset + 2);

        intBuffer.put(vertexOffset + 2);
        intBuffer.put(vertexOffset + 3);
        intBuffer.put(vertexOffset + 0);
    }

    static void writeVertexIndexes(IntBuffer intBuffer, int[] quadIndexes) {
        for (int quadIndexPos = 0; quadIndexPos < quadIndexes.length; quadIndexPos++) {
            putQuadVertexIndexes(intBuffer, quadIndexes[quadIndexPos]);
        }
    }

    static void writeVertexIndexesOffset(IntBuffer intBuffer, int[] quadIndexes, int offset) {
        for (int quadIndexPos = 0; quadIndexPos < quadIndexes.length; quadIndexPos++) {
            putQuadVertexIndexes(intBuffer, quadIndexes[quadIndexPos] + offset);
        }
    }

    static VertexRange getUnassignedVertexRange(BuiltSectionMeshParts translucentMesh) {
        VertexRange range = translucentMesh.getVertexRanges()[ModelQuadFacing.UNASSIGNED.ordinal()];

        if (range == null) {
            throw new IllegalStateException("No unassigned data in mesh");
        }

        return range;
    }
}
