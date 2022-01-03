package me.jellysquid.mods.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.interop.vanilla.math.frustum.Frustum;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.render.arena.AsyncBufferArena;
import me.jellysquid.mods.sodium.render.arena.GlBufferArena;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.stream.StreamingBuffer;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexFormats;
import me.jellysquid.mods.sodium.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.Set;

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

    private static final int REGION_EXCESS = 8;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final RenderRegionManager manager;

    private final Set<RenderSection> chunks = new ObjectOpenHashSet<>();
    private RenderRegionArenas arenas;

    private final int x, y, z;

    private Frustum.Visibility visibility;

    public RenderRegion(RenderRegionManager manager, int x, int y, int z) {
        this.manager = manager;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static RenderRegion createRegionForChunk(RenderRegionManager manager, int x, int y, int z) {
        return new RenderRegion(manager, x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public RenderRegionArenas getArenas() {
        return this.arenas;
    }

    public void deleteResources() {
        if (this.arenas != null) {
            this.arenas.delete();
            this.arenas = null;
        }
    }

    public static long getRegionKeyForChunk(int x, int y, int z) {
        return ChunkSectionPos.asLong(x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public int getOriginX() {
        return this.x << REGION_WIDTH_SH << 4;
    }

    public int getOriginY() {
        return this.y << REGION_HEIGHT_SH << 4;
    }

    public int getOriginZ() {
        return this.z << REGION_LENGTH_SH << 4;
    }

    public RenderRegionArenas getOrCreateArenas() {
        RenderRegionArenas arenas = this.arenas;

        if (arenas == null) {
            this.arenas = (arenas = this.manager.createRegionArenas());
        }

        return arenas;
    }

    public void addChunk(RenderSection chunk) {
        if (!this.chunks.add(chunk)) {
            throw new IllegalStateException("Chunk " + chunk + " is already a member of region " + this);
        }
    }

    public void removeChunk(RenderSection chunk) {
        if (!this.chunks.remove(chunk)) {
            throw new IllegalStateException("Chunk " + chunk + " is not a member of region " + this);
        }
    }

    public boolean isEmpty() {
        return this.chunks.isEmpty();
    }

    public int getChunkCount() {
        return this.chunks.size();
    }

    public void updateVisibility(Frustum frustum) {
        int x = this.getOriginX();
        int y = this.getOriginY();
        int z = this.getOriginZ();

        // HACK: Regions need to be slightly larger than their real volume
        // Otherwise, the first node in the iteration graph might be incorrectly culled when the camera
        // is at the extreme end of a region
        this.visibility = frustum.testBox(x - REGION_EXCESS, y - REGION_EXCESS, z - REGION_EXCESS,
                x + (REGION_WIDTH << 4) + REGION_EXCESS, y + (REGION_HEIGHT << 4) + REGION_EXCESS, z + (REGION_LENGTH << 4) + REGION_EXCESS);
    }

    public Frustum.Visibility getVisibility() {
        return this.visibility;
    }

    public static int getChunkIndex(int x, int y, int z) {
        return (x * RenderRegion.REGION_LENGTH * RenderRegion.REGION_HEIGHT) + (y * RenderRegion.REGION_LENGTH) + z;
    }

    public static class RenderRegionArenas {
        public final GlBufferArena vertexBuffers;
        public final GlBufferArena indexBuffers;

        public RenderRegionArenas(RenderDevice device, StreamingBuffer stagingBuffer) {
            int expectedVertexCount = REGION_SIZE * 756;
            int expectedIndexCount = (expectedVertexCount / 4) * 6;

            this.vertexBuffers = createArena(device, expectedVertexCount, TerrainVertexFormats.STANDARD.getBufferVertexFormat().getStride(), stagingBuffer);
            this.indexBuffers = createArena(device, expectedIndexCount, 4, stagingBuffer);
        }

        public void delete() {
            this.vertexBuffers.delete();
            this.indexBuffers.delete();
        }

        public boolean isEmpty() {
            return this.vertexBuffers.isEmpty() && this.indexBuffers.isEmpty();
        }

        public long getDeviceUsedMemory() {
            return this.vertexBuffers.getDeviceUsedMemory() + this.indexBuffers.getDeviceUsedMemory();
        }

        public long getDeviceAllocatedMemory() {
            return this.vertexBuffers.getDeviceAllocatedMemory() + this.indexBuffers.getDeviceAllocatedMemory();
        }

        private static GlBufferArena createArena(RenderDevice device, int initialCapacity, int stride, StreamingBuffer stagingBuffer) {
            return new AsyncBufferArena(device, initialCapacity * stride, stride, stagingBuffer);
        }
    }
}
