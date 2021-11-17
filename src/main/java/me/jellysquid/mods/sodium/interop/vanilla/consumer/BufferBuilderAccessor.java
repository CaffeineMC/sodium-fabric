package me.jellysquid.mods.sodium.interop.vanilla.consumer;

import java.nio.ByteBuffer;

public interface BufferBuilderAccessor {

    ByteBuffer getBuffer();

    int getElementOffset();

    int getVertexCount();
}
