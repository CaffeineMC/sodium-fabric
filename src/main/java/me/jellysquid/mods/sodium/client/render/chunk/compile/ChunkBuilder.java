package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCacheManager;
import me.jellysquid.mods.sodium.common.util.arena.Arena;
import me.jellysquid.mods.sodium.common.util.collections.DequeDrain;
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

public class ChunkBuilder<T extends ChunkRenderState> {
    private static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");

    private final Deque<WrappedTask<T>> buildQueue = new ConcurrentLinkedDeque<>();
    private final Deque<ChunkBuildResult<T>> uploadQueue = new ConcurrentLinkedDeque<>();

    private final Object jobNotifier = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Thread> threads = new ArrayList<>();

    private final Arena<WorldSlice> worldSliceArena;

    private World world;
    private Vector3d cameraPosition;
    private BiomeCacheManager biomeCacheManager;
    private BlockRenderPassManager renderPassManager;

    private final int limitThreads;
    private final GlVertexFormat<?> format;

    public ChunkBuilder(GlVertexFormat<?> format) {
        this.format = format;
        this.limitThreads = getOptimalThreadCount();
        this.worldSliceArena = new Arena<>(this.getBudget(), WorldSlice::new);
    }

    public int getBudget() {
        return Math.max(0, (this.limitThreads * 3) - this.buildQueue.size());
    }

    public void startWorkers() {
        if (this.running.getAndSet(true)) {
            return;
        }

        if (!this.threads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
        }

        for (int i = 0; i < this.limitThreads; i++) {
            ChunkBuildBuffers bufferCache = new ChunkBuildBuffers(this.format, this.renderPassManager);
            WorkerRunnable worker = new WorkerRunnable(bufferCache);

            Thread thread = new Thread(worker, "Chunk Render Task Executor #" + i);
            thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
            thread.start();

            this.threads.add(thread);
        }

        LOGGER.info("Started {} worker threads", this.threads.size());
    }

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
        this.worldSliceArena.reset();
    }

    public boolean upload(ChunkRenderBackend<T> backend) {
        if (this.uploadQueue.isEmpty()) {
            return false;
        }

        backend.upload(new DequeDrain<>(this.uploadQueue));

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

    public void enqueueUpload(ChunkBuildResult<T> result) {
        this.uploadQueue.add(result);
    }

    public void setCameraPosition(double x, double y, double z) {
        this.cameraPosition = new Vector3d(x, y, z);
    }

    public Vector3d getCameraPosition() {
        return this.cameraPosition;
    }

    public boolean isEmpty() {
        return this.buildQueue.isEmpty();
    }

    public void init(ClientWorld world, BlockRenderPassManager renderPassManager) {
        if (world == null) {
            throw new NullPointerException("World is null");
        }

        this.stopWorkers();

        this.world = world;
        this.renderPassManager = renderPassManager;
        this.biomeCacheManager = new BiomeCacheManager(world.getDimension().getType().getBiomeAccessType(), world.getSeed());

        this.startWorkers();
    }

    private static int getOptimalThreadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    public WorldSlice createChunkSlice(ChunkSectionPos pos) {
        WorldChunk[] chunks = WorldSlice.getChunks(this.world, pos);

        if (chunks == null) {
            return null;
        }

        WorldSlice slice = this.worldSliceArena.allocate();
        slice.init(this, this.world, pos, chunks);

        return slice;
    }

    public void releaseChunkSlice(WorldSlice slice) {
        this.worldSliceArena.release(slice);
    }

    public BiomeCacheManager getBiomeCacheManager() {
        return this.biomeCacheManager;
    }

    public void clearCachesForChunk(int x, int z) {
        this.biomeCacheManager.dropCachesForChunk(x, z);
    }

    public void rebuild(ChunkRender<T> render) {
        this.createRebuildFuture(render).thenAccept(this::enqueueUpload);
    }

    public CompletableFuture<ChunkBuildResult<T>> createRebuildFuture(ChunkRender<T> render) {
        return this.schedule(this.createRebuildTask(render));
    }

    private ChunkRenderBuildTask<T> createRebuildTask(ChunkRender<T> render) {
        render.cancelRebuildTask();

        WorldSlice slice = this.createChunkSlice(render.getChunkPos());

        if (slice == null) {
            return new ChunkRenderEmptyBuildTask<>(render);
        } else {
            return new ChunkRenderRebuildTask<>(this, render, slice);
        }
    }

    private class WorkerRunnable implements Runnable {
        private final ChunkBuildBuffers bufferCache;
        private final AtomicBoolean running = ChunkBuilder.this.running;

        private final ChunkRenderPipeline pipeline = new ChunkRenderPipeline(MinecraftClient.getInstance());

        public WorkerRunnable(ChunkBuildBuffers bufferCache) {
            this.bufferCache = bufferCache;
        }

        @Override
        public void run() {
            while (this.running.get()) {
                WrappedTask<T> job = this.getNextJob();

                if (job == null || job.future.isCancelled()) {
                    continue;
                }

                ChunkBuildResult<T> result = job.task.performBuild(this.pipeline, this.bufferCache);
                job.task.releaseResources();

                job.future.complete(result);
            }
        }

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

    private static class WrappedTask<T extends ChunkRenderState> {
        private final ChunkRenderBuildTask<T> task;
        private final CompletableFuture<ChunkBuildResult<T>> future;

        private WrappedTask(ChunkRenderBuildTask<T> task) {
            this.task = task;
            this.future = new CompletableFuture<>();
        }
    }
}
