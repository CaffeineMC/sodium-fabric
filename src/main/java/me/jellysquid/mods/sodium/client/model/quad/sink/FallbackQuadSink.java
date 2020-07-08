package me.jellysquid.mods.sodium.client.model.quad.sink;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

/**
 * A fallback implementation of {@link ModelQuadSink} for when we're writing into an arbitrary {@link BufferBuilder}.
 * This implementation is considerably slower than other sinks as it must perform many matrix transformations for every
 * vertex and unpack values as assumptions can't be made about what the backing buffer type is.
 */
public class FallbackQuadSink implements ModelQuadSink, ModelQuadSinkDelegate {
    private final VertexConsumer consumer;

    // Hoisted matrices to avoid lookups in peeking
    private final Matrix4f modelMatrix;
    private final Matrix3f normalMatrix;

    // Cached vectors to avoid allocations
    private final Vector4f vector;
    private final Vector3f normal;

    public FallbackQuadSink(VertexConsumer consumer, MatrixStack matrixStack) {
        this.consumer = consumer;
        this.modelMatrix = matrixStack.peek().getModel();
        this.normalMatrix = matrixStack.peek().getNormal();
        this.vector = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        this.normal = new Vector3f(0.0f, 0.0f, 0.0f);
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        Vector4f posVec = this.vector;
        Vector3f normVec = this.normal;

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            posVec.set(x, y, z, 1.0F);
            posVec.transform(this.modelMatrix);

            int color = quad.getColor(i);

            float r = ColorARGB.normalize(ColorARGB.unpackRed(color));
            float g = ColorARGB.normalize(ColorARGB.unpackGreen(color));
            float b = ColorARGB.normalize(ColorARGB.unpackBlue(color));
            float a = ColorARGB.normalize(ColorARGB.unpackAlpha(color));

            float u = quad.getTexU(i);
            float v = quad.getTexV(i);

            int light = quad.getLight(i);
            int norm = quad.getNormal(i);

            float normX = Norm3b.unpackX(norm);
            float normY = Norm3b.unpackY(norm);
            float normZ = Norm3b.unpackZ(norm);

            normVec.set(normX, normY, normZ);
            normVec.transform(this.normalMatrix);

            this.consumer.vertex(posVec.getX(), posVec.getY(), posVec.getZ(), r, g, b, a, u, v, OverlayTexture.DEFAULT_UV, light, normVec.getX(), normVec.getY(), normVec.getZ());
        }
    }

    @Override
    public ModelQuadSink get(ModelQuadFacing facing) {
        return this;
    }
}
