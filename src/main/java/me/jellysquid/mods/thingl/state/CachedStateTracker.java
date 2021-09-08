package me.jellysquid.mods.thingl.state;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.buffer.GlBufferTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;

import java.util.Arrays;

public class CachedStateTracker implements StateTracker {
    protected final int[] bufferState = new int[GlBufferTarget.COUNT];
    protected int vertexArrayState;
    protected int programState;

    @Override
    public void notifyVertexArrayDeleted(GlVertexArray vertexArray) {
        if (this.vertexArrayState == vertexArray.handle()) {
            this.vertexArrayState = UNASSIGNED_HANDLE;
        }
    }

    @Override
    public void notifyBufferDeleted(GlBuffer buffer) {
        for (GlBufferTarget target : GlBufferTarget.VALUES) {
            if (this.bufferState[target.ordinal()] == buffer.handle()) {
                this.bufferState[target.ordinal()] = UNASSIGNED_HANDLE;
            }
        }
    }

    @Override
    public boolean makeBufferActive(GlBufferTarget target, int buffer) {
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
