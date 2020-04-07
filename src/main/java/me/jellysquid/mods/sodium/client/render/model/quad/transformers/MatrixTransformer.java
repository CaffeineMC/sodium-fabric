package me.jellysquid.mods.sodium.client.render.model.quad.transformers;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadTransformer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector4f;

public class MatrixTransformer implements ModelQuadTransformer {
    private final Matrix4f matrix;
    private final Vector4f vector;

    public MatrixTransformer(MatrixStack matrixStack) {
        this.matrix = matrixStack.peek().getModel();
        this.vector = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public void transform(ModelQuadViewMutable quad) {
        Vector4f vec = this.vector;

        for (int i = 0; i < 4; i++) {
            vec.set(quad.getX(i), quad.getY(i), quad.getZ(i), 1.0F);
            vec.transform(this.matrix);

            quad.setX(i, vec.getX());
            quad.setY(i, vec.getY());
            quad.setZ(i, vec.getZ());
        }
    }
}
