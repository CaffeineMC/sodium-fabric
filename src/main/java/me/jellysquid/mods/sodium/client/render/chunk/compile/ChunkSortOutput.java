package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.DynamicData;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.PresentTranslucentData;

public class ChunkSortOutput extends BuilderTaskOutput implements OutputWithIndexData {
    public final DynamicData dynamicData;

    public ChunkSortOutput(RenderSection render, int buildTime, DynamicData dynamicData) {
        super(render, buildTime);

        this.dynamicData = dynamicData;
    }

    @Override
    public PresentTranslucentData getTranslucentData() {
        return this.dynamicData;
    }

    // doesn't implement deletion because the task doesn't allocate any new buffers.
    // the buffers used belong to the section and are deleted when it is deleted.
    // buffers created during section building are deleted at section deletion or
    // when the rebuild is cancelled.
}
