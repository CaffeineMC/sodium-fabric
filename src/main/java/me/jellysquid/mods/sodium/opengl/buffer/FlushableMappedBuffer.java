package me.jellysquid.mods.sodium.opengl.buffer;

public interface FlushableMappedBuffer extends MappedBuffer {
    default void flush() {
        flush(0, this.getCapacity());
    }

    void flush(long offset, long length);
}
