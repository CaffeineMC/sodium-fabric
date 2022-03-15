package net.caffeinemc.sodium.interop.vanilla.vertex.formats.screen.writer;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferWriterNio;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.screen.BasicScreenQuadVertexSink;

import java.nio.ByteBuffer;

public class BasicScreenQuadVertexBufferWriterNio extends VertexBufferWriterNio implements BasicScreenQuadVertexSink {
    public BasicScreenQuadVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexFormats.BASIC_SCREEN_QUADS);
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
