package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import java.nio.ByteBuffer;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexUtil;
import me.jellysquid.mods.sodium.client.util.Norm3b;

public class XHFPModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    public XHFPModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, DefaultModelVertexFormats.MODEL_VERTEX_XHFP);
    }

    private static final int STRIDE = 48;

    int vertexCount = 0;
    float uSum;
    float vSum;

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light) {
        this.writeQuad(x, y, z, color, u, v, light, (short) -1);
    }

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light, short blockId) {
        uSum += u;
        vSum += v;

        this.writeQuadInternal(
            ModelVertexUtil.denormalizeFloatAsShort(x),
            ModelVertexUtil.denormalizeFloatAsShort(y),
            ModelVertexUtil.denormalizeFloatAsShort(z),
            color,
            ModelVertexUtil.denormalizeFloatAsShort(u),
            ModelVertexUtil.denormalizeFloatAsShort(v),
            ModelVertexUtil.encodeLightMapTexCoord(light),
            blockId
        );
    }

    private void writeQuadInternal(short x, short y, short z, int color, short u, short v, int light, short blockId) {
        int i = this.writeOffset;

        vertexCount++;
        // NB: uSum and vSum must already be incremented outside of this function.

        ByteBuffer buffer = this.byteBuffer;
        buffer.putShort(i, x);
        buffer.putShort(i + 2, y);
        buffer.putShort(i + 4, z);
        buffer.putInt(i + 8, color);
        buffer.putShort(i + 12, u);
        buffer.putShort(i + 14, v);
        buffer.putInt(i + 16, light);
        // NB: We don't set midTexCoord and normal here, they will be filled in later.
        // tangent
        buffer.put(i + 24, (byte) 255);
        buffer.put(i + 25, (byte) 0);
        buffer.put(i + 26, (byte) 0);
        buffer.put(i + 27, (byte) 255);
        // block ID
        buffer.putFloat(i + 32, blockId);
        buffer.putFloat(i + 36, (short) 0);
        buffer.putFloat(i + 40, (short) 0);
        buffer.putFloat(i + 44, (short) 0);

        if (vertexCount == 4) {
            short midU = ModelVertexUtil.denormalizeFloatAsShort(uSum * 0.25f);
            short midV = ModelVertexUtil.denormalizeFloatAsShort(vSum * 0.25f);
            int midTexCoord = (midV << 16) | midU;

            buffer.putInt(i + 20, midTexCoord);
            buffer.putInt(i + 20 - STRIDE, midTexCoord);
            buffer.putInt(i + 20 - STRIDE * 2, midTexCoord);
            buffer.putInt(i + 20 - STRIDE * 3, midTexCoord);

            vertexCount = 0;
            uSum = 0;
            vSum = 0;

            // normal computation
            // Implementation based on the algorithm found here:
            // https://github.com/IrisShaders/ShaderDoc/blob/master/vertex-format-extensions.md#surface-normal-vector

            // Capture all of the relevant vertex positions
            float x0 = normalizeShortAsFloat(buffer.getShort(i - STRIDE * 3));
            float y0 = normalizeShortAsFloat(buffer.getShort(i + 2 - STRIDE * 3));
            float z0 = normalizeShortAsFloat(buffer.getShort(i + 4 - STRIDE * 3));

            float x1 = normalizeShortAsFloat(buffer.getShort(i - STRIDE * 2));
            float y1 = normalizeShortAsFloat(buffer.getShort(i + 2 - STRIDE * 2));
            float z1 = normalizeShortAsFloat(buffer.getShort(i + 4 - STRIDE * 2));

            float x2 = normalizeShortAsFloat(buffer.getShort(i - STRIDE));
            float y2 = normalizeShortAsFloat(buffer.getShort(i + 2 - STRIDE));
            float z2 = normalizeShortAsFloat(buffer.getShort(i + 4 - STRIDE));

            float x3 = normalizeShortAsFloat(x);
            float y3 = normalizeShortAsFloat(y);
            float z3 = normalizeShortAsFloat(z);

            // (v2 - v0)
            float cx0 = x2 - x0;
            float cy0 = y2 - y0;
            float cz0 = z2 - z0;

            // (v3 - v1)
            float cx1 = x3 - x1;
            float cy1 = y3 - y1;
            float cz1 = z3 - z1;

            // (v2 - v0) × (v3 - v1)
            // Compute the determinant of the following matrix to get the cross product
            //  i   j   k
            // cx0 cy0 cz0
            // cx1 cy1 cz1

            float nx = cy0 * cz1 - cz0 * cy1;
            float ny = -(cx0 * cz1 - cz0 * cx1);
            float nz = cx0 * cy1 - cy0 * cx1;

            // squared length of the un-normalized normal vector
            float nlengthSquared = nx * nx + ny * ny + nz * nz;

            // get the actual length using square root, then get the inverse
            float ncoeff = rsqrt(nlengthSquared);

            // Normalize the normal vector
            nx *= ncoeff;
            ny *= ncoeff;
            nz *= ncoeff;

            // Pack the normal vector into a 32-bit integer
            int normal = Norm3b.pack(nx, ny, nz);

            buffer.putInt(i + 28, normal);
            buffer.putInt(i + 28 - STRIDE, normal);
            buffer.putInt(i + 28 - STRIDE * 2, normal);
            buffer.putInt(i + 28 - STRIDE * 3, normal);
        }


        /*
        .addElement(ChunkMeshAttribute.MID_TEX_COORD, 20, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.TANGENT, 24, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.NORMAL, 28, GlVertexAttributeFormat.UNSIGNED_BYTE, 3, true)
            .addElement(ChunkMeshAttribute.BLOCK_ID, 32, GlVertexAttributeFormat.UNSIGNED_INT, 1, false)
         */

        this.advance();
    }

    private static float normalizeShortAsFloat(short value) {
        return value * (1.0f / 65535.0f);
    }

    private static float rsqrt(float value) {
        if (value == 0.0f) {
            // You heard it here first, folks: 1 divided by 0 equals 1
            // In actuality, this is a workaround for normalizing a zero length vector (leaving it as zero length)
            return 1.0f;
        } else {
            return (float) (1.0 / Math.sqrt(value));
        }
    }
}
