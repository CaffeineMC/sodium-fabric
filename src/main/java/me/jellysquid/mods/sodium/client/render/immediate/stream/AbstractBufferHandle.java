package me.jellysquid.mods.sodium.client.render.immediate.stream;

public abstract class AbstractBufferHandle implements BufferHandle {
    private final int offset;
    private final int stride;
    private final int length;

    AbstractBufferHandle(int offset, int stride, int length) {
        this.offset = offset;
        this.stride = stride;
        this.length = length;
    }

    @Override
    public int getLength() {
        return this.length;
    }

    @Override
    public int getElementCount() {
        return this.length / this.stride;
    }

    @Override
    public int getElementOffset() {
        return this.offset / this.stride;
    }
}
