package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class ChunkCameraContext {
    // We want to reduce the precision of the deltas to avoid seams along chunk/region boundaries. This is done by
    // ensuring the camera position would be the same if we did cameraPos + 0 - 0 as if we did cameraPos + 128 - 128.
    private static final float PRECISION_MODIFIER = RenderRegion.REGION_WIDTH * 16; // 16 blocks per section

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
