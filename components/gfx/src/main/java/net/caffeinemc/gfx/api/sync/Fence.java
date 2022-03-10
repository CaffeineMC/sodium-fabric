package net.caffeinemc.gfx.api.sync;

public interface Fence {
    boolean poll();

    void sync();
}
