package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;

public class ColumnRender<T extends ChunkRenderData> {
    @SuppressWarnings("unchecked")
    private final ChunkRender<T>[] chunks = new ChunkRender[16];
    private final World world;

    private final int x, z;
    private int count;
    private boolean chunkPresent;

    public ColumnRender(World world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;

        this.refreshChunkStatus();
    }

    public ChunkRender<T> getChunk(int y) {
        if (y < 0 || y >= 16) {
            return null;
        }

        return this.chunks[y];
    }

    public ChunkRender<T> getOrCreateChunk(int y, RenderFactory<T> factory) {
        if (y < 0 || y >= 16) {
            return null;
        }

        ChunkRender<T> chunk = this.chunks[y];

        if (chunk == null) {
            chunk = factory.create(this, this.x, y, this.z);

            this.chunks[y] = chunk;
            this.count++;
        }

        return chunk;
    }

    public void deleteData() {
        for (ChunkRender<T> render : this.chunks) {
            if (render != null) {
                render.deleteData();
            }
        }
    }

    public void refreshChunkStatus() {
        // ClientWorld#isChunkLoaded cannot be used as it will always return true
        // We also must specify we don't want an empty chunk
        this.chunkPresent = this.world.getChunk(this.x, this.z, ChunkStatus.FULL, false) != null;
    }

    public void setChunkPresent(boolean flag) {
        this.chunkPresent = flag;
    }

    public boolean isChunkPresent() {
        return this.chunkPresent;
    }

    public void remove(ChunkRender<T> render) {
        int y = render.getChunkY();

        if (this.chunks[y] != null) {
            this.chunks[y] = null;
            this.count--;
        }
    }

    public long getKey() {
        return ChunkPos.toLong(this.x, this.z);
    }

    public boolean isEmpty() {
        return this.count > 0;
    }

    public interface RenderFactory<T extends ChunkRenderData> {
        ChunkRender<T> create(ColumnRender<T> column, int x, int y, int z);
    }
}
