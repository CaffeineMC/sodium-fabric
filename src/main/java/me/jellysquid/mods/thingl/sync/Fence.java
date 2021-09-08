package me.jellysquid.mods.thingl.sync;

public interface Fence {
    boolean isCompleted();

    void sync();

    void sync(long timeout);
}
