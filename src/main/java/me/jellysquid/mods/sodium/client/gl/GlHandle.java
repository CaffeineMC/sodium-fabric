package me.jellysquid.mods.sodium.client.gl;

public class GlHandle {
    private static final int INVALID_HANDLE = Integer.MIN_VALUE;

    private int handle = INVALID_HANDLE;

    protected void setHandle(int handle) {
        this.handle = handle;
    }

    public int handle() {
        this.checkHandle();

        return this.handle;
    }

    protected void checkHandle() {
        if (!this.isHandleValid()) {
            throw new IllegalStateException("Handle is not valid");
        }
    }

    protected boolean isHandleValid() {
        return this.handle != INVALID_HANDLE;
    }

    protected void invalidateHandle() {
        this.handle = INVALID_HANDLE;
    }
}
