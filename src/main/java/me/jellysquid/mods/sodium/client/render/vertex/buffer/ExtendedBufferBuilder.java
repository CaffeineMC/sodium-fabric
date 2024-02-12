package me.jellysquid.mods.sodium.client.render.vertex.buffer;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

import java.nio.ByteBuffer;

public interface ExtendedBufferBuilder extends VertexBufferWriter {
    ByteBuffer sodium$getBuffer();
    int sodium$getElementOffset();
    void sodium$moveToNextVertex();
    VertexFormatDescription sodium$getFormatDescription();
    boolean sodium$usingFixedColor();
    SodiumBufferBuilder sodium$getDelegate();
}
