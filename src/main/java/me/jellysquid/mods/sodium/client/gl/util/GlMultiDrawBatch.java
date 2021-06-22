package me.jellysquid.mods.sodium.client.gl.util;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import sun.misc.Unsafe;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for batching draw calls for vertex data in the same buffer. This internally
 * uses {@link GL20#glMultiDrawArrays(int, IntBuffer, IntBuffer)} and should be compatible on any relevant platform.
 */
public interface GlMultiDrawBatch {
    static GlMultiDrawBatch create(int capacity) {
        return UnsafeUtil.isAvailable() ? new UnsafeMultiDrawBatch(capacity) : new NioMultiDrawBatch(capacity);
    }

    PointerBuffer getPointerBuffer();

    IntBuffer getCountBuffer();

    IntBuffer getBaseVertexBuffer();

    void begin();

    void add(int pointer, int count, int baseVertex);

    void end();

    void delete();

    boolean isEmpty();

    class NioMultiDrawBatch implements GlMultiDrawBatch {
        private final PointerBuffer bufPointer;
        private final IntBuffer bufCount;
        private final IntBuffer bufBaseVertex;

        private int count;

        private NioMultiDrawBatch(int capacity) {
            this.bufPointer = MemoryUtil.memAllocPointer(capacity);
            this.bufCount = MemoryUtil.memAllocInt(capacity);
            this.bufBaseVertex = MemoryUtil.memAllocInt(capacity);
        }

        @Override
        public PointerBuffer getPointerBuffer() {
            return this.bufPointer;
        }

        @Override
        public IntBuffer getCountBuffer() {
            return this.bufCount;
        }

        @Override
        public IntBuffer getBaseVertexBuffer() {
            return this.bufBaseVertex;
        }

        @Override
        public void begin() {
            this.bufPointer.clear();
            this.bufCount.clear();
            this.bufBaseVertex.clear();

            this.count = 0;
        }

        @Override
        public void add(int pointer, int count, int baseVertex) {
            int i = this.count++;

            this.bufPointer.put(i, pointer);
            this.bufCount.put(i, count);
            this.bufBaseVertex.put(i, baseVertex);
        }

        @Override
        public void end() {
            this.bufPointer.limit(this.count);
            this.bufCount.limit(this.count);
            this.bufBaseVertex.limit(this.count);
        }

        @Override
        public void delete() {
            MemoryUtil.memFree(this.bufPointer);
            MemoryUtil.memFree(this.bufCount);
            MemoryUtil.memFree(this.bufBaseVertex);
        }

        @Override
        public boolean isEmpty() {
            return this.count <= 0;
        }
    }

    class UnsafeMultiDrawBatch implements GlMultiDrawBatch {
        private static final Unsafe UNSAFE = UnsafeUtil.instanceNullable();

        private final PointerBuffer bufPointer;
        private final IntBuffer bufCount;
        private final IntBuffer bufBaseVertex;

        private long bufPointerAddr;
        private long bufCountAddr;
        private long bufBaseVertexAddr;

        private final int capacity;

        private int count;

        private UnsafeMultiDrawBatch(int capacity) {
            this.bufPointer = MemoryUtil.memAllocPointer(capacity);
            this.bufCount = MemoryUtil.memAllocInt(capacity);
            this.bufBaseVertex = MemoryUtil.memAllocInt(capacity);
            this.capacity = capacity;

            this.resetPointers();
        }

        private void resetPointers() {
            this.bufPointerAddr = MemoryUtil.memAddress(this.bufPointer);
            this.bufCountAddr = MemoryUtil.memAddress(this.bufCount);
            this.bufBaseVertexAddr = MemoryUtil.memAddress(this.bufBaseVertex);
        }

        @Override
        public PointerBuffer getPointerBuffer() {
            return MemoryUtil.memPointerBuffer(MemoryUtil.memAddress(this.bufPointer), this.count);
        }

        @Override
        public IntBuffer getCountBuffer() {
            return MemoryUtil.memIntBuffer(MemoryUtil.memAddress(this.bufCount), this.count);
        }

        @Override
        public IntBuffer getBaseVertexBuffer() {
            return MemoryUtil.memIntBuffer(MemoryUtil.memAddress(this.bufBaseVertex), this.count);
        }

        @Override
        public void begin() {
            this.count = 0;

            this.resetPointers();
        }

        @Override
        public void add(int pointer, int count, int baseVertex) {
            if (this.count >= this.capacity)  {
                throw new BufferUnderflowException();
            }

            UNSAFE.putLong(this.bufPointerAddr, pointer);
            this.bufPointerAddr += Pointer.POINTER_SIZE;

            UNSAFE.putInt(this.bufCountAddr, count);
            this.bufCountAddr += 4;

            UNSAFE.putInt(this.bufBaseVertexAddr, baseVertex);
            this.bufBaseVertexAddr += 4;

            this.count++;
        }

        @Override
        public void end() {

        }

        @Override
        public void delete() {
            MemoryUtil.memFree(this.bufPointer);
            MemoryUtil.memFree(this.bufCount);
            MemoryUtil.memFree(this.bufBaseVertex);
        }

        @Override
        public boolean isEmpty() {
            return this.count <= 0;
        }
    }
}
