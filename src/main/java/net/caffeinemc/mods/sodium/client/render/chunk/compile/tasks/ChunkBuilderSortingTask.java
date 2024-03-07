package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import org.joml.Vector3dc;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;

public class ChunkBuilderSortingTask extends ChunkBuilderTask<ChunkSortOutput> {
    private final DynamicData dynamicData;

    public ChunkBuilderSortingTask(RenderSection render, int frame, Vector3dc absoluteCameraPos,
            DynamicData dynamicData) {
        super(render, frame, absoluteCameraPos);
        this.dynamicData = dynamicData;
    }

    @Override
    public ChunkSortOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
        if (cancellationToken.isCancelled()) {
            return null;
        }
        this.render.getTranslucentData().sortOnTrigger(this.cameraPos);
        return new ChunkSortOutput(this.render, this.submitTime, this.dynamicData);
    }

    public static ChunkBuilderSortingTask createTask(RenderSection render, int frame, Vector3dc absoluteCameraPos) {
        if (render.getTranslucentData() instanceof DynamicData dynamicData) {
            return new ChunkBuilderSortingTask(render, frame, absoluteCameraPos, dynamicData);
        }
        return null;
    }

    @Override
    public int getEffort() {
        return ChunkBuilder.LOW_EFFORT;
    }
}
