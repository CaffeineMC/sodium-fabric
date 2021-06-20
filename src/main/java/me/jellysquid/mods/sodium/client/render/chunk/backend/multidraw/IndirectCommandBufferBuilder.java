package me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw;

import me.jellysquid.mods.sodium.client.gl.util.StructBuffer;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;

public interface IndirectCommandBufferBuilder extends StructBuffer {
    int STRIDE = 20;

    static IndirectCommandBufferBuilder create(int capacity) {
        return UnsafeUtil.isAvailable() ? new UnsafeIndirectCommandBufferBuilder(capacity) :
                new NioIndirectCommandBufferBuilder(capacity);
    }

    void addIndirectDrawCall(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance);

    class UnsafeIndirectCommandBufferBuilder extends StructBuffer.UnsafeStructBuffer implements IndirectCommandBufferBuilder {
        protected UnsafeIndirectCommandBufferBuilder(int capacity) {
            super(IndirectCommandBufferBuilder.STRIDE, capacity);
        }

        @Override
        public void addIndirectDrawCall(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
            if (this.count++ >= this.capacity) {
                this.growBuffer();
            }

            long address = this.writePointer;

            UNSAFE.putInt(address, count);                      // Element Count
            UNSAFE.putInt(address +  4, instanceCount); // Instance Count
            UNSAFE.putInt(address +  8, firstIndex);    // Base Index
            UNSAFE.putInt(address + 12, baseVertex);    // Base Vertex
            UNSAFE.putInt(address + 16, baseInstance);  // Base Instance

            this.writePointer += this.stride;
        }
    }

    class NioIndirectCommandBufferBuilder extends StructBuffer.NioStructBuffer implements IndirectCommandBufferBuilder {
        public NioIndirectCommandBufferBuilder(int capacity) {
            super(IndirectCommandBufferBuilder.STRIDE, capacity);
        }

        @Override
        public void addIndirectDrawCall(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
            if (this.buffer.remaining() < this.stride) {
                this.growBuffer();
            }

            int offset = this.buffer.position();

            this.buffer.putInt(offset, count);
            this.buffer.putInt(offset + 4, instanceCount);
            this.buffer.putInt(offset + 8, firstIndex);
            this.buffer.putInt(offset + 12, baseVertex);
            this.buffer.putInt(offset + 16, baseInstance);

            this.buffer.position(offset + this.stride);
        }
    }
}
