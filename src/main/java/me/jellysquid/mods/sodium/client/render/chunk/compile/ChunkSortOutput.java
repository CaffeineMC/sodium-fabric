package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.TranslucentData;

/**
 * TODO: translucent data can also be for non-dynamic sort types, so this might
 * not make a lot of sense as it is.
 */
public class ChunkSortOutput extends BuilderTaskOutput {
    public final TranslucentData translucentData;

    public ChunkSortOutput(RenderSection render, int buildTime, TranslucentData translucentData) {
        super(render, buildTime);

        this.translucentData = translucentData;
    }
}
