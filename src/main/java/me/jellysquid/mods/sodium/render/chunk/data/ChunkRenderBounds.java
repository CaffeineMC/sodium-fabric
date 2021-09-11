package me.jellysquid.mods.sodium.render.chunk.data;

import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacingBits;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderBounds {
    public static final ChunkRenderBounds ALWAYS_FALSE = new ChunkRenderBounds(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

    public final float x1, y1, z1;
    public final float x2, y2, z2;

    public ChunkRenderBounds(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;

        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public int calculateVisibility(float cameraX, float cameraY, float cameraZ) {
        int visibility = 0;

        if (cameraY > this.y2) {
            visibility |= ModelQuadFacingBits.UP_BITS;
        }

        if (cameraY < this.y1) {
            visibility |= ModelQuadFacingBits.DOWN_BITS;
        }

        if (cameraX > this.x2) {
            visibility |= ModelQuadFacingBits.EAST_BITS;
        }

        if (cameraX < this.x1) {
            visibility |= ModelQuadFacingBits.WEST_BITS;
        }

        if (cameraZ > this.z2) {
            visibility |= ModelQuadFacingBits.SOUTH_BITS;
        }

        if (cameraZ < this.z1) {
            visibility |= ModelQuadFacingBits.NORTH_BITS;
        }

        return visibility;
    }

    public static class Builder {
        private float x1;
        private float y1;
        private float z1;
        private float x2;
        private float y2;
        private float z2;

        public Builder() {
            this.reset();
        }

        public void reset() {
            this.x1 = Float.NEGATIVE_INFINITY;
            this.y1 = Float.NEGATIVE_INFINITY;
            this.z1 = Float.NEGATIVE_INFINITY;
            this.x2 = Float.POSITIVE_INFINITY;
            this.y2 = Float.POSITIVE_INFINITY;
            this.z2 = Float.POSITIVE_INFINITY;
        }

        public void updateBounds(ModelQuadFacing facing, float x, float y, float z) {
            switch (facing) {
                case UP -> this.y2 = Math.min(this.y2, y);
                case DOWN -> this.y1 = Math.max(this.y1, y);
                case EAST -> this.x2 = Math.min(this.x2, x);
                case WEST -> this.x1 = Math.max(this.x1, x);
                case SOUTH -> this.z2 = Math.min(this.z2, z);
                case NORTH -> this.z1 = Math.max(this.z1, z);
            }
        }

        public ChunkRenderBounds build(ChunkSectionPos origin) {
            return new ChunkRenderBounds(
                    (origin.getMinX() + this.x1) + 0.5f,
                    (origin.getMinY() + this.y1) + 0.5f,
                    (origin.getMinZ() + this.z1) + 0.5f,
                    (origin.getMinX() + this.x2) - 0.5f,
                    (origin.getMinY() + this.y2) - 0.5f,
                    (origin.getMinZ() + this.z2) - 0.5f
            );
        }
    }
}
