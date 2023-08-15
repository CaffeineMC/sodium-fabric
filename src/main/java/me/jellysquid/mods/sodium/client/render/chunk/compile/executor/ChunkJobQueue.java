package me.jellysquid.mods.sodium.client.render.chunk.compile.executor;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

class ChunkJobQueue {
    private final ConcurrentLinkedDeque<ChunkJob> jobs = new ConcurrentLinkedDeque<>();

    private final Semaphore semaphore = new Semaphore(0);

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void add(ChunkJob job, boolean important) {
        Validate.isTrue(this.isRunning(), "Queue is no longer running");

        if (important) {
            this.jobs.addFirst(job);
        } else {
            this.jobs.addLast(job);
        }

        this.semaphore.release(1);
    }

    @Nullable
    public ChunkJob waitForNextJob() throws InterruptedException {
        if (!this.isRunning()) {
            return null;
        }

        this.semaphore.acquire();

        return this.getNextTask();
    }

    public boolean stealJob(ChunkJob job) {
        if (!this.semaphore.tryAcquire()) {
            return false;
        }

        var success = this.jobs.remove(job);

        if (!success) {
            // If we didn't manage to actually steal the task, then we need to release the permit which we did steal
            this.semaphore.release(1);
        }

        return success;
    }

    @Nullable
    private ChunkJob getNextTask() {
        return this.jobs.poll();
    }


    public Collection<ChunkJob> shutdown() {
        var list = new ArrayDeque<ChunkJob>();

        this.isRunning.set(false);

        while (this.semaphore.tryAcquire()) {
            var task = this.jobs.poll();

            if (task != null) {
                list.add(task);
            }
        }

        // force the worker threads to wake up and exit
        this.semaphore.release(Runtime.getRuntime().availableProcessors());

        return list;
    }

    public int size() {
        return this.semaphore.availablePermits();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }
}
