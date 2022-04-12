package net.caffeinemc.gfx.api.buffer;

import java.util.Set;

public interface DynamicBuffer extends Buffer {
    Set<DynamicBufferFlags> flags();
}
