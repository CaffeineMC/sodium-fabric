package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.layer.MultiPhaseAccessor;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhase.class)
public class MixinMultiPhase implements MultiPhaseAccessor {
    @Shadow
    @Final
    private RenderLayer.MultiPhaseParameters phases;

    @Override
    public RenderLayer.MultiPhaseParameters getPhases() {
        return phases;
    }
}
