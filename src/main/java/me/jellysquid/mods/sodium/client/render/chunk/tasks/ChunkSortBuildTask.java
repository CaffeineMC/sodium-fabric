package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import org.joml.Vector3f;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkSortBuildResult;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

public class ChunkSortBuildTask extends ChunkRenderBuildTask {
    public ChunkSortBuildTask(RenderSection render, int frame, Vector3f cameraPos) {
        super(render, frame, cameraPos);
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource) {
        this.render.getTranslucentData().sort(this.cameraPos);

        return new ChunkSortBuildResult(this.render, this.frame, this.render.getTranslucentData());
    }

    @Override
    public void releaseResources() {
        // TODO: is there anything to release?
    }
}
