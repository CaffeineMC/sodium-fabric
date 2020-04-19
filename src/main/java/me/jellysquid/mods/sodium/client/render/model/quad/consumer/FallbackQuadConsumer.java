package me.jellysquid.mods.sodium.client.render.model.quad.consumer;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadConsumer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.*;

public class FallbackQuadConsumer implements ModelQuadConsumer {
    private final VertexConsumer consumer;
    private final Matrix4f modelMatrix;
    private final Matrix3f normalMatrix;

    private final Vector4f vector;
    private final Vector3f normal;

    public FallbackQuadConsumer(VertexConsumer consumer, MatrixStack matrixStack) {
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

            float r = ColorUtil.normalize(ColorUtil.unpackColorR(color));
            float g = ColorUtil.normalize(ColorUtil.unpackColorG(color));
            float b = ColorUtil.normalize(ColorUtil.unpackColorB(color));
            float a = ColorUtil.normalize(ColorUtil.unpackColorA(color));

            float u = quad.getTexU(i);
            float v = quad.getTexV(i);

            int light = quad.getLight(i);
            int norm = quad.getNormal(i);

            float normX = QuadUtil.unpackNormalX(norm);
            float normY = QuadUtil.unpackNormalY(norm);
            float normZ = QuadUtil.unpackNormalZ(norm);

            normVec.set(normX, normY, normZ);
            normVec.transform(this.normalMatrix);

            this.consumer.vertex(posVec.getX(), posVec.getY(), posVec.getZ(), r, g, b, a, u, v, OverlayTexture.DEFAULT_UV, light, normVec.getX(), normVec.getY(), normVec.getZ());
        }
    }
}
