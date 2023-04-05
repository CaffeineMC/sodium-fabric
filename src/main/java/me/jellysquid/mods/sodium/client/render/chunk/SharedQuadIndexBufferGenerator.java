package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumMap;

public class SharedQuadIndexBufferGenerator {
    private static EnumMap<SharedQuadIndexBuffer.IndexType, NativeBuffer> buffers = new EnumMap<>(SharedQuadIndexBuffer.IndexType.class);
    public static NativeBuffer getBuffer(int vertexCount) {
        if (vertexCount > SharedQuadIndexBuffer.IndexType.SHORT.getMaxVertexCount()) {
            throw new IllegalStateException();
        }
        return null;
    }

    public static void fillBuffer(long buffer, int vertexCount) {
        for (int primitiveIndex = 0; primitiveIndex < (vertexCount>>2); primitiveIndex++) {
            int indexOffset = primitiveIndex * 6;
            int vertexOffset = primitiveIndex * 4;

            MemoryUtil.memPutShort(buffer + (indexOffset + 0)*2, (short) (vertexOffset + 0));
            MemoryUtil.memPutShort(buffer + (indexOffset + 1)*2, (short) (vertexOffset + 1));
            MemoryUtil.memPutShort(buffer + (indexOffset + 2)*2, (short) (vertexOffset + 2));

            MemoryUtil.memPutShort(buffer + (indexOffset + 3)*2, (short) (vertexOffset + 2));
            MemoryUtil.memPutShort(buffer + (indexOffset + 4)*2, (short) (vertexOffset + 3));
            MemoryUtil.memPutShort(buffer + (indexOffset + 5)*2, (short) (vertexOffset + 0));
        }
        //System.out.println("a");
    }
}
