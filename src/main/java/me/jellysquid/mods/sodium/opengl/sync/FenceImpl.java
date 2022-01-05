package me.jellysquid.mods.sodium.opengl.sync;

import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public class FenceImpl implements Fence {
    private final long id;
    private boolean signaled;

    public FenceImpl(long id) {
        this.id = id;
    }

    /**
     * Polls the signaled state of the fence.
     * @return True if the fence has been signaled, otherwise false
     */
    @Override
    public boolean poll() {
        if (!this.signaled) {
            this.poll0();
        }

        return this.signaled;
    }

    private void poll0() {
        int result;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer values = stack.callocInt(1);
            GL32C.nglGetSynciv(this.id, GL32C.GL_SYNC_STATUS, 1, MemoryUtil.NULL, MemoryUtil.memAddress(values));

            result = values.get(0);
        }

        if (result == GL32C.GL_SIGNALED) {
            GL32C.glDeleteSync(this.id);
            this.signaled = true;
        }
    }

    /**
     * Performs a blocking wait until the fence to be signaled.
     */
    @Override
    public void sync() {
        if (this.signaled) {
            return;
        }

        GL32C.glClientWaitSync(this.id, 0, GL32C.GL_TIMEOUT_IGNORED);
        GL32C.glDeleteSync(this.id);

        this.signaled = true;
    }
}
