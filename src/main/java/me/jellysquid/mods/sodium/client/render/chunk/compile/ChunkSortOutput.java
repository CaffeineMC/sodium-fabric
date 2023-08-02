package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.TranslucentData;

public class ChunkSortOutput extends BuilderTaskOutput {
    public final TranslucentData translucentData;

    public ChunkSortOutput(RenderSection render, int buildTime, TranslucentData translucentData) {
        super(render, buildTime);

        this.translucentData = translucentData;
    }
}
