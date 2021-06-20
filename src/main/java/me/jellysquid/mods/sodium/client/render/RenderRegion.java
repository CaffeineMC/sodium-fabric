package me.jellysquid.mods.sodium.client.render;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.chunk.RenderChunk;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

public class RenderRegion {
    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;
    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
    }

    private final RenderChunk[] renders = new RenderChunk[REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH];
    private int loadedChunks = 0;

    private final GlBufferArena vertexBuffers;
    private final GlBufferArena indexBuffers;

    private final RenderDevice device;

    private GlTessellation tessellation;

    public RenderRegion(RenderDevice device) {
        this.device = device;

        this.vertexBuffers = new GlBufferArena(device, 4096);
        this.indexBuffers = new GlBufferArena(device, 128);
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
        return (REGION_HEIGHT * REGION_WIDTH * z) + (REGION_WIDTH * y) + x;
    }

    public GlBufferArena getVertexBufferArena() {
        return this.vertexBuffers;
    }

    public GlBufferArena getIndexBufferArena() {
        return this.indexBuffers;
    }

    public void deleteResources() {
        if (this.tessellation != null) {
            try (CommandList commands = this.device.createCommandList()) {
                commands.deleteTessellation(this.tessellation);
            }

            this.tessellation = null;
        }

        this.vertexBuffers.delete();
        this.indexBuffers.delete();
    }

    public GlTessellation getTessellation() {
        return this.tessellation;
    }

    public void setTessellation(GlTessellation tessellation) {
        this.tessellation = tessellation;
    }

    public int getChunkCount() {
        return this.loadedChunks;
    }
}
