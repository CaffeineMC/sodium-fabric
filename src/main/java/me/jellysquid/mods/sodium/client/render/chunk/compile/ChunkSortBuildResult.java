package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.TranslucentData;

/**
 * TODO: is passing a result around explicitly necessary? Somehow we have to
 * notify the system about an update to the change in index data, but if we
 * store a reference to the TranslucentData on the render section, then setting
 * it here would do nothing. Is it even permissible to modify the translucent
 * data directly or does a copy have to be made?
 */
public class ChunkSortBuildResult extends ChunkBuildResult {
    public final TranslucentData data;

    public ChunkSortBuildResult(RenderSection render, int buildTime, TranslucentData data) {
        super(render, buildTime);
        this.data = data;
    }

    @Override
    public void setDataOn(RenderSection section) {
        // TODO: necessary? see note in class javadoc
        section.setTranslucentData(this.data);
    }

    @Override
    public void delete() {
        // TODO: necessary? only if there are native buffers involved
    }
}
