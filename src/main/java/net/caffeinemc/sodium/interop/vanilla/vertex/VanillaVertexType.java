package net.caffeinemc.sodium.interop.vanilla.vertex;

import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;
import net.caffeinemc.sodium.render.vertex.type.BufferVertexType;
import net.minecraft.client.render.VertexFormat;

public interface VanillaVertexType<T extends VertexSink> extends BufferVertexType<T> {
    default BufferVertexFormat getBufferVertexFormat() {
        return (BufferVertexFormat) this.getVertexFormat();
    }

    VertexFormat getVertexFormat();
}
