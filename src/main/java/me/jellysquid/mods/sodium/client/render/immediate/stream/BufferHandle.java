package me.jellysquid.mods.sodium.client.render.immediate.stream;

import me.jellysquid.mods.sodium.client.gl.sync.GlFence;

import java.util.function.Supplier;

public interface BufferHandle {
    int getLength();

    int getElementCount();

    int getElementOffset();

    void finish(Supplier<GlFence> fence);
}
