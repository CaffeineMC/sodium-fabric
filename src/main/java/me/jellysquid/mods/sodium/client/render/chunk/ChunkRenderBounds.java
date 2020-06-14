package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderBounds {
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
            this.x1 = Math.min(this.x1, x);
            this.y1 = Math.min(this.y1, y);
            this.z1 = Math.min(this.z1, z);

            this.x2 = Math.max(this.x2, x + 1);
            this.y2 = Math.max(this.y2, y + 1);
            this.z2 = Math.max(this.z2, z + 1);

            this.empty = false;
        }

        public ChunkRenderBounds build(ChunkSectionPos origin) {
            if (this.empty) {
                return new ChunkRenderBounds(origin);
            }

            return new ChunkRenderBounds(
                    Math.max(this.x1, origin.getMinX()),
                    Math.max(this.y1, origin.getMinY()),
                    Math.max(this.z1, origin.getMinZ()),

                    Math.min(this.x2, origin.getMaxX() + 1),
                    Math.min(this.y2, origin.getMaxY() + 1),
                    Math.min(this.z2, origin.getMaxZ() + 1)
            );
        }
    }
}
