package net.caffeinemc.mods.sodium.client.render.vertex.buffer;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

import java.nio.ByteBuffer;

public interface BufferBuilderExtension extends VertexBufferWriter {
    void sodium$duplicateVertex();
}
