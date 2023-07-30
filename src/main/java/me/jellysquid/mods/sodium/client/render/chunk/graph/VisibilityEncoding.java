package me.jellysquid.mods.sodium.client.render.chunk.graph;

import net.minecraft.client.render.chunk.ChunkOcclusionData;

public class VisibilityEncoding {
    public static long extract(ChunkOcclusionData occlusionData) {
        if (occlusionData == null) {
            return 0;
        } else {
            return occlusionData.visibility.toLongArray()[0];
        }
    }
}