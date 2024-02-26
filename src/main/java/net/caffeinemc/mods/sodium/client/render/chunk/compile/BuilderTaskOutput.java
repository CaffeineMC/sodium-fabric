package net.caffeinemc.mods.sodium.client.render.chunk.compile;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;

public abstract class BuilderTaskOutput {
    public final RenderSection render;
    public final int submitTime;
    private boolean fullyDeleted;

    public BuilderTaskOutput(RenderSection render, int buildTime) {
        this.render = render;
        this.submitTime = buildTime;
    }

    public void deleteFully() {
        this.fullyDeleted = true;
        this.deleteAfterUpload();
    }

    public void deleteAfterUploadSafe() {
        if (!this.fullyDeleted) {
            this.deleteAfterUpload();
        }
    }

    protected void deleteAfterUpload() {
    }
}
