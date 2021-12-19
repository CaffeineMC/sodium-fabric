package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import java.util.Optional;
import java.util.function.Supplier;

import me.jellysquid.mods.sodium.interop.vanilla.layer.RenderPhaseShaderAccessor;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.Shader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderPhase.Shader.class)
public class MixinRenderPhaseShader implements RenderPhaseShaderAccessor {

    // suppress because we can't do anything about it
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Shadow
    @Final
    private Optional<Supplier<Shader>> supplier;

    @Override
    public Optional<Supplier<Shader>> getSupplier() {
        return supplier;
    }
}
