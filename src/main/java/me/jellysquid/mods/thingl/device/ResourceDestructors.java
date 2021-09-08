package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.array.GlVertexArray;
import me.jellysquid.mods.thingl.buffer.GlBuffer;
import me.jellysquid.mods.thingl.tessellation.GlTessellation;

public interface ResourceDestructors {
    void deleteBuffer(GlBuffer buffer);

    void deleteTessellation(GlTessellation tessellation);

    void deleteVertexArray(GlVertexArray vertexArray);
}
