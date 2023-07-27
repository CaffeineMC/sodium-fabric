package me.jellysquid.mods.sodium.client.render.chunk.data;

import java.nio.ByteBuffer;

import org.joml.Vector3f;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;

public class IndexedPrimitives {
    public final Vector3f[] centers;
    public int[] indexes;

    public IndexedPrimitives(
            ChunkVertexType vertexType,
            ByteBuffer buffer,
            VertexRange vertexRange,
            int primitiveCount) {
        this.centers = new Vector3f[primitiveCount];
        this.indexes = new int[primitiveCount];
        if (!(vertexType instanceof CompactChunkVertex)) {
            throw new UnsupportedOperationException("Unknown vertex type: " + vertexType);
        }
        int vertexStride = vertexType.getVertexFormat().getStride();
        int primitiveStride = vertexStride * 4; // TODO: is it quads or triangles?
        for (int i = 0; i < primitiveCount; i ++) {
            // calculate the primitive center
            int offset = i * primitiveStride + vertexRange.vertexStart();
            short x = buffer.getShort(offset + 0);
            short y = buffer.getShort(offset + 2);
            short z = buffer.getShort(offset + 4);

            offset += vertexStride;
            x += buffer.getShort(offset + 0);
            y += buffer.getShort(offset + 2);
            z += buffer.getShort(offset + 4);

            offset += vertexStride;
            x += buffer.getShort(offset + 0);
            y += buffer.getShort(offset + 2);
            z += buffer.getShort(offset + 4);

            offset += vertexStride;
            x += buffer.getShort(offset + 0);
            y += buffer.getShort(offset + 2);
            z += buffer.getShort(offset + 4);

            float xCenter = CompactChunkVertex.decodePositionSum(x, 4);
            float yCenter = CompactChunkVertex.decodePositionSum(y, 4);
            float zCenter = CompactChunkVertex.decodePositionSum(z, 4);

            this.centers[i] = new Vector3f(xCenter, yCenter, zCenter);
            this.indexes[i] = i;
        }
    }
}
