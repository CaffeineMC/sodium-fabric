package me.jellysquid.mods.sodium.client.render.renderer;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import org.apache.commons.lang3.Validate;

abstract class RenderContextBase implements RenderContext {
    private final Stack<QuadTransform> transformStack = new ObjectArrayList<>();

    protected final boolean transform(MutableQuadView q) {
        if (this.transformStack.isEmpty()) {
            return true;
        }

        return this.transformStack.top()
                .transform(q);
    }

    @Override
    public void pushTransform(QuadTransform transform) {
        Validate.notNull(transform, "Transform must be non-null");

        this.transformStack.push(transform);
    }

    @Override
    public void popTransform() {
        this.transformStack.pop();
    }
}
