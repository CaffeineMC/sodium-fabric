package net.caffeinemc.gfx.opengl.buffer;

import net.caffeinemc.gfx.api.buffer.DynamicBuffer;
import net.caffeinemc.gfx.api.buffer.DynamicBufferFlags;

import java.util.Collections;
import java.util.Set;

public class GlDynamicBuffer extends GlBuffer implements DynamicBuffer {
    private final Set<DynamicBufferFlags> flags;

    public GlDynamicBuffer(int handle, long capacity, Set<DynamicBufferFlags> flags) {
        super(handle, capacity);
        this.flags = Collections.unmodifiableSet(flags);
    }

    @Override
    public Set<DynamicBufferFlags> flags() {
        return this.flags;
    }
}
