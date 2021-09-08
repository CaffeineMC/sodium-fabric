package me.jellysquid.mods.sodium.model.vertex.type;

import me.jellysquid.mods.thingl.attribute.BufferVertexFormat;
import me.jellysquid.mods.thingl.attribute.VertexFormat;
import me.jellysquid.mods.sodium.model.vertex.VertexSink;

public interface CustomVertexType<T extends VertexSink, A extends Enum<A>> extends BufferVertexType<T> {
    /**
     * @return The {@link VertexFormat} required for blitting (direct writing into buffers)
     */
    VertexFormat<A> getCustomVertexFormat();

    @Override
    default BufferVertexFormat getBufferVertexFormat() {
        return this.getCustomVertexFormat();
    }
}
