package me.jellysquid.mods.thingl.state;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.buffer.GlBufferTarget;

public interface StateTracker {
    int UNASSIGNED_HANDLE = Integer.MIN_VALUE;

    void notifyVertexArrayDeleted(GlVertexArray vertexArray);

    void notifyBufferDeleted(GlBuffer buffer);

    boolean makeBufferActive(GlBufferTarget target, int buffer);

    boolean makeVertexArrayActive(int array);

    boolean makeProgramActive(int program);
}
