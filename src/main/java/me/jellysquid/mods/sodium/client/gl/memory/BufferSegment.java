package me.jellysquid.mods.sodium.client.gl.memory;

public class BufferSegment {
    private final BufferBlock block;
    private final int start;
    private final int len;

    BufferSegment(BufferBlock block, int start, int len) {
        this.block = block;
        this.start = start;
        this.len = len;
    }

    public int getStart() {
        return this.start;
    }

    public int getLength() {
        return this.len;
    }

    public BufferBlock getBlock() {
        return this.block;
    }

    public void delete() {
        this.block.free(this);
    }
}
