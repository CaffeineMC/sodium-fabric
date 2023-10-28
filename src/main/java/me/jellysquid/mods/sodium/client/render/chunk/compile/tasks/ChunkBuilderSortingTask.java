package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import org.joml.Vector3dc;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.DynamicData;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;

public class ChunkBuilderSortingTask extends ChunkBuilderTask<ChunkSortOutput> {
    private final DynamicData dynamicData;

    public ChunkBuilderSortingTask(RenderSection render, int frame, Vector3dc absoluteCameraPos,
            DynamicData dynamicData) {
        super(render, frame, absoluteCameraPos);
        this.dynamicData = dynamicData;
    }

    @Override
    public ChunkSortOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
        this.render.getTranslucentData().sortOnTrigger(this.cameraPos);
        return new ChunkSortOutput(this.render, this.submitTime, this.dynamicData);
    }

    public static ChunkBuilderSortingTask createTask(RenderSection render, int frame, Vector3dc absoluteCameraPos) {
        if (render.getTranslucentData() instanceof DynamicData dynamicData) {
            return new ChunkBuilderSortingTask(render, frame, absoluteCameraPos, dynamicData);
        }
        return null;
    }
}
