package me.jellysquid.mods.thingl.sync;

import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class FenceImpl implements Fence {
    private final long id;

    public FenceImpl() {
        this.id = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

        if (this.id == 0) {
            throw new RuntimeException("Failed to create fence sync object");
        }
    }

    @Override
    public boolean isCompleted() {
        int result;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.callocInt(1);
            result = GL32C.glGetSynci(this.id, GL32C.GL_SYNC_STATUS, count);

            if (count.get(0) != 1) {
                throw new RuntimeException("glGetSync returned more than one value");
            }
        }

        return result == GL32C.GL_SIGNALED;
    }

    @Override
    public void sync() {
        this.sync(Long.MAX_VALUE);
    }

    @Override
    public void sync(long timeout) {
        GL32C.glClientWaitSync(this.id, GL32C.GL_SYNC_FLUSH_COMMANDS_BIT, timeout);
    }

    public void delete() {
        GL32C.glDeleteSync(this.id);
    }
}
