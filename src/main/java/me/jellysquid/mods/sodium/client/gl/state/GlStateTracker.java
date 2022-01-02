package me.jellysquid.mods.sodium.client.gl.state;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;

import java.util.Arrays;

public class GlStateTracker {
    private static final int UNASSIGNED_HANDLE = -1;

    private final int[] bufferState = new int[GlBufferTarget.COUNT];
    private final int[] bufferRestoreState = new int[GlBufferTarget.COUNT];

    public GlStateTracker() {
        this.reset();
    }

    public void notifyBufferDeleted(GlBuffer buffer) {
        for (GlBufferTarget target : GlBufferTarget.VALUES) {
            if (this.bufferState[target.ordinal()] == buffer.handle()) {
                this.bufferState[target.ordinal()] = UNASSIGNED_HANDLE;
            }
        }
    }

    public boolean makeBufferActive(GlBufferTarget target, int handle) {
        boolean changed = this.bufferState[target.ordinal()] != handle;

        if (changed) {
            this.bufferState[target.ordinal()] = handle;
        }

        return changed;
    }

    public void pop() {
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
    }

    public void push() {
        for (GlBufferTarget target : GlBufferTarget.VALUES) {
            this.bufferRestoreState[target.ordinal()] = GL11C.glGetInteger(target.getBindingParameter());
        }
    }
}
