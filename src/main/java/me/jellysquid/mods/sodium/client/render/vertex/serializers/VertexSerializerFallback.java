package me.jellysquid.mods.sodium.client.render.vertex.serializers;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

// This is very slow. Maybe there is some way to improve it when we can't rely on generating an implementation.
final class VertexSerializerFallback implements VertexSerializer {
    private final int[] srcOffsets;
    private final int[] dstOffsets;

    private final int[] lengths;

    private final int elementCount;

    private final int srcStride, dstStride;

    public VertexSerializerFallback(List<MemoryTransfer> ops, VertexFormatDescription srcFormat, VertexFormatDescription dstFormat) {
        this.srcStride = srcFormat.stride;
        this.dstStride = dstFormat.stride;

        this.elementCount = ops.size();

        this.srcOffsets = new int[this.elementCount];
        this.dstOffsets = new int[this.elementCount];

        this.lengths = new int[this.elementCount];

        for (int i = 0; i < this.elementCount; i++) {
            MemoryTransfer op = ops.get(i);

            this.srcOffsets[i] = op.src();
            this.dstOffsets[i] = op.dst();

            this.lengths[i] = op.length();
        }
    }

    @Override
    public void serialize(long src, long dst, int vertexCount) {
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            for (int elementIndex = 0; elementIndex < this.elementCount; elementIndex++) {
                MemoryUtil.memCopy(src + this.srcOffsets[elementIndex], dst + this.dstOffsets[elementIndex], this.lengths[elementIndex] * 4);
            }

            src += this.srcStride;
            dst += this.dstStride;
        }
    }

}
