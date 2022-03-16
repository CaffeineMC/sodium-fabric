package net.caffeinemc.gfx.api.sync;

public interface Fence {
    /**
     * Polls the signaled state of the fence.
     * @return True if the fence has been signaled, otherwise false
     */
    boolean poll();

    /**
     * Performs a blocking wait until the fence to be signaled. If the fence has already been signaled,
     * this method may return immediately without blocking.
     */
    void sync();
}
