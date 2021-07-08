package me.jellysquid.mods.sodium.client.gl;

import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

/**
 * An abstract object used to represent objects in OpenGL code safely. This class hides the direct handle to a OpenGL
 * object, requiring that it first be checked by all callers to prevent null pointer de-referencing. However, this will
 * not stop code from cloning the handle and trying to use it after it has been deleted and as such should not be
 * relied on too heavily.
 */
public abstract class GlObject {
    private static final int INVALID_HANDLE = Integer.MIN_VALUE;

    private int handle = INVALID_HANDLE;

    protected GlObject() {

    }

    protected final void setHandle(int handle) {
        this.handle = handle;
    }

    public final int handle() {
        this.checkHandle();

        return this.handle;
    }

    protected final void checkHandle() {
        if (!this.isHandleValid()) {
            throw new IllegalStateException("Handle is not valid");
        }
    }

    protected final boolean isHandleValid() {
        return this.handle != INVALID_HANDLE;
    }

    public final void invalidateHandle() {
        this.handle = INVALID_HANDLE;
    }
}
