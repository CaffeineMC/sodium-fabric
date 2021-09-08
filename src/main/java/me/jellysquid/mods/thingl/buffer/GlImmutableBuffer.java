package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.util.EnumBitField;

public class GlImmutableBuffer extends GlBuffer {
    private final EnumBitField<GlBufferStorageFlags> flags;

    public GlImmutableBuffer(RenderDeviceImpl device, EnumBitField<GlBufferStorageFlags> flags) {
        super(device);

        this.flags = flags;
    }

    public EnumBitField<GlBufferStorageFlags> getFlags() {
        return this.flags;
    }
}
