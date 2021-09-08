package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.util.EnumBitField;

public class ImmutableBufferImpl extends BufferImpl implements ImmutableBuffer {
    protected final EnumBitField<BufferStorageFlags> flags;

    public ImmutableBufferImpl(RenderDeviceImpl device, EnumBitField<BufferStorageFlags> flags, boolean dsa) {
        super(device, dsa);

        this.flags = flags;
    }

    public EnumBitField<BufferStorageFlags> getFlags() {
        return this.flags;
    }

    public void createBufferStorage(long bufferSize) {
        if (this.dsa) {
            this.device.getDeviceFunctions()
                    .getDirectStateAccessFunctions()
                    .createNamedBufferStorage(this.handle(), bufferSize, this.flags);
        } else {
            this.device.getDeviceFunctions()
                    .getBufferStorageFunctions()
                    .createBufferStorage(BufferTarget.ARRAY_BUFFER, bufferSize, this.flags);
        }
    }
}
