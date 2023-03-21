package me.jellysquid.mods.sodium.client.render.chunk.graph;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public class SearchQueueAccessUnsafe {
    private static final int OFFSET_SELF = 0;
    private static final int OFFSET_DOWN = (1 * Pointer.POINTER_SIZE);
    private static final int OFFSET_UP = (2 * Pointer.POINTER_SIZE);
    private static final int OFFSET_NORTH = (3 * Pointer.POINTER_SIZE);
    private static final int OFFSET_SOUTH = (4 * Pointer.POINTER_SIZE);
    private static final int OFFSET_WEST = (5 * Pointer.POINTER_SIZE);
    private static final int OFFSET_EAST = (6 * Pointer.POINTER_SIZE);

    private static final int STRUCT_SIZE = 7 * Pointer.POINTER_SIZE;

    public static long allocateStack(MemoryStack stack) {
        return stack.ncalloc(8, 1, STRUCT_SIZE);
    }

    public static void setSelf(long ptr, long value) {
        MemoryUtil.memPutAddress(ptr + OFFSET_SELF, value);
    }

    public static void setAdjacent(long ptr, int dir, long value) {
        MemoryUtil.memPutAddress(ptr + getAdjacentOffset(dir), value);
    }

    public static long down(long ptr, boolean neighbor) {
        return MemoryUtil.memGetAddress(ptr + (neighbor ? OFFSET_DOWN : 0));
    }

    public static long up(long ptr, boolean neighbor) {
        return MemoryUtil.memGetAddress(ptr + (neighbor ? OFFSET_UP : 0));
    }

    public static long north(long ptr, boolean neighbor) {
        return MemoryUtil.memGetAddress(ptr + (neighbor ? OFFSET_NORTH : 0));
    }

    public static long south(long ptr, boolean neighbor) {
        return MemoryUtil.memGetAddress(ptr + (neighbor ? OFFSET_SOUTH : 0));
    }

    public static long west(long ptr, boolean neighbor) {
        return MemoryUtil.memGetAddress(ptr + (neighbor ? OFFSET_WEST : 0));
    }

    public static long east(long ptr, boolean neighbor) {
        return MemoryUtil.memGetAddress(ptr + (neighbor ? OFFSET_EAST : 0));
    }

    private static long getAdjacentOffset(int dir) {
        return switch (dir) {
            case GraphDirection.DOWN -> OFFSET_DOWN;
            case GraphDirection.UP -> OFFSET_UP;
            case GraphDirection.NORTH -> OFFSET_NORTH;
            case GraphDirection.SOUTH -> OFFSET_SOUTH;
            case GraphDirection.WEST -> OFFSET_WEST;
            case GraphDirection.EAST -> OFFSET_EAST;
            default -> throw new IllegalStateException("Unexpected value: " + dir);
        };
    }
}
