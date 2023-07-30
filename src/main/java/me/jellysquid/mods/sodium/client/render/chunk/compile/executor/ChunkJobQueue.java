package me.jellysquid.mods.sodium.client.render.chunk.compile.executor;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

class ChunkJobQueue {
    private final ConcurrentLinkedDeque<ChunkJob> synchronousJobs = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<ChunkJob> asynchronousJobs = new ConcurrentLinkedDeque<>();

    private final Semaphore semaphore = new Semaphore(0);

    public void add(ChunkJob job, boolean asynchronous) {
        if (asynchronous) {
            this.asynchronousJobs.add(job);
        } else {
            this.synchronousJobs.add(job);
        }

        this.semaphore.release(1);
    }

    @Nullable
    public ChunkJob waitForNextJob() throws InterruptedException {
        this.semaphore.acquire();

        return this.getNextTask();
    }

    @Nullable
    public ChunkJob stealSynchronousJob() {
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
    private ChunkJob getNextTask() {
        ChunkJob job;

        if ((job = this.synchronousJobs.poll()) != null) {
            return job;
        }

        if ((job = this.asynchronousJobs.poll()) != null) {
            return job;
        }

        return job;
    }


    public Collection<ChunkJob> removeAll() {
        var list = new ArrayDeque<ChunkJob>();

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
