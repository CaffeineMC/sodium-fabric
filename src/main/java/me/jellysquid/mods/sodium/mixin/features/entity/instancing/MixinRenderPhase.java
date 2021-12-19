package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.layer.RenderPhaseAccessor;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderPhase.class)
public class MixinRenderPhase implements RenderPhaseAccessor {

    @Shadow
    @Final
    protected String name;

    @Override
    public String getName() {
        return name;
    }
}
