package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.TranslucentData;

public class ChunkSortOutput extends BuilderTaskOutput {
    public final TranslucentData translucentData;

    public ChunkSortOutput(RenderSection render, int buildTime, TranslucentData translucentData) {
        super(render, buildTime);

        this.translucentData = translucentData;
    }

    @Override
    public void deleteAfterUpload() {
        super.deleteAfterUpload();

        // delete translucent data if it's not persisted for dynamic sorting
        if (this.translucentData != null && !this.translucentData.getSortType().needsPlaneTrigger) {
            this.translucentData.delete();
        }
    }

    @Override
    public void deleteFully() {
        super.deleteFully();
        if (this.translucentData != null) {
            this.translucentData.delete();
        }
    }
}
