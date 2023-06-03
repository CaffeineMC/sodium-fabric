package me.jellysquid.mods.sodium.core;

import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

class WrappedPointer {
    private long ptr;

    protected WrappedPointer(long handle) {
        Validate.isTrue(handle != MemoryUtil.NULL);

        this.ptr = handle;
    }

    long ptr() {
        this.validate();

        return this.ptr;
    }

    void invalidate() {
        this.validate();

        this.ptr = MemoryUtil.NULL;
    }

    private void validate() {
        if (this.ptr == MemoryUtil.NULL) {
            throw new IllegalStateException("Use after free");
        }
    }
}
