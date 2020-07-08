package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkDrawCallBatcher;

public class ChunkRegion<T extends ChunkGraphicsState> {
    private static final int EXPECTED_CHUNK_SIZE = 4 * 1024;

    private final GlBufferArena arena;
    private final GlVertexArray vao;
    private final ChunkDrawCallBatcher batch;

    private final ObjectArrayList<ChunkBuildResult<T>> uploads;

    private GlBuffer prevBuffer;

    public ChunkRegion(MemoryTracker memoryTracker, int size) {
        int arenaSize = EXPECTED_CHUNK_SIZE * size;

        this.arena = new GlBufferArena(memoryTracker, arenaSize, arenaSize);
        this.uploads = new ObjectArrayList<>();
        this.vao = new GlVertexArray();

        this.batch = ChunkDrawCallBatcher.create(size * ModelQuadFacing.COUNT);
    }

    public GlBufferArena getBufferArena() {
        return this.arena;
    }

    public boolean isArenaEmpty() {
        return this.arena.isEmpty();
    }

    public void deleteResources() {
        this.arena.delete();
        this.vao.delete();
        this.batch.delete();
    }

    public ObjectArrayList<ChunkBuildResult<T>> getUploadQueue() {
        return this.uploads;
    }

    public ChunkDrawCallBatcher getDrawBatcher() {
        return this.batch;
    }

    public GlVertexArray getVertexArray() {
        return this.vao;
    }

    public boolean isDirty() {
        return this.prevBuffer != this.arena.getBuffer();
    }

    public void markClean() {
        this.prevBuffer = this.arena.getBuffer();
    }
}
