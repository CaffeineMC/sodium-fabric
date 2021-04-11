package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.ClientWorldExtended;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCacheManager;
import me.jellysquid.mods.sodium.common.util.collections.DequeDrain;
import me.jellysquid.mods.sodium.common.util.pool.ObjectPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBuilder<T extends ChunkGraphicsState> {
    /**
     * The maximum number of jobs that can be queued for a given worker thread.
     */
    private static final int TASK_QUEUE_LIMIT_PER_WORKER = 2;

    private static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");

    private final Deque<WrappedTask<T>> buildQueue = new ConcurrentLinkedDeque<>();
    private final Deque<ChunkBuildResult<T>> uploadQueue = new ConcurrentLinkedDeque<>();

    private final Object jobNotifier = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Thread> threads = new ArrayList<>();

    private final ObjectPool<WorldSlice> pool;

    private World world;
    private Vector3d cameraPosition;
    private BiomeCacheManager biomeCacheManager;
    private BlockRenderPassManager renderPassManager;

    private final int limitThreads;
    private final ChunkVertexType vertexType;
    private final ChunkRenderBackend<T> backend;

    public ChunkBuilder(ChunkVertexType vertexType, ChunkRenderBackend<T> backend) {
        this.vertexType = vertexType;
        this.backend = backend;
        this.limitThreads = getOptimalThreadCount();
        this.pool = new ObjectPool<>(this.getSchedulingBudget(), WorldSlice::new);
    }

    /**
     * Returns the remaining number of build tasks which should be scheduled this frame. If an attempt is made to
     * spawn more tasks than the budget allows, it will block until resources become available.
     */
    public int getSchedulingBudget() {
        return Math.max(0, (this.limitThreads * TASK_QUEUE_LIMIT_PER_WORKER) - this.buildQueue.size());
    }

    /**
     * Spawns a number of work-stealing threads to process results in the build queue. If the builder is already
     * running, this method does nothing and exits.
     */
    public void startWorkers() {
        if (this.running.getAndSet(true)) {
            return;
        }

        if (!this.threads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
        }

        MinecraftClient client = MinecraftClient.getInstance();

        for (int i = 0; i < this.limitThreads; i++) {
            ChunkBuildBuffers buffers = new ChunkBuildBuffers(this.vertexType, this.renderPassManager);
            ChunkRenderContext pipeline = new ChunkRenderContext(client);

            WorkerRunnable worker = new WorkerRunnable(buffers, pipeline);

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
        if (!this.running.getAndSet(false)) {
            return;
        }

        if (this.threads.isEmpty()) {
            throw new IllegalStateException("No threads are alive but the executor is in the RUNNING state");
        }

        LOGGER.info("Stopping worker threads");

        // Notify all worker threads to wake up, where they will then terminate
        synchronized (this.jobNotifier) {
            this.jobNotifier.notifyAll();
        }

        // Wait for every remaining thread to terminate
        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }

        this.threads.clear();

        // Drop any pending work queues and cancel futures
        this.uploadQueue.clear();

        for (WrappedTask<?> job : this.buildQueue) {
            job.future.cancel(true);
        }

        this.buildQueue.clear();

        this.world = null;
        this.biomeCacheManager = null;
        this.pool.reset();
    }

    /**
     * Processes all pending build task uploads using the chunk render backend.
     */
    // TODO: Limit the amount of time this can take per frame
    public boolean performPendingUploads() {
        if (this.uploadQueue.isEmpty()) {
            return false;
        }

        this.backend.upload(new DequeDrain<>(this.uploadQueue));

        return true;
    }

    public CompletableFuture<ChunkBuildResult<T>> schedule(ChunkRenderBuildTask<T> task) {
        if (!this.running.get()) {
            throw new IllegalStateException("Executor is stopped");
        }

        WrappedTask<T> job = new WrappedTask<>(task);

        this.buildQueue.add(job);

        synchronized (this.jobNotifier) {
            this.jobNotifier.notify();
        }

        return job.future;
    }

    /**
     * Sets the current camera position of the player used for task prioritization.
     */
    public void setCameraPosition(double x, double y, double z) {
        this.cameraPosition = new Vector3d(x, y, z);
    }

    /**
     * Returns the current camera position of the player used for task prioritization.
     */
    public Vector3d getCameraPosition() {
        return this.cameraPosition;
    }

    /**
     * @return True if the build queue is empty
     */
    public boolean isBuildQueueEmpty() {
        return this.buildQueue.isEmpty();
    }

    /**
     * Initializes this chunk builder for the given world. If the builder is already running (which can happen during
     * a world teleportation event), the worker threads will first be stopped and all pending tasks will be discarded
     * before being started again.
     * @param world The world instance
     * @param renderPassManager The render pass manager used for the world
     */
    public void init(ClientWorld world, BlockRenderPassManager renderPassManager) {
        if (world == null) {
            throw new NullPointerException("World is null");
        }

        this.stopWorkers();

        this.world = world;
        this.renderPassManager = renderPassManager;
        this.biomeCacheManager = new BiomeCacheManager(world.getDimension().getBiomeAccessType(), ((ClientWorldExtended) world).getBiomeSeed());

        this.startWorkers();
    }

    /**
     * Returns the "optimal" number of threads to be used for chunk build tasks. This is always at least one thread,
     * but can be up to the number of available processor threads on the system.
     */
    private static int getOptimalThreadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a {@link WorldSlice} around the given chunk section. If the chunk section is empty, null is returned.
     * @param pos The position of the chunk section
     * @return A world slice containing the section's context for rendering, or null if it has none
     */
    public WorldSlice createWorldSlice(ChunkSectionPos pos) {
        WorldChunk[] chunks = WorldSlice.createChunkSlice(this.world, pos);

        if (chunks == null) {
            return null;
        }

        WorldSlice slice = this.pool.allocate();
        slice.init(this, this.world, pos, chunks);

        return slice;
    }

    /**
     * Releases a world slice from a build task back to this builder's object pool.
     * @param slice The chunk slice to release
     */
    public void releaseWorldSlice(WorldSlice slice) {
        this.pool.release(slice);
    }

    /**
     * Returns the global biome cache for this world
     */
    public BiomeCacheManager getBiomeCacheManager() {
        return this.biomeCacheManager;
    }

    /**
     * Called after a chunk's status is changed in the world (i.e. after a load or unload.) This is used to reset any
     * caches which depend on its data and to release any pooled resources attached to it.
     * @param x The x-position of the chunk
     * @param z The z-position of the chunk
     */
    public void onChunkStatusChanged(int x, int z) {
        this.biomeCacheManager.dropCachesForChunk(x, z);
    }

    /**
     * Creates a rebuild task and defers it to the work queue. When the task is completed, it will be moved onto the
     * completed uploads queued which will then be drained during the next available synchronization point with the
     * main thread.
     * @param render The render to rebuild
     */
    public void deferRebuild(ChunkRenderContainer<T> render) {
        this.scheduleRebuildTaskAsync(render)
                .thenAccept(this::enqueueUpload);
    }


    /**
     * Enqueues the build task result to the pending result queue to be later processed during the next available
     * synchronization point on the main thread.
     * @param result The build task's result
     */
    private void enqueueUpload(ChunkBuildResult<T> result) {
        this.uploadQueue.add(result);
    }

    /**
     * Schedules the rebuild task asynchronously on the worker pool, returning a future wrapping the task.
     * @param render The render to rebuild
     */
    public CompletableFuture<ChunkBuildResult<T>> scheduleRebuildTaskAsync(ChunkRenderContainer<T> render) {
        return this.schedule(this.createRebuildTask(render));
    }

    /**
     * Creates a task to rebuild the geometry of a {@link ChunkRenderContainer}.
     * @param render The render to rebuild
     */
    private ChunkRenderBuildTask<T> createRebuildTask(ChunkRenderContainer<T> render) {
        render.cancelRebuildTask();

        WorldSlice slice = this.createWorldSlice(render.getChunkPos());

        if (slice == null) {
            return new ChunkRenderEmptyBuildTask<>(render);
        } else {
            return new ChunkRenderRebuildTask<>(this, render, slice, render.getRenderOrigin());
        }
    }

    private class WorkerRunnable implements Runnable {
        private final AtomicBoolean running = ChunkBuilder.this.running;

        // The re-useable build buffers used by this worker for building chunk meshes
        private final ChunkBuildBuffers bufferCache;

        // Making this thread-local provides a small boost to performance by avoiding the overhead in synchronizing
        // caches between different CPU cores
        private final ChunkRenderContext pipeline;

        public WorkerRunnable(ChunkBuildBuffers bufferCache, ChunkRenderContext pipeline) {
            this.bufferCache = bufferCache;
            this.pipeline = pipeline;
        }

        @Override
        public void run() {
            // Run until the chunk builder shuts down
            while (this.running.get()) {
                WrappedTask<T> job = this.getNextJob();

                // If the job is null or no longer valid, keep searching for a task
                if (job == null || job.isCancelled()) {
                    continue;
                }

                ChunkBuildResult<T> result;

                try {
                    // Perform the build task with this worker's local resources and obtain the result
                    result = job.task.performBuild(this.pipeline, this.bufferCache, job);
                } catch (Exception e) {
                    // Propagate any exception from chunk building
                    job.future.completeExceptionally(e);
                    continue;
                } finally {
                    // After the task has executed, it's safe to release any resources attached to the task
                    job.task.releaseResources();
                }

                // The result can be null if the task is cancelled
                if (result != null) {
                    // Notify the future that the result is now available
                    job.future.complete(result);
                } else if (!job.isCancelled()) {
                    // If the job wasn't cancelled and no result was produced, we've hit a bug
                    job.future.completeExceptionally(new RuntimeException("No result was produced by the task"));
                }
            }
        }

        /**
         * Returns the next task which this worker can work on or blocks until one becomes available. If no tasks are
         * currently available, it will wait on {@link ChunkBuilder#jobNotifier} field until notified.
         */
        private WrappedTask<T> getNextJob() {
            WrappedTask<T> job = ChunkBuilder.this.buildQueue.poll();

            if (job == null) {
                synchronized (ChunkBuilder.this.jobNotifier) {
                    try {
                        ChunkBuilder.this.jobNotifier.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            return job;
        }
    }

    private static class WrappedTask<T extends ChunkGraphicsState> implements CancellationSource {
        private final ChunkRenderBuildTask<T> task;
        private final CompletableFuture<ChunkBuildResult<T>> future;

        private WrappedTask(ChunkRenderBuildTask<T> task) {
            this.task = task;
            this.future = new CompletableFuture<>();
        }

        @Override
        public boolean isCancelled() {
            return this.future.isCancelled();
        }
    }
}
