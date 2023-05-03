package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderBounds {
    public static final ChunkRenderBounds ALWAYS_FALSE = new ChunkRenderBounds(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

    public final double minX, minY, minZ;
    public final double maxX, maxY, maxZ;

    public ChunkRenderBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static class Builder {
        private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        public void add(float x, float y, float z, ModelQuadFacing facing) {
            if (facing == ModelQuadFacing.UP) {
                this.minY = Math.min(this.minY, y);
            }

            if (facing == ModelQuadFacing.DOWN) {
                this.maxY = Math.max(this.maxY, y);
            }

            if (facing == ModelQuadFacing.EAST) {
                this.minX = Math.min(this.minX, x);
            }

            if (facing == ModelQuadFacing.WEST) {
                this.maxX = Math.max(this.maxX, x);
            }

            if (facing == ModelQuadFacing.SOUTH) {
                this.minZ = Math.min(this.minZ, z);
            }

            if (facing == ModelQuadFacing.NORTH) {
                this.maxZ = Math.max(this.maxZ, z);
            }
        }

        public ChunkRenderBounds build(ChunkSectionPos origin) {
            double minX = origin.getMinX() + (double) this.minX;
            double maxX = origin.getMinX() + (double) this.maxX;

            double minY = origin.getMinY() + (double) this.minY;
            double maxY = origin.getMinY() + (double) this.maxY;

            double minZ = origin.getMinZ() + (double) this.minZ;
            double maxZ = origin.getMinZ() + (double) this.maxZ;

            return new ChunkRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
