package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public abstract class ChunkBuildResult {
    public final RenderSection render;
    public final int buildTime;

    public ChunkBuildResult(RenderSection render, int buildTime) {
        this.render = render;
        this.buildTime = buildTime;
    }

    public abstract void setDataOn(RenderSection section);

    public abstract void delete();
}
