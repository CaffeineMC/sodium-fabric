package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;

public abstract class BuilderTaskOutput {
    public final RenderSection render;
    public final int buildTime;

    public BuilderTaskOutput(RenderSection render, int buildTime) {
        this.render = render;
        this.buildTime = buildTime;
    }

    public void delete() {
    }
}
