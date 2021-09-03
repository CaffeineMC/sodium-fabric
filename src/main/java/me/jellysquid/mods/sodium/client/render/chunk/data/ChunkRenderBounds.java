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
        this.x1 = origin.getMinX() - 8;
        this.y1 = origin.getMinY() - 8;
        this.z1 = origin.getMinZ() - 8;

        this.x2 = origin.getMaxX() + 8;
        this.y2 = origin.getMaxY() + 8;
        this.z2 = origin.getMaxZ() + 8;
    }
}
