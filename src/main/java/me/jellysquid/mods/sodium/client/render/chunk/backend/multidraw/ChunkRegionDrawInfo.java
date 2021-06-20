package me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw;

import me.jellysquid.mods.sodium.client.render.RenderRegion;

public class ChunkRegionDrawInfo {
    public final RenderRegion region;
    public final int commandOffset, commandLength;

    public ChunkRegionDrawInfo(RenderRegion region, int commandOffset, int commandLength) {
        this.region = region;
        this.commandOffset = commandOffset;
        this.commandLength = commandLength;
    }
}
