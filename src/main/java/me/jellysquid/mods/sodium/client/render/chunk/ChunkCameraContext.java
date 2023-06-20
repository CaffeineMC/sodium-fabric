package me.jellysquid.mods.sodium.client.render.chunk;

public class ChunkCameraContext {
    // 32-bit floats have a 23-bit mantissa. We want to reduce that to 14 bits to avoid seams along chunk/region
    // boundaries. To do this, we have to add and subtract a number with (23 - 14) bits to effectively shift
    // the bits out of the number.
    private static final float PRECISION_MODIFIER = 0x1p9f; // 512.0f

    public final int blockX, blockY, blockZ;
    public final float deltaX, deltaY, deltaZ;
    public final double posX, posY, posZ;

    public ChunkCameraContext(double x, double y, double z) {
        this.blockX = (int) x;
        this.blockY = (int) y;
        this.blockZ = (int) z;

        this.deltaX = ((float) (x - this.blockX) + PRECISION_MODIFIER) - PRECISION_MODIFIER;
        this.deltaY = ((float) (y - this.blockY) + PRECISION_MODIFIER) - PRECISION_MODIFIER;
        this.deltaZ = ((float) (z - this.blockZ) + PRECISION_MODIFIER) - PRECISION_MODIFIER;

        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

}
