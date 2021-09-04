package me.jellysquid.mods.sodium.render.renderer.transforms;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.util.math.Vec3d;

public class ModelOffsetTransform implements RenderContext.QuadTransform {
    private float x, y, z;

    public ModelOffsetTransform prepare(Vec3d offset) {
        this.x = (float) offset.getX();
        this.y = (float) offset.getY();
        this.z = (float) offset.getZ();

        return this;
    }

    @Override
    public boolean transform(MutableQuadView quad) {
        for (int i = 0; i < 4; i++) {
            quad.pos(i, quad.x(i) + this.x, quad.y(i) + this.y, quad.z(i) + this.z);
        }

        return true;
    }
}
