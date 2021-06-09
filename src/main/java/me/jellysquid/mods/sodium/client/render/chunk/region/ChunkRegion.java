package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.ChunkDrawCallBatcher;

public class ChunkRegion<T extends ChunkGraphicsState> {
    private final GlBufferArena vertexBuffers;
    private final GlBufferArena indexBuffers;

    private final ChunkDrawCallBatcher batch;
    private final RenderDevice device;

    private final ObjectArrayList<ChunkBuildResult<T>> uploadQueue;

    private GlTessellation tessellation;

    public ChunkRegion(RenderDevice device, int size) {
        this.device = device;
        this.uploadQueue = new ObjectArrayList<>();

        this.vertexBuffers = new GlBufferArena(device, 4096);
        this.indexBuffers = new GlBufferArena(device, 128);

        this.batch = ChunkDrawCallBatcher.create(size * ModelQuadFacing.COUNT);
    }

    public GlBufferArena getVertexBufferArena() {
        return this.vertexBuffers;
    }

    public GlBufferArena getIndexBufferArena() {
        return this.indexBuffers;
    }

    public boolean isArenaEmpty() {
        // TODO: move counters into ChunkRegion?
        return this.vertexBuffers.isEmpty();
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

        this.batch.delete();
    }

    public ObjectArrayList<ChunkBuildResult<T>> getUploadQueue() {
        return this.uploadQueue;
    }

    public ChunkDrawCallBatcher getDrawBatcher() {
        return this.batch;
    }

    public GlTessellation getTessellation() {
        return this.tessellation;
    }

    public void setTessellation(GlTessellation tessellation) {
        this.tessellation = tessellation;
    }
}
