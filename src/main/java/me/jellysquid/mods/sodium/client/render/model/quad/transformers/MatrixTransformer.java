package me.jellysquid.mods.sodium.client.render.model.quad.transformers;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadTransformer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import net.minecraft.client.util.math.*;

public class MatrixTransformer implements ModelQuadTransformer {
    private final Matrix4f modelMatrix;
    private final Matrix3f normalMatrix;

    private final Vector4f vector;
    private final Vector3f normal;

    public MatrixTransformer(MatrixStack matrixStack) {
        this.modelMatrix = matrixStack.peek().getModel();
        this.normalMatrix = matrixStack.peek().getNormal();
        this.vector = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        this.normal = new Vector3f(0.0f, 0.0f, 0.0f);
    }

    @Override
    public void transform(ModelQuadViewMutable quad) {
        Vector4f pos = this.vector;
        Vector3f norm = this.normal;

        for (int i = 0; i < 4; i++) {
            pos.set(quad.getX(i), quad.getY(i), quad.getZ(i), 1.0F);
            pos.transform(this.modelMatrix);

            quad.setX(i, pos.getX());
            quad.setY(i, pos.getY());
            quad.setZ(i, pos.getZ());

            int normI = quad.getNormal(i);

            float normX = QuadUtil.unpackNormalX(normI);
            float normY = QuadUtil.unpackNormalY(normI);
            float normZ = QuadUtil.unpackNormalZ(normI);

            norm.set(normX, normY, normZ);
            norm.transform(this.normalMatrix);

            quad.setNormal(i, QuadUtil.encodeNormal(norm.getX(), norm.getY(), norm.getZ()));
        }
    }
}
