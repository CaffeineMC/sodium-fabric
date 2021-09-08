package me.jellysquid.mods.thingl.state;

import me.jellysquid.mods.thingl.array.VertexArrayImpl;
import me.jellysquid.mods.thingl.buffer.BufferImpl;
import me.jellysquid.mods.thingl.buffer.BufferTarget;

public class CachedStateTracker implements StateTracker {
    protected final int[] bufferState = new int[BufferTarget.COUNT];
    protected int vertexArrayState;
    protected int programState;

    @Override
    public void notifyVertexArrayDeleted(VertexArrayImpl vertexArray) {
        if (this.vertexArrayState == vertexArray.handle()) {
            this.vertexArrayState = UNASSIGNED_HANDLE;
        }
    }

    @Override
    public void notifyBufferDeleted(BufferImpl buffer) {
        for (BufferTarget target : BufferTarget.VALUES) {
            if (this.bufferState[target.ordinal()] == buffer.handle()) {
                this.bufferState[target.ordinal()] = UNASSIGNED_HANDLE;
            }
        }
    }

    @Override
    public boolean makeBufferActive(BufferTarget target, int buffer) {
        boolean changed = this.bufferState[target.ordinal()] != buffer;

        if (changed) {
            this.bufferState[target.ordinal()] = buffer;
        }

        return changed;
    }

    @Override
    public boolean makeVertexArrayActive(int array) {
        boolean changed = this.vertexArrayState != array;

        if (changed) {
            this.vertexArrayState = array;
        }

        return changed;
    }

    @Override
    public boolean makeProgramActive(int program) {
        boolean changed = this.programState != program;

        if (changed) {
            this.programState = program;
        }

        return changed;
    }
}
