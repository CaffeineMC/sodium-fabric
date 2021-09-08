package me.jellysquid.mods.thingl.state;

import me.jellysquid.mods.thingl.array.VertexArrayImpl;
import me.jellysquid.mods.thingl.buffer.BufferImpl;
import me.jellysquid.mods.thingl.buffer.BufferTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.util.Arrays;

public class RecordingStateTracker extends CachedStateTracker {
    private final int[] bufferRestoreState = new int[BufferTarget.COUNT];
    private int vertexArrayRestoreState;
    private int programRestoreState;

    private boolean active;

    @Override
    public void notifyVertexArrayDeleted(VertexArrayImpl vertexArray) {
        super.notifyVertexArrayDeleted(vertexArray);
    }

    @Override
    public void notifyBufferDeleted(BufferImpl buffer) {
        super.notifyBufferDeleted(buffer);
    }

    @Override
    public boolean makeBufferActive(BufferTarget target, int buffer) {
        if (this.bufferRestoreState[target.ordinal()] == UNASSIGNED_HANDLE) {
            this.bufferRestoreState[target.ordinal()] = GL11C.glGetInteger(target.getBindingParameter());
        }

        return super.makeBufferActive(target, buffer);
    }

    @Override
    public boolean makeVertexArrayActive(int array) {
        if (this.vertexArrayRestoreState == UNASSIGNED_HANDLE) {
            this.vertexArrayRestoreState = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        }

        return super.makeVertexArrayActive(array);
    }

    @Override
    public boolean makeProgramActive(int program) {
        if (this.programRestoreState == UNASSIGNED_HANDLE) {
            this.programRestoreState = GL11C.glGetInteger(GL30C.GL_CURRENT_PROGRAM);
        }

        return super.makeProgramActive(program);
    }

    public void pop() {
        if (!this.active) {
            throw new IllegalStateException("Tried to pop state but state tracker is not enabled");
        }

        if (this.vertexArrayRestoreState != UNASSIGNED_HANDLE && this.vertexArrayState != this.vertexArrayRestoreState) {
            GL30C.glBindVertexArray(this.vertexArrayRestoreState);
        }

        for (int i = 0; i < BufferTarget.COUNT; i++) {
            if (this.bufferRestoreState[i] != UNASSIGNED_HANDLE && this.bufferRestoreState[i] != this.bufferState[i]) {
                GL20C.glBindBuffer(BufferTarget.VALUES[i].getTargetParameter(), this.bufferRestoreState[i]);
            }
        }

        if (this.programRestoreState != UNASSIGNED_HANDLE && this.programState != this.programRestoreState) {
            GL20C.glUseProgram(this.programRestoreState);
        }

        this.reset();
        this.active = false;
    }

    public void reset() {
        Arrays.fill(this.bufferState, UNASSIGNED_HANDLE);
        Arrays.fill(this.bufferRestoreState, UNASSIGNED_HANDLE);

        this.vertexArrayState = UNASSIGNED_HANDLE;
        this.vertexArrayRestoreState = UNASSIGNED_HANDLE;
    }

    public void push() {
        if (this.active) {
            throw new IllegalStateException("Tried to push state twice (re-entrance is not allowed)");
        }

        this.active = true;
    }

    private void checkState() {
        if (!this.active) {
            throw new RuntimeException("State tracking not enabled");
        }
    }
}
