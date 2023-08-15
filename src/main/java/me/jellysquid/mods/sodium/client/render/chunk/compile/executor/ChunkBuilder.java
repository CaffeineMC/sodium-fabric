package me.jellysquid.mods.sodium.client.render.chunk.compile.executor;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ChunkBuilder {
    static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");

    private final ChunkJobQueue queue = new ChunkJobQueue();

    private final List<Thread> threads = new ArrayList<>();

    private final AtomicInteger busyThreadCount = new AtomicInteger();

    private final ChunkBuildContext localContext;

    public ChunkBuilder(ClientWorld world, ChunkVertexType vertexType) {
        int count = getThreadCount();

        for (int i = 0; i < count; i++) {
            ChunkBuildContext context = new ChunkBuildContext(world, vertexType);
            WorkerRunnable worker = new WorkerRunnable(context);

            Thread thread = new Thread(worker, "Chunk Render Task Executor #" + i);
            thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
            thread.start();

            this.threads.add(thread);
        }

        LOGGER.info("Started {} worker threads", this.threads.size());

        this.localContext = new ChunkBuildContext(world, vertexType);
    }

    /**
     * Returns the remaining number of build tasks which should be scheduled this frame. If an attempt is made to
     * spawn more tasks than the budget allows, it will block until resources become available.
     */
    public int getSchedulingBudget() {
        return Math.max(0, this.threads.size() - this.queue.size());
    }

    /**
     * <p>Notifies all worker threads to stop and blocks until all workers terminate. After the workers have been shut
     * down, all tasks are cancelled and the pending queues are cleared. If the builder is already stopped, this
     * method does nothing and exits.</p>
     *
     * <p>After shutdown, all previously scheduled jobs will have been cancelled. Jobs that finished while
     * waiting for worker threads to shut down will still have their results processed for later cleanup.</p>
     */
    public void shutdown() {
        if (!this.queue.isRunning()) {
            throw new IllegalStateException("Worker threads are not running");
        }

        // Delete any queued tasks and resources attached to them
        var jobs = this.queue.shutdown();

        for (var job : jobs) {
            job.setCancelled();
        }

        this.shutdownThreads();
    }

    private void shutdownThreads() {
        LOGGER.info("Stopping worker threads");

        // Wait for every remaining thread to terminate
        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) { }
        }

        this.threads.clear();
    }

    public <TASK extends ChunkBuilderTask<OUTPUT>, OUTPUT> ChunkJobTyped<TASK, OUTPUT> scheduleTask(TASK task, boolean important,
                                                                                                    Consumer<ChunkJobResult<OUTPUT>> consumer)
    {
        Validate.notNull(task, "Task must be non-null");

        if (!this.queue.isRunning()) {
            throw new IllegalStateException("Executor is stopped");
        }

        var job = new ChunkJobTyped<>(task, consumer);

        this.queue.add(job, important);

        return job;
    }

    /**
     * Returns the "optimal" number of threads to be used for chunk build tasks. This will always return at least one
     * thread.
     */
    private static int getOptimalThreadCount() {
        return MathHelper.clamp(Math.max(getMaxThreadCount() / 3, getMaxThreadCount() - 6), 1, 10);
    }

    private static int getThreadCount() {
        int requested = SodiumClientMod.options().performance.chunkBuilderThreads;
        return requested == 0 ? getOptimalThreadCount() : Math.min(requested, getMaxThreadCount());
    }

    private static int getMaxThreadCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public void tryStealTask(ChunkJob job) {
        if (!this.queue.stealJob(job)) {
            return;
        }

        var localContext = this.localContext;

        try {
            job.execute(localContext);
        } finally {
            localContext.cleanup();
        }
    }

    public boolean isBuildQueueEmpty() {
        return this.queue.isEmpty();
    }

    public int getScheduledJobCount() {
        return this.queue.size();
    }

    public int getBusyThreadCount() {
        return this.busyThreadCount.get();
    }

    public int getTotalThreadCount() {
        return this.threads.size();
    }

    private class WorkerRunnable implements Runnable {
        // Making this thread-local provides a small boost to performance by avoiding the overhead in synchronizing
        // caches between different CPU cores
        private final ChunkBuildContext context;

        public WorkerRunnable(ChunkBuildContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            // Run until the chunk builder shuts down
            while (ChunkBuilder.this.queue.isRunning()) {
                ChunkJob job;

                try {
                    job = ChunkBuilder.this.queue.waitForNextJob();
                } catch (InterruptedException ignored) {
                    continue;
                }

                if (job == null) {
                    // might mean we are not running anymore... go around and check isRunning
                    continue;
                }

                ChunkBuilder.this.busyThreadCount.getAndIncrement();

                try {
                    job.execute(this.context);
                } finally {
                    this.context.cleanup();

                    ChunkBuilder.this.busyThreadCount.decrementAndGet();
                }
            }
        }
    }
}
