package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.util.collections.QueueDrainingIterator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkBuilder {
    private static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");


    protected volatile boolean running;
    private final AtomicInteger idleThreads = new AtomicInteger();
    private final List<Thread> threads = new ArrayList<>();

    private World world;
    private final ChunkVertexType vertexType;

    private final Semaphore jobCount = new Semaphore(0);
    private final Deque<ChunkRenderBuildTask> buildQueue = new ConcurrentLinkedDeque<>();
    private final Queue<ChunkBuildResult> deferredResultQueue = new ConcurrentLinkedDeque<>();

    public ChunkBuilder(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
    }

    /**
     * Returns the remaining number of build tasks which should be scheduled this frame. If an attempt is made to
     * spawn more tasks than the budget allows, it will block until resources become available.
     */
    public int getSchedulingBudget() {
        return Math.max(0, this.threads.size() - this.buildQueue.size());
    }

    /**
     * Spawns a number of work-stealing threads to process results in the build queue. If the builder is already
     * running, this method does nothing and exits.
     */
    public void startWorkers() {
        this.running = true;

        if (!this.threads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
        }

        int workerCount = getThreadCount();
        for (int i = 0; i < workerCount; i++) {
            ChunkWorker worker = new ChunkWorker();

            Thread thread = new Thread(worker, "Chunk Render Task Executor #" + i);
            thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
            thread.start();

            this.threads.add(thread);
        }

        LOGGER.info("Started {} worker threads", this.threads.size());
    }

    /**
     * Notifies all worker threads to stop and blocks until all workers terminate. After the workers have been shut
     * down, all tasks are cancelled and the pending queues are cleared. If the builder is already stopped, this
     * method does nothing and exits.
     */
    public void stopWorkers() {
        boolean wasRunning = this.running;
        this.running = false;
        if (!wasRunning) {
            return;
        }

        if (this.threads.isEmpty()) {
            throw new IllegalStateException("No threads are alive but the executor is in the RUNNING state");
        }

        LOGGER.info("Stopping worker threads");

        //Flood workers with permits, this will release them and will all immediately exit
        jobCount.release(100000);

        // Wait for every remaining thread to terminate
        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }

        this.threads.clear();

        // Delete any queued tasks and resources attached to them
        while (!this.buildQueue.isEmpty()) {
            var job = this.buildQueue.poll();
            job.cancel();
            job.releaseResources();
        }

        // Delete any results in the deferred queue
        while (!this.deferredResultQueue.isEmpty()) {
            this.deferredResultQueue.remove()
                    .delete();
        }

        this.buildQueue.clear();

        this.world = null;

    }


    public void enqueue(ChunkRenderBuildTask task, boolean important) {
        if (task.isComplete()) {
            return;
        }

        //If the build is important, make sure it gets done first
        if (important) {
            task.important = true;
            buildQueue.addFirst(task);
        } else {
            buildQueue.add(task);
        }

        jobCount.release();
    }

    private final Iterator<ChunkBuildResult> drain = new QueueDrainingIterator<>(deferredResultQueue);
    public Iterator<ChunkBuildResult> getResultDrain() {
        return drain;
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

    public boolean isIdle() {
        return idleThreads.get() == threads.size();
    }

    private static void processJob(ChunkRenderBuildTask job, ChunkBuildContext context, Queue<ChunkBuildResult> resultQueue) {
        if (job.isCancelled() || job.execute()) {
            return;
        }

        ChunkBuildResult result;

        try {
            // Perform the build task with this worker's local resources and obtain the result
            result = job.performBuild(context, job::isCancelled);
        } finally {
            job.releaseResources();
        }

        // The result can be null if the task is cancelled
        if (result != null) {
            // Notify the future that the result is now available
            job.complete(result);

            //If the job wasnt important (i.e. in the immediate blocking queue) then enqueue it to be drained
            if (!job.important) {
                resultQueue.add(result);
            }
        } else if (!job.isCancelled()) {
            // If the job wasn't cancelled and no result was produced, we've hit a bug
            throw new RuntimeException("No result was produced by the task");
        }
    }

    public boolean isBuildQueueEmpty() {
        return jobCount.availablePermits() == 0;
    }

    private class ChunkWorker implements Runnable {
        @Override
        public void run() {
            //Try with our local context until we exit or something else goes wrong
            ChunkBuildContext context = new ChunkBuildContext(ChunkBuilder.this.world, ChunkBuilder.this.vertexType);
            try {
                // Run forever
                while (true) {
                    idleThreads.incrementAndGet();
                    //Acquire a permit
                    ChunkBuilder.this.jobCount.acquire(1);
                    idleThreads.decrementAndGet();
                    if (!ChunkBuilder.this.running) {
                        //If we arnt running anymore, exit
                        return;
                    }

                    processJob(ChunkBuilder.this.buildQueue.poll(), context, deferredResultQueue);
                }
            } catch (Throwable exception) {//Catch everything
                //TODO: report this exception to the main thread, to throw
                exception.printStackTrace();
            } finally {
                context.release();
            }
        }
    }
}
