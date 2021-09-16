package me.jellysquid.mods.sodium.render.chunk.graph;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongPriorityQueue;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class ChunkIterationQueue {
    private static final int RENDER_OFFSET = 32;
    private static final int DIRECTION_OFFSET = 0;

    private static final long MASK = 0xFFFFFFFFL;

    private final LongPriorityQueue queue = new LongArrayFIFOQueue();

    public static Direction getDirection(long next) {
        return idToDirection((int) ((next >> DIRECTION_OFFSET) & MASK));
    }

    public static int getRender(long next) {
        return (int) (next >>> RENDER_OFFSET);
    }

    public void add(int render, Direction direction) {
        this.queue.enqueue(pack(render, direction));
    }

    private static long pack(int render, Direction dir) {
        return ((render & MASK) << RENDER_OFFSET) | ((directionToId(dir) & MASK) << DIRECTION_OFFSET);
    }

    private static int directionToId(Direction dir) {
        return (dir == null ? -1 : dir.ordinal());
    }

    private static Direction idToDirection(int id) {
        return id == -1 ? null : DirectionUtil.ALL_DIRECTIONS[id];
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public long dequeueNext() {
        return this.queue.dequeueLong();
    }
}
