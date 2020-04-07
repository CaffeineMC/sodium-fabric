package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import me.jellysquid.mods.sodium.client.world.BiomeCacheManager;
import me.jellysquid.mods.sodium.common.util.arena.Arena;
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
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBuilder {
    private static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");

    private final Deque<WrappedTask> buildQueue = new ConcurrentLinkedDeque<>();
    private final Queue<ChunkRenderUploadTask> uploadQueue = new ConcurrentLinkedDeque<>();

    private final Object jobNotifier = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Thread> threads = new ArrayList<>();

    private final Arena<ChunkSlice> chunkSliceArena;

    private World world;
    private Vector3d cameraPosition;
    private BiomeCacheManager biomeCacheManager;

    private final int limitThreads;

    public ChunkBuilder() {
        this.limitThreads = getOptimalThreadCount();
        this.chunkSliceArena = new Arena<>(this.getBudget(), ChunkSlice::new);
    }

    public int getBudget() {
        return Math.max(0, (this.limitThreads * 3) - this.buildQueue.size());
    }

    public void start() {
        if (this.running.getAndSet(true)) {
            return;
        }

        if (!this.threads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
        }

        for (int i = 0; i < this.limitThreads; i++) {
            Thread thread = new Thread(new WorkerRunnable(), "Chunk Render Task Executor #" + i);
            thread.start();

            this.threads.add(thread);
        }

        LOGGER.info("Started {} worker threads", this.threads.size());
    }

    public void reset() {
        this.shutdown();
        this.start();
    }

    public void shutdown() {
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

        for (WrappedTask job : this.buildQueue) {
            job.future.cancel(true);
        }

        this.buildQueue.clear();

        this.world = null;
        this.biomeCacheManager = null;
    }

    public boolean upload() {
        int count = 0;

        while (!this.uploadQueue.isEmpty()) {
            ChunkRenderUploadTask task = this.uploadQueue.remove();
            task.performUpload();

            count++;
        }

        return count > 0;
    }

    public CompletableFuture<ChunkRenderUploadTask> schedule(ChunkRenderBuildTask task) {
        if (!this.running.get()) {
            throw new IllegalStateException("Executor is stopped");
        }

        WrappedTask job = new WrappedTask(task);

        this.buildQueue.add(job);

        synchronized (this.jobNotifier) {
            this.jobNotifier.notify();
        }

        return job.future;
    }

    public void enqueueUpload(ChunkRenderUploadTask task) {
        this.uploadQueue.add(task);
    }

    public void setCameraPosition(double x, double y, double z) {
        this.cameraPosition = new Vector3d(x, y, z);
    }

    public World getWorld() {
        return this.world;
    }

    public Vector3d getCameraPosition() {
        return this.cameraPosition;
    }

    public boolean isEmpty() {
        return this.buildQueue.isEmpty();
    }

    public void setWorld(ClientWorld world) {
        if (world == null) {
            throw new NullPointerException("World is null");
        }

        this.reset();

        this.world = world;
        this.biomeCacheManager = new BiomeCacheManager(world.getDimension().getType().getBiomeAccessType(), world.getSeed());
    }

    private static int getOptimalThreadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    }

    public ChunkSlice createChunkSlice(ChunkSectionPos pos) {
        WorldChunk[] chunks = ChunkSlice.getChunks(this.world, pos);

        if (chunks == null) {
            return null;
        }

        ChunkSlice slice = this.chunkSliceArena.allocate();
        slice.init(this, this.world, pos, chunks);
        slice.allocateReference();

        return slice;
    }

    public void releaseChunkSlice(ChunkSlice slice) {
        slice.releaseReference();
        slice.cleanup();

        this.chunkSliceArena.reclaim(slice);
    }

    public BiomeCacheManager getBiomeCacheManager() {
        return this.biomeCacheManager;
    }

    public void clearCachesForChunk(int x, int z) {
        this.biomeCacheManager.dropCachesForChunk(x, z);
    }

    private class WorkerRunnable implements Runnable {
        private final VertexBufferCache bufferCache = new VertexBufferCache();
        private final AtomicBoolean running = ChunkBuilder.this.running;

        private final ChunkRenderPipeline pipeline = new ChunkRenderPipeline(MinecraftClient.getInstance());

        @Override
        public void run() {
            while (this.running.get()) {
                WrappedTask job = this.getNextJob();

                if (job == null || job.future.isCancelled()) {
                    continue;
                }

                job.future.complete(job.task.performBuild(this.pipeline, this.bufferCache));
            }
        }

        private WrappedTask getNextJob() {
            WrappedTask job = ChunkBuilder.this.buildQueue.poll();

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

    private static class WrappedTask {
        private final ChunkRenderBuildTask task;
        private final CompletableFuture<ChunkRenderUploadTask> future;

        private WrappedTask(ChunkRenderBuildTask task) {
            this.task = task;
            this.future = new CompletableFuture<>();
        }
    }
}
