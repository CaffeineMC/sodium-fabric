package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import java.nio.ByteBuffer;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.MaterialIdHolder;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexUtil;
import me.jellysquid.mods.sodium.client.util.Norm3b;

import net.minecraft.client.util.math.Vector3f;

public class XHFPModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    private MaterialIdHolder idHolder;

    public XHFPModelVertexBufferWriterNio(VertexBufferView backingBuffer, MaterialIdHolder idHolder) {
        super(backingBuffer, DefaultModelVertexFormats.MODEL_VERTEX_XHFP);

        this.idHolder = idHolder;
    }

    private static final int STRIDE = 48;

    int vertexCount = 0;
    float uSum;
    float vSum;

    private QuadView currentQuad = new QuadView();
    private Vector3f normal = new Vector3f();

    @Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light) {
        uSum += u;
        vSum += v;

        short materialId = idHolder.id;

        this.writeQuadInternal(
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(x),
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(y),
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(z),
                color,
                ModelVertexUtil.denormalizeVertexTextureFloatAsShort(u),
                ModelVertexUtil.denormalizeVertexTextureFloatAsShort(v),
                ModelVertexUtil.encodeLightMapTexCoord(light),
                materialId
        );
    }

    private void writeQuadInternal(short x, short y, short z, int color, short u, short v, int light, short materialId) {
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
        // NB: We don't set midTexCoord, normal, and tangent here, they will be filled in later.
        // block ID
        buffer.putFloat(i + 32, materialId);
        buffer.putFloat(i + 36, (short) 0);
        buffer.putFloat(i + 40, (short) 0);
        buffer.putFloat(i + 44, (short) 0);

        if (vertexCount == 4) {
            // TODO: Consider applying similar vertex coordinate transformations as the normal HFP texture coordinates
            short midU = (short)(65536.0F * (uSum * 0.25f));
            short midV = (short)(65536.0F * (vSum * 0.25f));
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

            currentQuad.buffer = this.byteBuffer;
            currentQuad.writeOffset = this.writeOffset;
            NormalHelper.computeFaceNormal(normal, currentQuad);
            int packedNormal = NormalHelper.packNormal(normal, 0.0f);

            buffer.putInt(i + 28, packedNormal);
            buffer.putInt(i + 28 - STRIDE, packedNormal);
            buffer.putInt(i + 28 - STRIDE * 2, packedNormal);
            buffer.putInt(i + 28 - STRIDE * 3, packedNormal);

            // Capture all of the relevant vertex positions
            float x0 = normalizeVertexPositionShortAsFloat(buffer.getShort(i - STRIDE * 3));
            float y0 = normalizeVertexPositionShortAsFloat(buffer.getShort(i + 2 - STRIDE * 3));
            float z0 = normalizeVertexPositionShortAsFloat(buffer.getShort(i + 4 - STRIDE * 3));

            float x1 = normalizeVertexPositionShortAsFloat(buffer.getShort(i - STRIDE * 2));
            float y1 = normalizeVertexPositionShortAsFloat(buffer.getShort(i + 2 - STRIDE * 2));
            float z1 = normalizeVertexPositionShortAsFloat(buffer.getShort(i + 4 - STRIDE * 2));

            float x2 = normalizeVertexPositionShortAsFloat(buffer.getShort(i - STRIDE));
            float y2 = normalizeVertexPositionShortAsFloat(buffer.getShort(i + 2 - STRIDE));
            float z2 = normalizeVertexPositionShortAsFloat(buffer.getShort(i + 4 - STRIDE));

            float edge1x = x1 - x0;
            float edge1y = y1 - y0;
            float edge1z = z1 - z0;

            float edge2x = x2 - x0;
            float edge2y = y2 - y0;
            float edge2z = z2 - z0;

            float u0 = normalizeVertexTextureShortAsFloat(buffer.getShort(i + 12 - STRIDE * 3));
            float v0 = normalizeVertexTextureShortAsFloat(buffer.getShort(i + 14 - STRIDE * 3));

            float u1 = normalizeVertexTextureShortAsFloat(buffer.getShort(i + 12 - STRIDE * 2));
            float v1 = normalizeVertexTextureShortAsFloat(buffer.getShort(i + 14 - STRIDE * 2));

            float u2 = normalizeVertexTextureShortAsFloat(buffer.getShort(i + 12 - STRIDE));
            float v2 = normalizeVertexTextureShortAsFloat(buffer.getShort(i + 14 - STRIDE));

            float deltaU1 = u1 - u0;
            float deltaV1 = v1 - v0;
            float deltaU2 = u2 - u0;
            float deltaV2 = v2 - v0;

            float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
            float f;

            if (fdenom == 0.0) {
                f = 1.0f;
            } else {
                f = 1.0f / fdenom;
            }

            float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
            float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
            float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
            float tcoeff = rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz);
            tangentx *= tcoeff;
            tangenty *= tcoeff;
            tangentz *= tcoeff;

            float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
            float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
            float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
            float bitcoeff = rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz);
            bitangentx *= bitcoeff;
            bitangenty *= bitcoeff;
            bitangentz *= bitcoeff;

            // predicted bitangent = tangent × normal
            // Compute the determinant of the following matrix to get the cross product
            //  i  j  k
            // tx ty tz
            // nx ny nz

            float pbitangentx =   tangenty * normal.getZ() - tangentz * normal.getY();
            float pbitangenty = -(tangentx * normal.getZ() - tangentz * normal.getX());
            float pbitangentz =   tangentx * normal.getX() - tangenty * normal.getY();

            float dot = bitangentx * pbitangentx + bitangenty + pbitangenty + bitangentz * pbitangentz;
            byte tangentW;

            if (dot < 0) {
                tangentW = -127;
            } else {
                tangentW = 127;
            }

            int tangent = Norm3b.pack(tangentx, tangenty, tangentz);
            tangent |= (tangentW << 24);

            buffer.putInt(i + 24, tangent);
            buffer.putInt(i + 24 - STRIDE, tangent);
            buffer.putInt(i + 24 - STRIDE * 2, tangent);
            buffer.putInt(i + 24 - STRIDE * 3, tangent);
        }

        this.advance();
    }

    // TODO: Verify that this works with the new changes to the CVF
    private static float normalizeVertexPositionShortAsFloat(short value) {
        return (value & 0xFFFF) * (1.0f / 65535.0f);
    }

    // TODO: Verify that this is correct
    private static float normalizeVertexTextureShortAsFloat(short value) {
        return (value & 0xFFFF) * (1.0f / 32768.0f);
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
