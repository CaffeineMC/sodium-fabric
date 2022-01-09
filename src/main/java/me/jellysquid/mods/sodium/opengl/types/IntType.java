package me.jellysquid.mods.sodium.opengl.types;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.lwjgl.opengl.GL32C;

public enum IntType {
    UNSIGNED_BYTE(GL32C.GL_UNSIGNED_BYTE, 1),
    UNSIGNED_SHORT(GL32C.GL_UNSIGNED_SHORT, 2),
    UNSIGNED_INT(GL32C.GL_UNSIGNED_INT, 4);

    public static final Int2ReferenceMap<IntType> BY_FORMAT;

    static {
        var map = new Int2ReferenceOpenHashMap<IntType>();

        for (var type : IntType.values()) {
            map.put(type.id, type);
        }

        BY_FORMAT = Int2ReferenceMaps.unmodifiable(map);
    }

    private final int id;
    private final int size;

    IntType(int id, int size) {
        this.id = id;
        this.size = size;
    }

    public int getFormatId() {
        return this.id;
    }

    public int getSize() {
        return this.size;
    }

    public static final IntType[] VALUES = IntType.values();
}
