package net.caffeinemc.gfx.opengl.sync;

import net.caffeinemc.gfx.api.sync.Fence;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class GlFence implements Fence {
    private final long id;
    private boolean signaled;

    public GlFence() {
        this.id = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

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
            long statusPointer = stack.ncalloc(Integer.BYTES, 0, Integer.BYTES);
            GL32C.nglGetSynciv(this.id, GL32C.GL_SYNC_STATUS, 1, MemoryUtil.NULL, statusPointer);

            result = MemoryUtil.memGetInt(statusPointer);
        }

        if (result == GL32C.GL_SIGNALED) {
            GL32C.glDeleteSync(this.id);
            this.signaled = true;
        }
    }

    @Override
    public void sync(boolean flush) {
        if (this.poll()) {
            return;
        }

        GL32C.glClientWaitSync(this.id, flush ? GL32C.GL_SYNC_FLUSH_COMMANDS_BIT : 0, GL32C.GL_TIMEOUT_IGNORED);
        GL32C.glDeleteSync(this.id);

        this.signaled = true;
    }

    @Override
    public void delete() {
        if (!this.signaled) {
            GL32C.glDeleteSync(this.id);
        }
    }
}
