package me.jellysquid.mods.sodium.client.render.chunk.compile;

import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBuilder {
    private final Deque<BuildJob> buildQueue = new ConcurrentLinkedDeque<>();
    private final Queue<ChunkRenderUploadTask> uploadQueue = new ConcurrentLinkedDeque<>();

    private final Object jobNotifier = new Object();

    private final List<Thread> threads = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final int numThreads;

    private World world;
    private Vector3d cameraPosition;

    public ChunkBuilder(int numThreads) {
        this.numThreads = numThreads;
        this.spawnThreads(numThreads);
    }

    public CompletableFuture<ChunkRenderUploadTask> schedule(ChunkRenderBuildTask task) {
        BuildJob job = new BuildJob(task);

        this.buildQueue.add(job);

        synchronized (this.jobNotifier) {
            this.jobNotifier.notify();
        }

        return job.future;
    }

    public void addUploadTask(ChunkRenderUploadTask task) {
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

    private void spawnThreads(int numThreads) {
        this.running.set(true);

        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(new WorkerRunnable(), "Chunk Render Task Executor #" + i);
            thread.start();

            this.threads.add(thread);
        }
    }

    public void shutdown() {
        this.running.set(false);

        synchronized (this.jobNotifier) {
            this.jobNotifier.notifyAll();
        }

        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) { }
        }

        this.threads.clear();
        this.uploadQueue.clear();

        for (BuildJob job : this.buildQueue) {
            job.future.cancel(true);
        }

        this.buildQueue.clear();
    }

    public boolean isEmpty() {
        return this.buildQueue.isEmpty();
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

    public void setWorld(ClientWorld world) {
        this.world = world;
    }

    public void abortTasks() {
        this.shutdown();
        this.spawnThreads(this.numThreads);
    }

    private class WorkerRunnable implements Runnable {
        private final VertexBufferCache bufferCache = new VertexBufferCache();

        @Override
        public void run() {
            while (ChunkBuilder.this.running.get()) {
                BuildJob job = ChunkBuilder.this.buildQueue.poll();

                if (job == null) {
                    synchronized (ChunkBuilder.this.jobNotifier) {
                        try {
                            ChunkBuilder.this.jobNotifier.wait();
                        } catch (InterruptedException ignored) { }
                    }

                    continue;
                }

                if (!job.future.isCancelled()) {
                    ChunkRenderUploadTask uploadTask = job.task.performBuild(this.bufferCache);

                    job.future.complete(uploadTask);
                }

            }
        }
    }

    private static class BuildJob {
        private final ChunkRenderBuildTask task;
        private final CompletableFuture<ChunkRenderUploadTask> future;

        private BuildJob(ChunkRenderBuildTask task) {
            this.task = task;
            this.future = new CompletableFuture<>();
        }
    }
}
