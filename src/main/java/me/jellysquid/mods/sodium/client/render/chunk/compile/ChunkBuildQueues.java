package me.jellysquid.mods.sodium.client.render.chunk.compile;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

public class ChunkBuildQueues {
    private final ConcurrentLinkedDeque<ChunkBuilderJob> synchronousJobs = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ChunkBuilderJob> asynchronousJobs = new ConcurrentLinkedDeque<>();

    private final Semaphore semaphore = new Semaphore(0);

    public void add(ChunkBuilderJob job, boolean asynchronous) {
        if (asynchronous) {
            this.asynchronousJobs.add(job);
        } else {
            this.synchronousJobs.add(job);
        }

        this.semaphore.release(1);
    }

    @Nullable
    public ChunkBuilderJob waitForNextJob() throws InterruptedException {
        this.semaphore.acquire();

        return this.getNextTask();
    }

    @Nullable
    public ChunkBuilderJob stealSynchronousJob() {
        if (!this.semaphore.tryAcquire()) {
            return null;
        }

        var job = this.synchronousJobs.poll();

        if (job == null) {
            // If there was nothing in the synchronous queue, that means we stole from the
            // asynchronous queue, and we need to return the permit
            this.semaphore.release(1);
        }

        return job;
    }

    @Nullable
    private ChunkBuilderJob getNextTask() {
        ChunkBuilderJob job;

        if ((job = this.synchronousJobs.poll()) != null) {
            return job;
        }

        if ((job = this.asynchronousJobs.poll()) != null) {
            return job;
        }

        return job;
    }


    public Collection<ChunkBuilderJob> removeAll() {
        var list = new ArrayDeque<ChunkBuilderJob>();

        while (this.semaphore.tryAcquire()) {
            var task = this.getNextTask();

            if (task != null) {
                list.add(task);
            }
        }

        return list;
    }

    public int size() {
        return this.semaphore.availablePermits();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }
}
