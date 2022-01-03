package me.jellysquid.mods.sodium.opengl.types;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.lwjgl.opengl.GL20C;

public enum PrimitiveType {
    TRIANGLES(GL20C.GL_TRIANGLES),
    TRIANGLE_STRIP(GL20C.GL_TRIANGLE_STRIP),
    TRIANGLE_FAN(GL20C.GL_TRIANGLE_FAN),
    LINES(GL20C.GL_LINES),
    LINE_STRIP(GL20C.GL_LINE_STRIP);

    public static final Int2ReferenceMap<PrimitiveType> BY_FORMAT;

    static {
        var map = new Int2ReferenceOpenHashMap<PrimitiveType>();

        for (var type : PrimitiveType.values()) {
            map.put(type.id, type);
        }

        BY_FORMAT = Int2ReferenceMaps.unmodifiable(map);
    }

    private final int id;

    PrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
