package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ChunkStatusTracker implements ChunkStatusListener {
    private static final Direction[] NEIGHBORS = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    private final int[] status;
    private final int diameter;

    public ChunkStatusTracker(int renderDistance) {
        this.diameter = (renderDistance * 2) + 1;
        this.status = new int[this.diameter * this.diameter];
    }

    public int getIndex(int chunkX, int chunkZ) {
        return Math.floorMod(chunkZ, this.diameter) * this.diameter + Math.floorMod(chunkX, this.diameter);
    }

    public boolean areChunksAvailable(int idx) {
        return this.status[idx] == 4;
    }

    @Override
    public void onChunkAdded(int x, int z) {
        for (Direction dir : NEIGHBORS) {
            this.status[this.getIndex(x + dir.getOffsetX(), z + dir.getOffsetZ())]++;
        }
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        for (Direction dir : NEIGHBORS) {
            this.status[this.getIndex(x + dir.getOffsetX(), z + dir.getOffsetZ())]--;
        }
    }
}
