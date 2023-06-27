package me.jellysquid.mods.sodium.mixin.features.frapi.block_non_terrain;

import me.jellysquid.mods.sodium.client.frapi.render.BlockModelRendererExtended;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class MixinBlockRenderManager {
    @Shadow
    private BlockModelRenderer blockModelRenderer;

    /**
     * Recreate contexts on reload to reset the sprite finder.
     */
    @Inject(at = @At("HEAD"), method = "reload")
    public void hook_reload(ResourceManager manager, CallbackInfo ci) {
        ((BlockModelRendererExtended) this.blockModelRenderer).sodium_clearFrapiContexts();
    }
}
