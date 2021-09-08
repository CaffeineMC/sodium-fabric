package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.util.EnumBitField;

public class ImmutableBufferImpl extends BufferImpl implements ImmutableBuffer {
    private final EnumBitField<BufferStorageFlags> flags;

    public ImmutableBufferImpl(RenderDeviceImpl device, EnumBitField<BufferStorageFlags> flags) {
        super(device);

        this.flags = flags;
    }

    public EnumBitField<BufferStorageFlags> getFlags() {
        return this.flags;
    }
}
