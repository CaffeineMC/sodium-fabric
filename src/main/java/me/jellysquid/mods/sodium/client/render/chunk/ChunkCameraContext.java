package me.jellysquid.mods.sodium.client.render.chunk;

public class ChunkCameraContext {
    // 32-bit floats have a 23-bit mantissa. We want to reduce that to 16 bits to avoid seams along chunk/region
    // boundaries. To do this, we have to add and subtract a number with (23 - 16) bits to effectively shift
    // the bits out of the number.
    private static final float PRECISION_MODIFIER = 0x1p7f;

    public final int blockX, blockY, blockZ;
    public final float deltaX, deltaY, deltaZ;
    public final double posX, posY, posZ;

    public ChunkCameraContext(double x, double y, double z) {
        this.blockX = (int) x;
        this.blockY = (int) y;
        this.blockZ = (int) z;

        float deltaXFullPrecision = (float) (x - this.blockX);
        float deltaYFullPrecision = (float) (y - this.blockY);
        float deltaZFullPrecision = (float) (z - this.blockZ);

        float deltaXModifier = Math.copySign(PRECISION_MODIFIER, deltaXFullPrecision);
        float deltaYModifier = Math.copySign(PRECISION_MODIFIER, deltaYFullPrecision);
        float deltaZModifier = Math.copySign(PRECISION_MODIFIER, deltaZFullPrecision);

        this.deltaX = (deltaXFullPrecision + deltaXModifier) - deltaXModifier;
        this.deltaY = (deltaYFullPrecision + deltaYModifier) - deltaYModifier;
        this.deltaZ = (deltaZFullPrecision + deltaZModifier) - deltaZModifier;

        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

}
