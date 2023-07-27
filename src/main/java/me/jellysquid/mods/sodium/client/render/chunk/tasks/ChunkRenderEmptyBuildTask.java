package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

import java.util.Collections;

import org.joml.Vector3f;

/**
 * A build task which does no computation and always return an empty build result. These tasks are created whenever
 * chunk meshes need to be deleted as the only way to change graphics state is to send a message to the main
 * actor thread. In cases where new chunk renders are being created and scheduled, the scheduler will prefer to just
 * synchronously update the render's data to an empty state to speed things along.
 */
public class ChunkRenderEmptyBuildTask extends ChunkRenderBuildTask {
    public ChunkRenderEmptyBuildTask(RenderSection render, int frame, Vector3f cameraPos) {
        super(render, frame, cameraPos);
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource) {
        return new ChunkMeshBuildResult(this.render, this.frame, ChunkRenderData.EMPTY, Collections.emptyMap());
    }

    @Override
    public void releaseResources() {

    }
}
