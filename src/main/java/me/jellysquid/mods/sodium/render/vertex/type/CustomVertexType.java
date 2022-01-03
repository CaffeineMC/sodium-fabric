package me.jellysquid.mods.sodium.render.vertex.type;

import me.jellysquid.mods.sodium.opengl.attribute.BufferVertexFormat;
import me.jellysquid.mods.sodium.opengl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.render.vertex.VertexSink;

public interface CustomVertexType<T extends VertexSink, A extends Enum<A>> extends BufferVertexType<T> {
    /**
     * @return The {@link GlVertexFormat} required for blitting (direct writing into buffers)
     */
    GlVertexFormat<A> getCustomVertexFormat();

    @Override
    default BufferVertexFormat getBufferVertexFormat() {
        return this.getCustomVertexFormat();
    }
}
