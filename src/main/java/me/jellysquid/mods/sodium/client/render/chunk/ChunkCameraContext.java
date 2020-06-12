package me.jellysquid.mods.sodium.client.render.chunk;

public class ChunkCameraContext {
    public final int blockOriginX, blockOriginY, blockOriginZ;
    public final float originX, originY, originZ;

    public ChunkCameraContext(double x, double y, double z) {
        this.blockOriginX = (int) x;
        this.blockOriginY = (int) y;
        this.blockOriginZ = (int) z;

        this.originX = (float) (x - this.blockOriginX);
        this.originY = (float) (y - this.blockOriginY);
        this.originZ = (float) (z - this.blockOriginZ);
    }

    public float getChunkModelOffset(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        int t = chunkBlockPos - cameraBlockPos;
        return t - cameraPos;
    }
}
