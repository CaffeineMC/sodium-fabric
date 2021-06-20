package me.jellysquid.mods.sodium.client.gl.util;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;

public interface StructBuffer {
    void delete();

    void reset();

    int getCount();

    ByteBuffer getBuffer();

    class UnsafeStructBuffer implements StructBuffer {
        protected static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        protected final int stride;

        protected long address;
        protected long writePointer;

        protected int capacity;
        protected int count;

        public UnsafeStructBuffer(int stride, int capacity) {
            this.stride = stride;
            this.capacity = capacity;

            this.address = MemoryUtil.nmemAlloc((long) this.stride * capacity);
        }

        @Override
        public void delete() {
            MemoryUtil.nmemFree(this.address);

            this.address = MemoryUtil.NULL;
        }

        @Override
        public void reset() {
            this.writePointer = this.address;
        }

        @Override
        public int getCount() {
            return (int) (this.writePointer - this.address) / this.stride;
        }

        protected void growBuffer() {
            int capacity = this.capacity * 2;
            long position = this.writePointer - this.address;

            long address = MemoryUtil.nmemRealloc(this.address, (long) this.stride * capacity);

            if (address == MemoryUtil.NULL) {
                throw new RuntimeException("Failed to re-allocate buffer");
            }

            this.address = address;
            this.capacity = capacity;

            this.writePointer = address + position;
        }

        @Override
        public ByteBuffer getBuffer() {
            return MemoryUtil.memByteBuffer(this.address, (int) (this.writePointer - this.address));
        }
    }

    class NioStructBuffer implements StructBuffer {
        protected final int stride;

        protected ByteBuffer buffer;

        public NioStructBuffer(int stride, int capacity) {
            this.stride = stride;
            this.buffer = MemoryUtil.memAlloc(stride * capacity);
        }

        @Override
        public void delete() {
            MemoryUtil.memFree(this.buffer);
        }

        @Override
        public void reset() {
            this.buffer.clear();
        }

        @Override
        public int getCount() {
            return this.buffer.position() / this.stride;
        }

        @Override
        public ByteBuffer getBuffer() {
            return this.buffer.flip();
        }

        protected void growBuffer() {
            this.buffer = MemoryUtil.memRealloc(this.buffer, this.buffer.capacity() * 2);
        }
    }
}
