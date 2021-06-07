package me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import net.minecraft.util.math.MathHelper;

import java.nio.ByteBuffer;

public class LineVertexBufferWriterNio extends VertexBufferWriterNio implements LineVertexSink {
    public LineVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexTypes.LINES);
    }

    @Override
    public void vertexLine(
            float x, float y, float z,
            int color,
            float normalX, float normalY, float normalZ) {

        for (int r = 0; r < 2; r++) {
            int i = this.writeOffset;

            ByteBuffer buffer = this.byteBuffer;
            buffer.putFloat(i, x);
            buffer.putFloat(i + 4, y);
            buffer.putFloat(i + 8, z);
            buffer.putInt(i + 12, color);
            buffer.put(i + 16, packByte(normalX));
            buffer.put(i + 17, packByte(normalY));
            buffer.put(i + 18, packByte(normalZ));

            this.advance();
        }
    }

    private static byte packByte(float f) {
        return (byte)((int)(MathHelper.clamp(f, -1.0F, 1.0F) * 127.0F) & 255);
    }
}
