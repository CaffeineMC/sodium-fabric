package me.jellysquid.mods.sodium.client.gl.state;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.Arrays;

public class GlStateTracker {
    private static final int UNASSIGNED_HANDLE = -1;

    private final int[] bufferState = new int[GlBufferTarget.COUNT];
    private final int[] bufferRestoreState = new int[GlBufferTarget.COUNT];

    private int vertexArrayState;
    private int vertexArrayRestoreState;

    public GlStateTracker() {
        this.reset();
    }

    public void notifyVertexArrayDeleted(GlVertexArray vertexArray) {
        if (this.vertexArrayState == vertexArray.handle()) {
            this.vertexArrayState = UNASSIGNED_HANDLE;
        }
    }

    public void notifyBufferDeleted(GlBuffer buffer) {
        for (GlBufferTarget target : GlBufferTarget.VALUES) {
            if (this.bufferState[target.ordinal()] == buffer.handle()) {
                this.bufferState[target.ordinal()] = UNASSIGNED_HANDLE;
            }
        }
    }

    public boolean makeBufferActive(GlBufferTarget target, GlBuffer buffer) {
        boolean changed = this.bufferState[target.ordinal()] != buffer.handle();

        if (changed) {
            this.bufferState[target.ordinal()] = buffer.handle();
        }

        return changed;
    }

    public boolean makeVertexArrayActive(GlVertexArray array) {
        if (this.vertexArrayRestoreState == UNASSIGNED_HANDLE) {
            this.vertexArrayRestoreState = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        }

        int handle = array == null ? GlVertexArray.NULL_ARRAY_ID : array.handle();
        boolean changed = this.vertexArrayState != handle;

        if (changed) {
            this.vertexArrayState = handle;

            Arrays.fill(this.bufferState, UNASSIGNED_HANDLE);
        }

        return changed;
    }

    public void pop() {
        if (this.vertexArrayRestoreState != UNASSIGNED_HANDLE && this.vertexArrayState != this.vertexArrayRestoreState) {
            GL30C.glBindVertexArray(this.vertexArrayRestoreState);
        }

        for (int i = 0; i < GlBufferTarget.COUNT; i++) {
            if (this.bufferRestoreState[i] != UNASSIGNED_HANDLE && this.bufferRestoreState[i] != this.bufferState[i]) {
                GL20C.glBindBuffer(GlBufferTarget.VALUES[i].getTargetParameter(), this.bufferRestoreState[i]);
            }
        }

        this.reset();
    }

    public void reset() {
        Arrays.fill(this.bufferState, UNASSIGNED_HANDLE);
        Arrays.fill(this.bufferRestoreState, UNASSIGNED_HANDLE);

        this.vertexArrayState = UNASSIGNED_HANDLE;
        this.vertexArrayRestoreState = UNASSIGNED_HANDLE;
    }

    public void push() {
        for (GlBufferTarget target : GlBufferTarget.VALUES) {
            this.bufferRestoreState[target.ordinal()] = GL11C.glGetInteger(target.getBindingParameter());
        }
    }
}
