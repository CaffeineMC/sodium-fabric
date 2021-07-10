package me.jellysquid.mods.sodium.client.render.chunk;

public class ChunkCameraContext {
    public final int blockX, blockY, blockZ;
    public final float deltaX, deltaY, deltaZ;
    public final float posX, posY, posZ;

    public ChunkCameraContext(double x, double y, double z) {
        this.blockX = (int) x;
        this.blockY = (int) y;
        this.blockZ = (int) z;

        this.deltaX = (float) (x - this.blockX);
        this.deltaY = (float) (y - this.blockY);
        this.deltaZ = (float) (z - this.blockZ);

        this.posX = (float) x;
        this.posY = (float) y;
        this.posZ = (float) z;
    }

}
