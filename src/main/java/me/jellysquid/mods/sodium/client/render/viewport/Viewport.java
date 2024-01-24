package me.jellysquid.mods.sodium.client.render.viewport;

import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3d;

public final class Viewport {
    private final Frustum frustum;
    private final CameraTransform transform;

    private final ChunkSectionPos chunkCoords;
    private final BlockPos blockCoords;

    public Viewport(Frustum frustum, Vector3d position) {
        this.frustum = frustum;
        this.transform = new CameraTransform(position.x, position.y, position.z);

        this.chunkCoords = ChunkSectionPos.from(
                ChunkSectionPos.getSectionCoord(position.x),
                ChunkSectionPos.getSectionCoord(position.y),
                ChunkSectionPos.getSectionCoord(position.z)
        );

        this.blockCoords = BlockPos.ofFloored(position.x, position.y, position.z);
    }

    public boolean isBoxVisible(int intOriginX, int intOriginY, int intOriginZ, float floatSizeX, float floatSizeY, float floatSizeZ) {
        float floatOriginX = (intOriginX - this.transform.intX) - this.transform.fracX;
        float floatOriginY = (intOriginY - this.transform.intY) - this.transform.fracY;
        float floatOriginZ = (intOriginZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testAab(
                floatOriginX - floatSizeX,
                floatOriginY - floatSizeY,
                floatOriginZ - floatSizeZ,

                floatOriginX + floatSizeX,
                floatOriginY + floatSizeY,
                floatOriginZ + floatSizeZ
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
