package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightingProvider.class)
public class MixinLightingProvider {
    @Shadow
    @Final
    protected HeightLimitView world;

    @Inject(method = "setColumnEnabled", at = @At(value = "TAIL"))
    private void postLightUpdate(ChunkPos pos, boolean retainData, CallbackInfo ci) {
        if (world instanceof ClientWorld && retainData) {
            SodiumWorldRenderer.instance()
                    .onChunkLightAdded(pos.x, pos.z);
        }
    }
}
