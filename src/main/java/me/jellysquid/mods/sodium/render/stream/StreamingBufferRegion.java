package me.jellysquid.mods.sodium.render.stream;

import java.nio.ByteBuffer;

import me.jellysquid.mods.sodium.opengl.sync.Fence;

public class StreamingBufferRegion {
    private final long offset;
    private final long length;
    private final ByteBuffer pointer;

    private Fence fence;

    public StreamingBufferRegion(long offset, long length, ByteBuffer pointer) {
        this.offset = offset;
        this.length = length;
        this.pointer = pointer;
    }

    public long getLength() {
        return length;
    }

    public long getOffset() {
        return offset;
    }

    public ByteBuffer getPointer() {
        return pointer;
    }

    protected void setFence(Fence fence) {
        this.fence = fence;
    }

    protected Fence getFence() {
        return fence;
    }

}
