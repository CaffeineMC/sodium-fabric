package me.jellysquid.mods.thingl.state;

import me.jellysquid.mods.thingl.array.VertexArrayImpl;
import me.jellysquid.mods.thingl.buffer.BufferImpl;
import me.jellysquid.mods.thingl.buffer.BufferTarget;

public interface StateTracker {
    int UNASSIGNED_HANDLE = Integer.MIN_VALUE;

    void notifyVertexArrayDeleted(VertexArrayImpl vertexArray);

    void notifyBufferDeleted(BufferImpl buffer);

    boolean makeBufferActive(BufferTarget target, int buffer);

    boolean makeVertexArrayActive(int array);

    boolean makeProgramActive(int program);
}
