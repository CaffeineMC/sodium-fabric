package me.jellysquid.mods.sodium.client.render.chunk.data;

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

    public ChunkRenderBounds(ChunkSectionPos origin) {
        this.x1 = origin.getMinX();
        this.y1 = origin.getMinY();
        this.z1 = origin.getMinZ();

        this.x2 = origin.getMaxX() + 1;
        this.y2 = origin.getMaxY() + 1;
        this.z2 = origin.getMaxZ() + 1;
    }

    public static class Builder {
        private int x1 = Integer.MAX_VALUE, y1 = Integer.MAX_VALUE, z1 = Integer.MAX_VALUE;
        private int x2 = Integer.MIN_VALUE, y2 = Integer.MIN_VALUE, z2 = Integer.MIN_VALUE;

        private boolean empty = true;

        public void addBlock(int x, int y, int z) {
            if (x < this.x1) {
                this.x1 = x;
            }

            if (x > this.x2) {
                this.x2 = x;
            }

            if (y < this.y1) {
                this.y1 = y;
            }

            if (y > this.y2) {
                this.y2 = y;
            }

            if (z < this.z1) {
                this.z1 = z;
            }

            if (z > this.z2) {
                this.z2 = z;
            }

            this.empty = false;
        }

        public ChunkRenderBounds build(ChunkSectionPos origin) {
            if (this.empty) {
                return new ChunkRenderBounds(origin);
            }

            // Expand the bounding box by 8 blocks (half a chunk) in order to deal with diagonal surfaces
            return new ChunkRenderBounds(
                    Math.max(this.x1, origin.getMinX()) - 8.0f,
                    Math.max(this.y1, origin.getMinY()) - 8.0f,
                    Math.max(this.z1, origin.getMinZ()) - 8.0f,

                    Math.min(this.x2 + 1, origin.getMaxX()) + 8.0f,
                    Math.min(this.y2 + 1, origin.getMaxY()) + 8.0f,
                    Math.min(this.z2 + 1, origin.getMaxZ()) + 8.0f
            );
        }
    }
}
