package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.PriorityQueue;
import java.util.Collection;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.StreamingBuffer;
import net.caffeinemc.sodium.render.buffer.arena.ArenaBuffer;
import net.caffeinemc.sodium.render.buffer.arena.AsyncArenaBuffer;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

public class RenderRegion {
    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;

    private static final int REGION_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    private static final int REGION_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    private static final int REGION_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    private static final int REGION_WIDTH_SH = Integer.bitCount(REGION_WIDTH_M);
    private static final int REGION_HEIGHT_SH = Integer.bitCount(REGION_HEIGHT_M);
    private static final int REGION_LENGTH_SH = Integer.bitCount(REGION_LENGTH_M);

    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    public final ArenaBuffer vertexBuffers;
    public final int id;
    
    public RenderRegion(AsyncArenaBuffer vertexBuffers, int id) {
        this.vertexBuffers = vertexBuffers;
        this.id = id;
    }

    public RenderRegion(RenderDevice device, StreamingBuffer stagingBuffer, TerrainVertexType vertexType, int id) {
        this.vertexBuffers = new AsyncArenaBuffer(device, stagingBuffer, REGION_SIZE * 756, vertexType.getBufferVertexFormat().stride());
        this.id = id;
    }
    
    // TODO: include index buffer cache too
    public void recycle(PriorityQueue<ArenaBuffer> vertexBufferCache) {
        ArenaBuffer buffer = this.vertexBuffers;
        buffer.reset();
        vertexBufferCache.enqueue(buffer);
    }

    public void delete() {
        this.vertexBuffers.delete();
    }

    public boolean isEmpty() {
        return this.vertexBuffers.isEmpty();
    }

    public long getDeviceUsedMemory() {
        return this.vertexBuffers.getDeviceUsedMemory();
    }

    public long getDeviceAllocatedMemory() {
        return this.vertexBuffers.getDeviceAllocatedMemory();
    }

    public static long getRegionCoord(int chunkX, int chunkY, int chunkZ) {
        return ChunkSectionPos.asLong(chunkX >> REGION_WIDTH_SH, chunkY >> REGION_HEIGHT_SH, chunkZ >> REGION_LENGTH_SH);
    }
}
