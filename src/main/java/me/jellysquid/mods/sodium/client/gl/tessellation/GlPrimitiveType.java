package me.jellysquid.mods.sodium.client.gl.tessellation;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.lwjgl.opengl.GL20C;

public enum GlPrimitiveType {
    TRIANGLES(GL20C.GL_TRIANGLES),
    TRIANGLE_STRIP(GL20C.GL_TRIANGLE_STRIP),
    TRIANGLE_FAN(GL20C.GL_TRIANGLE_FAN),
    LINES(GL20C.GL_LINES),
    LINE_STRIP(GL20C.GL_LINE_STRIP);

    public static final Int2ReferenceMap<GlPrimitiveType> BY_FORMAT;

    static {
        var map = new Int2ReferenceOpenHashMap<GlPrimitiveType>();

        for (var type : GlPrimitiveType.values()) {
            map.put(type.id, type);
        }

        BY_FORMAT = Int2ReferenceMaps.unmodifiable(map);
    }

    private final int id;

    GlPrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
