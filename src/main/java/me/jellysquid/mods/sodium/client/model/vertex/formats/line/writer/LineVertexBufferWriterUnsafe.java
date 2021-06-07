package me.jellysquid.mods.sodium.client.model.vertex.formats.line.writer;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import net.minecraft.util.math.MathHelper;

public class LineVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements LineVertexSink {
    public LineVertexBufferWriterUnsafe(VertexBufferView backingBuffer) {
        super(backingBuffer, VanillaVertexTypes.LINES);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void vertexLine(
            float x, float y, float z,
            int color,
            float normalX, float normalY, float normalZ) {

        for (int r = 0; r < 2; r++) {
            long i = this.writePointer;

            UNSAFE.putFloat(i, x);
            UNSAFE.putFloat(i + 4, y);
            UNSAFE.putFloat(i + 8, z);
            UNSAFE.putInt(i + 12, color);
            UNSAFE.putByte(i + 16, packByte(normalX));
            UNSAFE.putByte(i + 17, packByte(normalY));
            UNSAFE.putByte(i + 18, packByte(normalZ));

            this.advance();
        }
    }


    private static byte packByte(float f) {
        return (byte)((int)(MathHelper.clamp(f, -1.0F, 1.0F) * 127.0F) & 255);
    }
}
