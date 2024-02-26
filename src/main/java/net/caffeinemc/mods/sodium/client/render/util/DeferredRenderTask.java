package net.caffeinemc.mods.sodium.client.render.util;

import java.util.concurrent.ConcurrentLinkedDeque;

public class DeferredRenderTask {
    private static final ConcurrentLinkedDeque<Runnable> queue = new ConcurrentLinkedDeque<>();

    /**
     * Schedules a render task to be executed on the main render thread as soon as possible. This is often at the
     * start of the next frame.
     * @param runnable The task to be executed on the render thread
     */
    public static void schedule(Runnable runnable) {
        queue.add(runnable);
    }

    /**
     * Executes all currently pending render tasks. This should only be called from the main render thread!
     */
    public static void runAll() {
        RenderAsserts.validateCurrentThread();

        Runnable runnable;

        while ((runnable = queue.poll()) != null) {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to execute deferred render task", throwable);
            }
        }
    }
}
