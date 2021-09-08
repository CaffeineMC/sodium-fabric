package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL20C;

import java.nio.ByteBuffer;

/**
 * A mutable buffer type which is supported with OpenGL 1.5+. The buffer's storage can be reallocated at any time
 * without needing to re-create the buffer itself.
 */
public class MutableBufferImpl extends BufferImpl implements MutableBuffer {
    private long size = 0L;

    public MutableBufferImpl(RenderDeviceImpl device, boolean dsa) {
        super(device, dsa);
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return this.size;
    }

    public void upload(ByteBuffer data, BufferUsage usage) {
        if (this.dsa) {
            this.device.getDeviceFunctions()
                    .getDirectStateAccessFunctions()
                    .namedBufferData(this.handle(), data, usage.getId());
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER);

            GL20C.glBufferData(BufferTarget.ARRAY_BUFFER.getTargetParameter(), data, usage.getId());
        }

        this.setSize(data.remaining());
    }
}
