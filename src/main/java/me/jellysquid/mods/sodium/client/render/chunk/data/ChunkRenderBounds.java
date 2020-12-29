package me.jellysquid.mods.sodium.client.render.chunk.data;

import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderBounds {
    public static final ChunkRenderBounds ALWAYS_FALSE = new ChunkRenderBounds(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

    public final float x1, x2;
    public final float y1, y2;
    public final float z1, z2;

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
        // Bit-mask of the blocks set on each axis
        private int x = 0, y = 0, z = 0;

        public void addBlock(int x, int y, int z) {
            // Accumulate bits on each axis for the given block position. This avoids needing to
            // branch multiple times trying to determine if the bounds need to grow. The min/max
            // value of each axis can later be extracted by counting the trailing/leading zeroes.
            this.x |= 1 << x;
            this.y |= 1 << y;
            this.z |= 1 << z;
        }

        public ChunkRenderBounds build(ChunkSectionPos origin) {
            // If no bits were set on any axis, return the default bounds
            if ((this.x | this.y | this.z) == 0) {
                return new ChunkRenderBounds(origin);
            }

            int x1 = origin.getMinX() + leftBound(this.x);
            int x2 = origin.getMinX() + rightBound(this.x);

            int y1 = origin.getMinY() + leftBound(this.y);
            int y2 = origin.getMinY() + rightBound(this.y);

            int z1 = origin.getMinZ() + leftBound(this.z);
            int z2 = origin.getMinZ() + rightBound(this.z);

            return new ChunkRenderBounds(
                    Math.max(x1, origin.getMinX()) - 0.5f,
                    Math.max(y1, origin.getMinY()) - 0.5f,
                    Math.max(z1, origin.getMinZ()) - 0.5f,

                    Math.min(x2, origin.getMaxX()) + 0.5f,
                    Math.min(y2, origin.getMaxY()) + 0.5f,
                    Math.min(z2, origin.getMaxZ()) + 0.5f
            );
        }

        // Return the left-bound of the bit-masked axis
        private static int leftBound(int i) {
            return Integer.numberOfTrailingZeros(i);
        }

        // Return the right-bound of the bit-masked axis
        private static int rightBound(int i) {
            return 32 - Integer.numberOfLeadingZeros(i);
        }
    }
}
