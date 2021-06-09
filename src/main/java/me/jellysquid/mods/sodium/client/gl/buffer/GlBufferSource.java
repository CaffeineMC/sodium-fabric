package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.model.BufferRange;

public class GlBufferSource {
    public final GlBuffer buffer;
    public final BufferRange range;

    public GlBufferSource(GlBuffer buffer) {
        this(buffer, null);
    }

    public GlBufferSource(GlBuffer buffer, BufferRange range) {
        this.buffer = buffer;
        this.range = range;
    }
}
