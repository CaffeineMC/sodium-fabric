package me.jellysquid.mods.sodium.client.render.model.quad.transformers;

import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadTransformer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;

public class TranslateTransformer implements ModelQuadTransformer {
    private int x, y, z;

    @Override
    public void transform(ModelQuadViewMutable quad) {
        for (int i = 0; i < 4; i++) {
            quad.setX(i, quad.getX(i) + this.x);
            quad.setY(i, quad.getY(i) + this.y);
            quad.setZ(i, quad.getZ(i) + this.z);
        }
    }

    public void setOffset(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
