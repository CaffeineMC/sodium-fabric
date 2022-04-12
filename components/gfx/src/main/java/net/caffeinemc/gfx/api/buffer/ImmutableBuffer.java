package net.caffeinemc.gfx.api.buffer;

import java.util.Set;

public interface ImmutableBuffer extends Buffer {
    Set<ImmutableBufferFlags> flags();
}
