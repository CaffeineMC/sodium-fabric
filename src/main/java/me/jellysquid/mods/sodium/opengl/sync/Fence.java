package me.jellysquid.mods.sodium.opengl.sync;

public interface Fence {
    boolean poll();

    void sync();
}
