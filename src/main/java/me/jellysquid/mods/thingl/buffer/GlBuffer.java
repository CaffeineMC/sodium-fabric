package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL20C;

public abstract class GlBuffer extends GlObject {
    private GlBufferMapping activeMapping;

    protected GlBuffer(RenderDeviceImpl device) {
        super(device);

        this.setHandle(GL20C.glGenBuffers());
    }

    public GlBufferMapping getActiveMapping() {
        return this.activeMapping;
    }

    public void setActiveMapping(GlBufferMapping mapping) {
        this.activeMapping = mapping;
    }

    public void bind(GlBufferTarget target) {
        var tracker = this.device.getStateTracker();
        var handle = this.handle();

        if (tracker.makeBufferActive(target, handle)) {
            GL20C.glBindBuffer(target.getTargetParameter(), handle);
        }
    }
}
