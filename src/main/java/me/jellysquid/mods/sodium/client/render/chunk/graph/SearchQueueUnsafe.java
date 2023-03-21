package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.util.memory.HeapArena;

import static org.lwjgl.system.MemoryUtil.*;

public class SearchQueueUnsafe {
    private static final long OFFSET_INT_SIZE;
    private static final long OFFSET_ARRAY_INCOMING;
    private static final long OFFSET_ARRAY_QUEUE;
    private static final int STRUCT_SIZE;

    static {
        long offset = 0;

        OFFSET_INT_SIZE = offset;
        offset += 16;

        OFFSET_ARRAY_INCOMING = offset;
        offset += 256;

        OFFSET_ARRAY_QUEUE = offset;
        offset += 256 + 16;

        STRUCT_SIZE = (int) offset;
    }
    
    public static long allocate(HeapArena arena) {
        return arena.alloc(16, STRUCT_SIZE);
    }

    public static void enqueueConditionally(long pQueue, int localSectionIndex, int direction, int condition) {
        final int incomingData = memGetArrayByte(pQueue + OFFSET_ARRAY_INCOMING, localSectionIndex); // this.queue[localSectionIndex]
        memPutArrayByte(pQueue + OFFSET_ARRAY_INCOMING, localSectionIndex, (incomingData | (condition << direction))); // this.queue[localSectionIndex] |= (condition << direction)

        final int queueSizeIncrement = (incomingData == 0 ? 1 : 0) & condition; // if (incomingData == 0 && condition != 0)

        memPutArrayByte(pQueue + OFFSET_ARRAY_QUEUE, memGetInt(pQueue + OFFSET_INT_SIZE), localSectionIndex); // this.queue[this.size] = localSectionIndex;
        memPutInt(pQueue + OFFSET_INT_SIZE, memGetInt(pQueue + OFFSET_INT_SIZE) + queueSizeIncrement); // this.size += queueSizeIncrement;
    }
    public static void enqueue(long pQueue, int x, int y, int z) {
        final int localSectionIndex = LocalSectionIndex.pack(x, y, z);

        final int incomingData = memGetArrayByte(pQueue + OFFSET_ARRAY_INCOMING, localSectionIndex);
        memPutArrayByte(pQueue + OFFSET_ARRAY_INCOMING, localSectionIndex, (incomingData | 0b111111));

        memPutArrayByte(pQueue + OFFSET_ARRAY_QUEUE, memGetInt(pQueue + OFFSET_INT_SIZE), localSectionIndex);
        memPutInt(pQueue + OFFSET_INT_SIZE, memGetInt(pQueue + OFFSET_INT_SIZE) + 1);
    }

    public static int getQueueSize(long pQueue) {
        return memGetInt(pQueue + OFFSET_INT_SIZE);
    }
    public static int getIncomingDirections(long pQueue, int sectionIndex) {
        return memGetArrayByte(pQueue + OFFSET_ARRAY_INCOMING, sectionIndex);
    }

    public static int getQueueEntry(long pQueue, int queueIndex) {
        return memGetArrayByte(pQueue + OFFSET_ARRAY_QUEUE, queueIndex);
    }

    private static int memGetArrayByte(long base, int offset) {
        return Byte.toUnsignedInt(memGetByte(base + Integer.toUnsignedLong(offset)));
    }

    private static void memPutArrayByte(long base, int offset, int value) {
        memPutByte(base + Integer.toUnsignedLong(offset), (byte) value);
    }

}
