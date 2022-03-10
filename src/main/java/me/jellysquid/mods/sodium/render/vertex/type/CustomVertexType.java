package me.jellysquid.mods.sodium.render.vertex.type;

import net.caffeinemc.gfx.api.array.attribute.VertexFormat;
import me.jellysquid.mods.sodium.render.vertex.VertexSink;
import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;

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
