package net.caffeinemc.sodium.render.chunk.draw;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;

public class ChunkCameraContext {
    public final int blockX, blockY, blockZ;
    public final float deltaX, deltaY, deltaZ;
    public final double posX, posY, posZ;

    public ChunkCameraContext(double x, double y, double z) {
        this.blockX = (int) x;
        this.blockY = (int) y;
        this.blockZ = (int) z;

        // Reduce camera delta precision to 14 bits to avoid seams along chunk/region boundaries
        this.deltaX = (float) Math.round((x - this.blockX) * 0x1p14f) * 0x1p-14f;
        this.deltaY = (float) Math.round((y - this.blockY) * 0x1p14f) * 0x1p-14f;
        this.deltaZ = (float) Math.round((z - this.blockZ) * 0x1p14f) * 0x1p-14f;

        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public ChunkCameraContext(Camera camera) {
        this(camera.getPos().x, camera.getPos().y, camera.getPos().z);
    }

    public BlockPos getBlockPos() {
        return new BlockPos(this.blockX, this.blockY, this.blockZ);
    }
}
