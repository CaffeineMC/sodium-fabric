package me.jellysquid.mods.sodium.client.render.chunk.region;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.AsyncBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.SwapBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

import java.util.EnumMap;
import java.util.Map;

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

    private final int x, y, z;

    private final Map<BlockRenderPass, RegionData> data = new EnumMap<>(BlockRenderPass.class);

    private final GlBufferArena vertexBuffers;
    private final GlBufferArena indexBuffers;

    private GlTessellation tessellation;

    public RenderRegion(int x, int y, int z,
                        CommandList commandList, StagingBuffer stagingBuffer) {
        int expectedVertexCount = REGION_SIZE * 756;
        int expectedIndexCount = (expectedVertexCount / 4) * 6;

        this.vertexBuffers = createArena(commandList, expectedVertexCount * ChunkModelVertexFormats.DEFAULT.getBufferVertexFormat().getStride(), stagingBuffer);
        this.indexBuffers = createArena(commandList, expectedIndexCount * 4, stagingBuffer);

        this.x = x;
        this.y = y;
        this.z = z;

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            this.data.put(pass, new RegionData());
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

    public void delete(CommandList commandList) {
        this.deleteTessellations(commandList);

        this.vertexBuffers.delete(commandList);
        this.indexBuffers.delete(commandList);
    }

    public void deleteTessellations(CommandList commandList) {
        if (this.tessellation != null) {
            commandList.deleteTessellation(this.tessellation);
            this.tessellation = null;
        }
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

    private static GlBufferArena createArena(CommandList commandList, int initialCapacity, StagingBuffer stagingBuffer) {
        return switch (SodiumClientMod.options().advanced.arenaMemoryAllocator) {
            case ASYNC -> new AsyncBufferArena(commandList, initialCapacity, stagingBuffer);
            case SWAP -> new SwapBufferArena(commandList);
        };
    }

    public static int getChunkIndex(int x, int y, int z) {
        return (x * RenderRegion.REGION_LENGTH * RenderRegion.REGION_HEIGHT) + (y * RenderRegion.REGION_LENGTH) + z;
    }

    public RegionData getData(BlockRenderPass pass) {
        return this.data.get(pass);
    }

    public GlTessellation getTessellation() {
        return this.tessellation;
    }

    public void setTessellation(GlTessellation tessellation) {
        this.tessellation = tessellation;
    }

    public GlBufferArena getVertexBuffer() {
        return this.vertexBuffers;
    }

    public GlBufferArena getIndexBuffer() {
        return this.indexBuffers;
    }

    public void deleteChunk(int localId) {
        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            this.getData(pass).deleteGraphicsState(localId);
        }
    }

    public static class RegionData {
        public final ChunkGraphicsState[] state;

        public RegionData() {
            this.state = new ChunkGraphicsState[RenderRegion.REGION_SIZE];
        }

        public ChunkGraphicsState getGraphicsState(int sectionId) {
            return this.state[sectionId];
        }

        public void setGraphicsState(int localId, ChunkGraphicsState state) {
            this.state[localId] = state;
        }

        public void deleteGraphicsState(int localId) {
            var state = this.state[localId];

            if (state != null) {
                state.delete();
            }

            this.state[localId] = null;
        }
    }
}
