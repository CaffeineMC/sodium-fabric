package net.caffeinemc.gfx.api.sync;

public interface Fence {
    /**
     * Polls the signaled state of the fence. If it has been signaled, it will be deleted.
     * @return True if the fence has been signaled, otherwise false
     */
    boolean poll();

    /**
     * Performs a blocking wait until the fence to be signaled. If the fence has already been signaled,
     * it will be deleted and this method may return immediately without blocking.
     */
    void sync();

    /**
     * Deletes the fence if it has not already been done. {@link Fence#poll()} and {@link Fence#sync()}
     * are also able to delete the Fence object if it has been signaled.
     *
     * Fences are the only object that can delete itself without using a ResourceDestructor.
     */
    void delete();
}
