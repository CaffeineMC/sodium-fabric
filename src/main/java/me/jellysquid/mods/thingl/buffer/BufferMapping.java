package me.jellysquid.mods.thingl.buffer;

import me.jellysquid.mods.thingl.util.EnumBitField;

import java.nio.ByteBuffer;

public interface BufferMapping {

    /**
     * The user of this method is responsible for the usage of this pointer based off of the flags.
     *
     * @return a pointer to the mapped buffer.
     */
    ByteBuffer getPointer();

    EnumBitField<BufferMapFlags> getFlags();
}
