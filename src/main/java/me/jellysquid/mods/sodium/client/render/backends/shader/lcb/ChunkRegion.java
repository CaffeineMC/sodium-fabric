package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.util.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRegion<T extends ChunkGraphicsState> {
    private final ChunkSectionPos origin;

    private final GlBufferArena arena;
    private final GlVertexArray vao;
    private final MultiDrawBatch batch;

    private final ObjectArrayList<ChunkBuildResult<T>> uploads;

    private GlBuffer prevBuffer;

    public ChunkRegion(ChunkSectionPos origin, int size) {
        this.origin = origin;
        this.arena = new GlBufferArena(768 * 1024, 512 * 1024);
        this.batch = new MultiDrawBatch(size);
        this.uploads = new ObjectArrayList<>();
        this.vao = new GlVertexArray();
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
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

    public MultiDrawBatch getDrawBatch() {
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
