package me.jellysquid.mods.sodium.interop.vanilla.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import me.jellysquid.mods.sodium.render.vertex.VertexSink;
import me.jellysquid.mods.sodium.render.vertex.type.BufferVertexFormat;
import me.jellysquid.mods.sodium.render.vertex.type.BufferVertexType;

public interface VanillaVertexType<T extends VertexSink> extends BufferVertexType<T> {
    default BufferVertexFormat getBufferVertexFormat() {
        return BufferVertexFormat.from(this.getVertexFormat());
    }

    VertexFormat getVertexFormat();
}
