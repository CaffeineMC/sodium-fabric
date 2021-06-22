package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
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

    private final RenderChunk[] renders = new RenderChunk[REGION_SIZE];
    private int loadedChunks = 0;

    private final Map<BlockRenderPass, RenderRegionArenas> arenas = new EnumMap<>(BlockRenderPass.class);

    private final RenderDevice device;
    private final int x, y, z;

    public RenderRegion(RenderDevice device, int x, int y, int z) {
        this.device = device;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static RenderRegion createRegionForChunk(RenderDevice device, int x, int y, int z) {
        return new RenderRegion(device, x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public RenderChunk getChunk(int x, int y, int z) {
        return this.renders[getRenderIndex(x, y, z)];
    }

    public void addRender(int x, int y, int z, RenderChunk render) {
        if (render == null) {
            throw new IllegalArgumentException("Render must not be null");
        }

        int idx = getRenderIndex(x, y, z);
        RenderChunk prev = this.renders[idx];

        if (prev != null) {
            throw new IllegalArgumentException("Chunk is already loaded at " + ChunkSectionPos.from(x, y, z));
        }

        this.renders[idx] = render;
        this.loadedChunks++;
    }

    public RenderChunk removeChunk(int x, int y, int z) {
        int idx = getRenderIndex(x, y, z);
        RenderChunk prev = this.renders[idx];

        if (prev == null) {
            throw new IllegalArgumentException("Chunk is not loaded at " + ChunkSectionPos.from(x, y, z));
        }

        this.renders[idx] = null;
        this.loadedChunks--;

        return prev;
    }

    private static int getRenderIndex(int x, int y, int z) {
        return (REGION_HEIGHT * REGION_WIDTH * (z & REGION_LENGTH_M)) + (REGION_WIDTH * (y & REGION_HEIGHT_M)) + (x & REGION_WIDTH_M);
    }

    public RenderRegionArenas getArenas(BlockRenderPass pass) {
        return this.arenas.get(pass);
    }

    public void deleteResources() {
        try (CommandList commandList = this.device.createCommandList()) {
            for (RenderRegionArenas arenas : this.arenas.values()) {
                arenas.delete(commandList);
            }
        }

        this.arenas.clear();
    }

    public int getChunkCount() {
        return this.loadedChunks;
    }

    public static long getRegionKey(int x, int y, int z) {
        return ChunkSectionPos.asLong(x >> REGION_WIDTH_SH, y >> REGION_HEIGHT_SH, z >> REGION_LENGTH_SH);
    }

    public Vec3i getRelativeOffset(int chunkX, int chunkY, int chunkZ) {
        return new Vec3i(chunkX & REGION_WIDTH_M, chunkY & REGION_HEIGHT_M, chunkZ & REGION_LENGTH_M);
    }

    public int getRenderX() {
        return ((this.x << REGION_WIDTH_SH) << 4) - 8;
    }

    public int getRenderY() {
        return ((this.y << REGION_HEIGHT_SH) << 4) - 8;
    }

    public int getRenderZ() {
        return ((this.z << REGION_LENGTH_SH) << 4) - 8;
    }

    public RenderRegionArenas createArenas(CommandList commandList, BlockRenderPass pass) {
        RenderRegionArenas arenas = new RenderRegionArenas(commandList);
        this.arenas.put(pass, arenas);

        return arenas;
    }

    public void deleteArenas(CommandList commandList, BlockRenderPass pass) {
        this.arenas.remove(pass)
                .delete(commandList);
    }

    public static class RenderRegionArenas {
        public final GlBufferArena vertexBuffers;
        public final GlBufferArena indexBuffers;

        public GlTessellation tessellation;

        public RenderRegionArenas(CommandList commandList) {
            this.vertexBuffers = new GlBufferArena(commandList, 768 * 1024);
            this.indexBuffers = new GlBufferArena(commandList, 64 * 1024);
        }

        public void delete(CommandList commandList) {
            this.vertexBuffers.delete(commandList);
            this.indexBuffers.delete(commandList);

            if (this.tessellation != null) {
                this.tessellation.delete(commandList);
            }
        }

        public void setTessellation(GlTessellation tessellation) {
            this.tessellation = tessellation;
        }

        public GlTessellation getTessellation() {
            return this.tessellation;
        }

        public boolean isEmpty() {
            return this.vertexBuffers.isEmpty() && this.indexBuffers.isEmpty();
        }
    }
}
