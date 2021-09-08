package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL20C;

public abstract class BufferImpl extends GlObject implements Buffer {
    private BufferMappingImpl activeMapping;

    protected BufferImpl(RenderDeviceImpl device) {
        super(device);

        this.setHandle(GL20C.glGenBuffers());
    }

    public BufferMappingImpl getActiveMapping() {
        return this.activeMapping;
    }

    public void setActiveMapping(BufferMappingImpl mapping) {
        this.activeMapping = mapping;
    }

    public void bind(BufferTarget target) {
        var tracker = this.device.getStateTracker();
        var handle = this.handle();

        if (tracker.makeBufferActive(target, handle)) {
            GL20C.glBindBuffer(target.getTargetParameter(), handle);
        }
    }

    @Override
    public int getGlId() {
        return this.handle();
    }
}
