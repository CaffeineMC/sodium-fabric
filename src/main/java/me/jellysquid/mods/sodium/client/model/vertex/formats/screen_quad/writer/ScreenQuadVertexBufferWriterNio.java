package me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.ScreenQuadVertexSink;

import java.nio.ByteBuffer;

public class ScreenQuadVertexBufferWriterNio extends VertexBufferWriterNio implements ScreenQuadVertexSink {
    public ScreenQuadVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexTypes.SCREEN_QUADS);
    }

    @Override
    public void writeQuad(float x, float y, float z, int color) {
        int i = this.writeOffset;

        ByteBuffer buf = this.byteBuffer;
        buf.putFloat(i, x);
        buf.putFloat(i + 4, y);
        buf.putFloat(i + 8, z);
        buf.putInt(i + 12, color);

        this.advance();
    }
}
