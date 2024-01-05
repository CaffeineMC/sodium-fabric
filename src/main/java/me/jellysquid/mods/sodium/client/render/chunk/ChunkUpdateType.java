package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortingTask;

public enum ChunkUpdateType {
    SORT(Integer.MAX_VALUE, ChunkBuilderSortingTask.SORT_TASK_EFFORT),
    INITIAL_BUILD(128, ChunkBuilderMeshingTask.MESH_TASK_EFFORT),
    REBUILD(Integer.MAX_VALUE, ChunkBuilderMeshingTask.MESH_TASK_EFFORT),
    IMPORTANT_REBUILD(Integer.MAX_VALUE, ChunkBuilderMeshingTask.MESH_TASK_EFFORT),
    IMPORTANT_SORT(Integer.MAX_VALUE, ChunkBuilderSortingTask.SORT_TASK_EFFORT);

    private final int maximumQueueSize;
    private final int taskEffort;

    ChunkUpdateType(int maximumQueueSize, int taskEffort) {
        this.maximumQueueSize = maximumQueueSize;
        this.taskEffort = taskEffort;
    }

    public static ChunkUpdateType getPromotionUpdateType(ChunkUpdateType prev, ChunkUpdateType next) {
        if (prev == null || prev == SORT || prev == next) {
            return next;
        }
        if (next == IMPORTANT_REBUILD
                || (prev == IMPORTANT_SORT && next == REBUILD)
                || (prev == REBUILD && next == IMPORTANT_SORT)) {
            return IMPORTANT_REBUILD;
        }
        return null;
    }

    public int getMaximumQueueSize() {
        return this.maximumQueueSize;
    }

    public boolean isImportant() {
        return this == IMPORTANT_REBUILD || this == IMPORTANT_SORT;
    }

    public int getTaskEffort() {
        return this.taskEffort;
    }
}
