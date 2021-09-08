package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;

/**
 * A mutable buffer type which is supported with OpenGL 1.5+. The buffer's storage can be reallocated at any time
 * without needing to re-create the buffer itself.
 */
public class GlMutableBuffer extends GlBuffer {
    private long size = 0L;

    public GlMutableBuffer(RenderDeviceImpl device) {
        super(device);
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return this.size;
    }
}
