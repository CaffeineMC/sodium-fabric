package me.jellysquid.mods.sodium.mixin.features.render_layer;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(RenderLayers.class)
public class MixinRenderLayers {
    @Mutable
    @Shadow
    @Final
    private static Map<Block, RenderLayer> BLOCKS;

    @Mutable
    @Shadow
    @Final
    private static Map<Fluid, RenderLayer> FLUIDS;

    static {
        // Replace the backing collection types with something a bit faster, since this is a hot spot in chunk rendering.
        BLOCKS = new Reference2ReferenceOpenHashMap<>(BLOCKS);
        FLUIDS = new Reference2ReferenceOpenHashMap<>(FLUIDS);
    }
}
