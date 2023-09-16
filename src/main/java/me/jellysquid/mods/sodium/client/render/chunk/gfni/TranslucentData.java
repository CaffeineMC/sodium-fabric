package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;

import org.joml.Vector3f;

import net.minecraft.util.math.ChunkSectionPos;

public abstract class TranslucentData {
    public static final int INDICES_PER_QUAD = 6;
    public static final int VERTICES_PER_QUAD = 4;
    public static final int BYTES_PER_INDEX = 4;

    public final ChunkSectionPos sectionPos;

    public TranslucentData(ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    public abstract SortType getSortType();

    public void sort(Vector3f cameraPos) {
        // there should be no sort calls to data that doesn't need sorting
        throw new UnsupportedOperationException();
    }

    public static int vertexCountToIndexBytes(int vertexCount) {
        // convert vertex count to quads, and then to indices, and then to bytes
        return vertexCount / VERTICES_PER_QUAD * INDICES_PER_QUAD * BYTES_PER_INDEX;
    }

    public static void putQuadVertexIndexes(IntBuffer intBuffer, int quadIndex) {
        int vertexOffset = quadIndex * VERTICES_PER_QUAD;

        intBuffer.put(vertexOffset + 0);
        intBuffer.put(vertexOffset + 1);
        intBuffer.put(vertexOffset + 2);

        intBuffer.put(vertexOffset + 2);
        intBuffer.put(vertexOffset + 3);
        intBuffer.put(vertexOffset + 0);
    }

    public static void writeVertexIndexes(IntBuffer intBuffer, int[] quadIndexes) {
        for (int quadIndexPos = 0; quadIndexPos < quadIndexes.length; quadIndexPos++) {
            putQuadVertexIndexes(intBuffer, quadIndexes[quadIndexPos]);
        }
    }
}
