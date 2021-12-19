package me.jellysquid.mods.sodium.render.entity.buffer;

import me.jellysquid.mods.thingl.buffer.BufferMapFlags;
import me.jellysquid.mods.thingl.buffer.BufferStorageFlags;
import me.jellysquid.mods.thingl.util.EnumBitField;

public class RenderInstancedStorage {
    private static final EnumBitField<BufferStorageFlags> STORAGE_FLAGS =
            me.jellysquid.mods.thingl.util.EnumBitField.of(BufferStorageFlags.PERSISTENT, BufferStorageFlags.MAP_WRITE); // BufferStorageFlags.CLIENT_STORAGE
    private static final EnumBitField<BufferMapFlags> MAP_FLAGS =
            EnumBitField.of(BufferMapFlags.PERSISTENT, BufferMapFlags.WRITE, BufferMapFlags.EXPLICIT_FLUSH);

    private static final int BUFFER_SECTIONS = 3;

    private static final long PART_BUFFER_SIZE = 9175040L; // 8.75 MiB
    private static final long MODEL_BUFFER_SIZE = 524288L; // 512 KiB
    private static final long TRANSLUCENT_EBO_SIZE = 1048576L; // 1 MiB



}
