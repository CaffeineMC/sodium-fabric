package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.functions.DirectStateAccessFunctions;
import me.jellysquid.mods.thingl.util.EnumBitField;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL32C;

import java.nio.ByteBuffer;

public abstract class BufferImpl extends GlObject implements Buffer {
    private BufferMappingImpl activeMapping;
    protected final boolean dsa;

    protected BufferImpl(RenderDeviceImpl device, boolean dsa) {
        super(device);

        int handle;

        if (dsa) {
            handle = device.getDeviceFunctions()
                    .getDirectStateAccessFunctions()
                    .createBuffers();
        } else {
            handle = GL20C.glGenBuffers();
        }

        this.setHandle(handle);
        this.dsa = dsa;
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

    public BufferMapping createMapping(long offset, long length, EnumBitField<BufferMapFlags> flags) {
        ByteBuffer buf;

        if (this.dsa) {
            buf = this.device.getDeviceFunctions()
                    .getDirectStateAccessFunctions()
                    .mapNamedBufferRange(this.handle(), offset, length, flags.getBitField());
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER);
            buf = GL32C.glMapBufferRange(BufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, length, flags.getBitField());
        }

        if (buf == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        BufferMappingImpl mapping = new BufferMappingImpl(this, buf);

        this.setActiveMapping(mapping);

        return mapping;
    }

    public void unmap() {
        if (this.dsa) {
            this.device.getDeviceFunctions()
                    .getDirectStateAccessFunctions()
                    .unmapNamedBuffer(this.handle());
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER);
            GL32C.glUnmapBuffer(BufferTarget.ARRAY_BUFFER.getTargetParameter());
        }

        this.setActiveMapping(null);
    }
}
