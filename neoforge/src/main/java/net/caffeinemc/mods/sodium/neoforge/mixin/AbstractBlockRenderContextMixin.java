package net.caffeinemc.mods.sodium.neoforge.mixin;

import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractBlockRenderContextMixin implements RenderContext {
    @Shadow
    protected RenderType type;

    @Shadow
    protected Object modelData;

    @Override
    public ModelData getModelData() {
        return (ModelData) this.modelData;
    }

    @Override
    public RenderType getRenderLayer() {
        return type;
    }
}
