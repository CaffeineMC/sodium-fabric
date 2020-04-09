package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;

public class ColumnRender<T extends ChunkRenderState> {
    @SuppressWarnings("unchecked")
    private final ChunkRender<T>[] chunks = new ChunkRender[16];
    private final World world;

    private final int chunkX, chunkZ;
    private final Box boundingBox;

    private int count;
    private boolean chunkPresent;
    private boolean visible;
    private int lastFrame = -1;

    public ColumnRender(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        int x = chunkX << 4;
        int z = chunkZ << 4;

        this.boundingBox = new Box(x, Double.NEGATIVE_INFINITY, z, x + 16.0, Double.POSITIVE_INFINITY, z + 16.0);

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
            chunk = factory.create(this, this.chunkX, y, this.chunkZ);

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
        this.chunkPresent = this.world.getChunk(this.chunkX, this.chunkZ, ChunkStatus.FULL, false) != null;
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
        return ChunkPos.toLong(this.chunkX, this.chunkZ);
    }

    public boolean isEmpty() {
        return this.count > 0;
    }

    public boolean isVisible(Frustum frustum, int frame) {
        if (this.lastFrame == frame) {
            return this.visible;
        }

        this.visible = frustum.isVisible(this.boundingBox);
        this.lastFrame = frame;

        return this.visible;
    }

    public ChunkRender<T>[] getChunks() {
        return this.chunks;
    }

    public interface RenderFactory<T extends ChunkRenderState> {
        ChunkRender<T> create(ColumnRender<T> column, int x, int y, int z);
    }
}
