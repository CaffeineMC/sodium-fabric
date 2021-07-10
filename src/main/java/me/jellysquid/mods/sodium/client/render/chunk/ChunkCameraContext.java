package me.jellysquid.mods.sodium.client.render.chunk;

public class ChunkCameraContext {
    public final int blockX, blockY, blockZ;
    public final float deltaX, deltaY, deltaZ;
    public final float posX, posY, posZ;

    public ChunkCameraContext(double x, double y, double z) {
        this.blockX = (int) x;
        this.blockY = (int) y;
        this.blockZ = (int) z;

        // Reduce camera delta precision to 14 bits to avoid seams along chunk/region boundaries
        this.deltaX = (float) Math.round((x - this.blockX) * 0x1p14f) * 0x1p-14f;
        this.deltaY = (float) Math.round((y - this.blockY) * 0x1p14f) * 0x1p-14f;
        this.deltaZ = (float) Math.round((z - this.blockZ) * 0x1p14f) * 0x1p-14f;

        this.posX = (float) x;
        this.posY = (float) y;
        this.posZ = (float) z;
    }

}
