package me.jellysquid.mods.sodium.client.render.viewport;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.FrustumIntersection;

public final class Viewport {
    private final FrustumIntersection frustum;
    private final CameraTransform transform;

    private final ChunkSectionPos chunkCoords;
    private final BlockPos blockCoords;

    public Viewport(FrustumIntersection frustum, double x, double y, double z) {
        this.frustum = frustum;
        this.transform = new CameraTransform(x, y, z);

        this.chunkCoords = ChunkSectionPos.from(
                ChunkSectionPos.getSectionCoord(x),
                ChunkSectionPos.getSectionCoord(y),
                ChunkSectionPos.getSectionCoord(z)
        );

        this.blockCoords = BlockPos.ofFloored(x, y, z);
    }

    public boolean isBoxVisible(int intX, int intY, int intZ, float radius) {
        float floatX = (intX - this.transform.intX) - this.transform.fracX;
        float floatY = (intY - this.transform.intY) - this.transform.fracY;
        float floatZ = (intZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testAab(
                floatX - radius,
                floatY - radius,
                floatZ - radius,

                floatX + radius,
                floatY + radius,
                floatZ + radius
        );
    }

    public CameraTransform getTransform() {
        return this.transform;
    }

    public ChunkSectionPos getChunkCoord() {
        return this.chunkCoords;
    }

    public BlockPos getBlockCoord() {
        return this.blockCoords;
    }
}
