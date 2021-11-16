package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.layer.BufferBuilderExtended;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements BufferBuilderExtended {
    private RenderLayer renderLayer;

    @Override
    public RenderLayer getRenderLayer() {
        return renderLayer;
    }

    @Override
    public void setRenderLayer(RenderLayer renderLayer) {
        this.renderLayer = renderLayer;
    }
}
