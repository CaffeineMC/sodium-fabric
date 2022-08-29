package net.caffeinemc.sodium.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Set;
import net.caffeinemc.gfx.api.buffer.ImmutableBuffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.buffer.BufferPool;
import net.caffeinemc.gfx.util.buffer.streaming.StreamingBuffer;
import net.caffeinemc.sodium.render.buffer.arena.ArenaBuffer;
import net.caffeinemc.sodium.render.buffer.arena.AsyncArenaBuffer;
import net.caffeinemc.sodium.render.buffer.arena.PendingUpload;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.gfx.util.misc.MathUtil;
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
    
    private static final int INITIAL_VERTICES = REGION_SIZE * 756; // 756 is the average-ish amount of vertices in a section
    private static final int INITIAL_INDICES = (INITIAL_VERTICES >> 2) * 6;
    private static final float RESIZE_FACTOR = .25f; // add 25% each resize
    
    private final Set<RenderSection> sections = new ObjectOpenHashSet<>(REGION_SIZE);
    
    private final ArenaBuffer vertexBuffer;
    private final int id;

    public RenderRegion(RenderDevice device, StreamingBuffer stagingBuffer, BufferPool<ImmutableBuffer> vertexBufferPool, TerrainVertexType vertexType, int id) {
        this.vertexBuffer = new AsyncArenaBuffer(
                device,
                stagingBuffer,
                vertexBufferPool,
                INITIAL_VERTICES, // 756 is the average-ish amount of vertices in a section
                RESIZE_FACTOR, // add 25% each resize
                vertexType.getBufferVertexFormat().stride()
        );
        this.id = id;
    }
    
    /**
     * Uploads the given pending uploads to the buffers, adding sections to this region as necessary.
     */
    public void submitUploads(List<PendingUpload> pendingUploads, int frameIndex) {
        this.vertexBuffer.upload(pendingUploads, frameIndex);
        
        // Collect the upload results
        for (PendingUpload pendingUpload : pendingUploads) {
            long bufferSegment = pendingUpload.bufferSegmentResult.get();
            RenderSection section = pendingUpload.section;
    
            section.setGeometry(this, bufferSegment);
            this.sections.add(section);
        }
    }
    
    /**
     * Removes the given section from the region, and frees the vertex buffer segment associated with the section.
     */
    public void removeSection(RenderSection section) {
        this.vertexBuffer.free(section.getUploadedGeometrySegment());
        this.sections.remove(section);
    }

    public void delete() {
        this.vertexBuffer.delete();
    }

    public boolean isEmpty() {
        return this.vertexBuffer.isEmpty();
    }

    public long getDeviceUsedMemory() {
        return this.vertexBuffer.getDeviceUsedMemory();
    }

    public long getDeviceAllocatedMemory() {
        return this.vertexBuffer.getDeviceAllocatedMemory();
    }

    public static long getRegionCoord(int chunkX, int chunkY, int chunkZ) {
        return ChunkSectionPos.asLong(chunkX >> REGION_WIDTH_SH, chunkY >> REGION_HEIGHT_SH, chunkZ >> REGION_LENGTH_SH);
    }
    
    public Set<RenderSection> getSections() {
        return this.sections;
    }
    
    public ArenaBuffer getVertexBuffer() {
        return this.vertexBuffer;
    }
    
    public int getId() {
        return this.id;
    }
}
