package me.jellysquid.mods.sodium.client.render.chunk.graph;

import net.minecraft.client.render.chunk.ChunkOcclusionData;

public class VisibilityEncoding {
    /**
     * @return a 36-bit representation of the occlusion data
     */
    public static long extract(ChunkOcclusionData occlusionData) {
        if (occlusionData == null) {
            return 0;
        } else {
            return occlusionData.visibility.toLongArray()[0];
        }
    }
}