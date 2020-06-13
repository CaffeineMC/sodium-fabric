package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.multidraw.ChunkMultiDrawBatch;

public class ChunkRegion<T extends ChunkGraphicsState> {
    private static final int EXPECTED_CHUNK_SIZE = 8 * 1024;

    private final GlBufferArena arena;
    private final GlVertexArray vao;
    private final ChunkMultiDrawBatch batch;

    private final ObjectArrayList<ChunkBuildResult<T>> uploads;

    private GlBuffer prevBuffer;

    public ChunkRegion(int size) {
        int arenaSize = EXPECTED_CHUNK_SIZE * size;
        this.arena = new GlBufferArena(arenaSize, arenaSize);
        this.batch = new ChunkMultiDrawBatch(size);
        this.uploads = new ObjectArrayList<>();
        this.vao = new GlVertexArray();
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
    }

    public ObjectArrayList<ChunkBuildResult<T>> getUploadQueue() {
        return this.uploads;
    }

    public ChunkMultiDrawBatch getDrawBatch() {
        return this.batch;
    }

    public GlVertexArray getVertexArray() {
        return this.vao;
    }

    public void setPrevVbo(GlBuffer buffer) {
        this.prevBuffer = buffer;
    }

    public GlBuffer getPrevVbo() {
        return this.prevBuffer;
    }
}
