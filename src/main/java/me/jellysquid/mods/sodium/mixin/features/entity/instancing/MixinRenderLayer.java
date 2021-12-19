package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.layer.RenderLayerAccessor;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.class)
public abstract class MixinRenderLayer extends MixinRenderPhase implements RenderLayerAccessor {

    @Shadow
    @Final
    private boolean translucent;

    public boolean getTranslucent() {
        return translucent;
    }
}
