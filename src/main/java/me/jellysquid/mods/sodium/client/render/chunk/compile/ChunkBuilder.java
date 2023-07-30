package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.collections.QueueDrainingIterator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBuilder {
    private static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");

    private final ChunkBuildQueues queue = new ChunkBuildQueues();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Thread> workerThreads = new ArrayList<>();

    private World world;
    private final int limitThreads;
    private final ChunkVertexType vertexType;

    private final ConcurrentLinkedDeque<ChunkBuilderJob.Result> results = new ConcurrentLinkedDeque<>();
    private final ThreadLocal<ChunkBuildContext> localContexts = new ThreadLocal<>();

    public ChunkBuilder(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.limitThreads = getThreadCount();
    }

    /**
     * Returns the remaining number of build tasks which should be scheduled this frame. If an attempt is made to
     * spawn more tasks than the budget allows, it will block until resources become available.
     */
    public int getSchedulingBudget() {
        return Math.max(0, this.limitThreads - this.queue.size());
    }

    /**
     * Spawns a number of work-stealing threads to process results in the build queue. If the builder is already
     * running, this method does nothing and exits.
     */
    public void startWorkers() {
        if (this.running.getAndSet(true)) {
            return;
        }

        if (!this.workerThreads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
        }

        for (int i = 0; i < this.limitThreads; i++) {
            ChunkBuildContext context = new ChunkBuildContext(this.world, this.vertexType);
            WorkerRunnable worker = new WorkerRunnable(context);

            Thread thread = new Thread(worker, "Chunk Render Task Executor #" + i);
            thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
            thread.start();

            this.workerThreads.add(thread);
        }

        LOGGER.info("Started {} worker threads", this.workerThreads.size());
    }

    /**
     * Notifies all worker threads to stop and blocks until all workers terminate. After the workers have been shut
     * down, all tasks are cancelled and the pending queues are cleared. If the builder is already stopped, this
     * method does nothing and exits. This method implicitly calls {@link ChunkBuilder#doneStealingTasks()} on the
     * calling thread.
     */
    public void stopWorkers() {
        if (!this.running.getAndSet(false)) {
            return;
        }

        this.shutdownThreads();

        // Delete any queued tasks and resources attached to them
        var jobs = this.queue.removeAll();

        for (ChunkBuilderJob job : jobs) {
            job.cancellationToken.set(true);
        }

        // Delete any results in the deferred queue
        while (!this.results.isEmpty()) {
            var result = this.results.remove();

            var data = result.unwrap();
            data.delete();
        }

        this.world = null;

        this.doneStealingTasks();
    }

    private void shutdownThreads() {
        if (this.workerThreads.isEmpty()) {
            throw new IllegalStateException("No threads are alive but the executor is in the RUNNING state");
        }
        
        LOGGER.info("Stopping worker threads");

        this.running.set(false);

        // Interrupt all the threads, so they wake up if they're waiting on the semaphore
        for (Thread thread : this.workerThreads) {
            thread.interrupt();
        }

        // Wait for every remaining thread to terminate
        for (Thread thread : this.workerThreads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) { }
        }

        this.workerThreads.clear();
    }

    /**
     * Cleans up resources allocated on the currently calling thread for the {@link ChunkBuilder#stealBlockingTask()} method.
     * This method should be called on a thread that has stolen tasks when it is done stealing to prevent resource
     * leaks.
     */
    public void doneStealingTasks() {
        this.localContexts.remove();
    }

    public ChunkBuilderJob scheduleTask(ChunkRenderBuildTask task, boolean asynchronous) {
        if (!this.running.get()) {
            throw new IllegalStateException("Executor is stopped");
        }

        var job = new ChunkBuilderJob(task);

        this.queue.add(job, asynchronous);

        return job;
    }

    /**
     * Initializes this chunk builder for the given world. If the builder is already running (which can happen during
     * a world teleportation event), the worker threads will first be stopped and all pending tasks will be discarded
     * before being started again.
     * @param world The world instance
     */
    public void init(ClientWorld world) {
        if (world == null) {
            throw new NullPointerException("World is null");
        }

        this.stopWorkers();

        this.world = world;

        this.startWorkers();
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

    public Iterator<ChunkBuilderJob.Result> pollResults() {
        return new QueueDrainingIterator<>(this.results);
    }

    /**
     * "Steals" a task on the queue and allows the currently calling thread to execute it using locally-allocated
     * resources instead. While this function returns true, the caller should continually execute it so that additional
     * tasks can be processed.
     *
     * @return True if it was able to steal a task, otherwise false
     */
    public boolean stealBlockingTask() {
        ChunkBuilderJob job = this.queue.stealSynchronousJob();

        if (job == null) {
            return false;
        }

        ChunkBuildContext context = this.localContexts.get();

        if (context == null) {
            this.localContexts.set(context = new ChunkBuildContext(this.world, this.vertexType));
        }

        try {
            this.executeJob(job, context);
        } finally {
            context.release();
        }

        return true;
    }

    private void executeJob(ChunkBuilderJob job, ChunkBuildContext context) {
        ChunkBuilderJob.Result result;

        try {
            if (job.isCancelled()) {
                return;
            }

            // Perform the build task with this worker's local resources and obtain the result
            result = ChunkBuilderJob.Result.successfully(job.task.performBuild(context, job));
        } catch (Throwable throwable) {
            result = ChunkBuilderJob.Result.exceptionally(throwable);
        } finally {
            job.completionToken.set(true);
            job.task.releaseResources();
        }

        this.results.add(result);
    }

    public boolean isBuildQueueEmpty() {
        return this.queue.isEmpty() && this.results.isEmpty();
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
            AtomicBoolean running = ChunkBuilder.this.running;

            // Run until the chunk builder shuts down
            while (running.get()) {
                ChunkBuilderJob job;

                try {
                    job = ChunkBuilder.this.queue.waitForNextJob();
                } catch (InterruptedException ignored) {
                    continue;
                }

                if (job == null) {
                    continue;
                }

                try {
                    ChunkBuilder.this.executeJob(job, this.context);
                } finally {
                    this.context.release();
                }
            }
        }
    }
}
