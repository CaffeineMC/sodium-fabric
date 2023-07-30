package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBuilderJob implements CancellationSource {
    final ChunkRenderBuildTask task;
    final AtomicBoolean cancellationToken, completionToken;

    ChunkBuilderJob(ChunkRenderBuildTask task) {
        this.task = task;
        this.cancellationToken = new AtomicBoolean();
        this.completionToken = new AtomicBoolean();
    }

    @Override
    public boolean isCancelled() {
        return this.cancellationToken.get();
    }

    public boolean isDone() {
        return this.completionToken.get();
    }

    public void cancel() {
        this.cancellationToken.set(true);
    }

    public static class Result {
        private final ChunkBuildResult output;
        private final Throwable throwable;

        private Result(ChunkBuildResult output, Throwable throwable) {
            this.output = output;
            this.throwable = throwable;
        }

        public static Result exceptionally(Throwable throwable) {
            return new Result(null, throwable);
        }

        public static Result successfully(ChunkBuildResult result) {
            return new Result(result, null);
        }

        public ChunkBuildResult unwrap() {
            if (this.throwable != null) {
                throw new RuntimeException("Exception thrown while executing job", this.throwable);
            }

            return this.output;
        }
    }
}
