package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.LightmapTextureManagerAccessor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightTexture.class)
public class MixinLightmapTextureManager implements LightmapTextureManagerAccessor {
    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Override
    public AbstractTexture getTexture() {
        return this.lightTexture;
    }
}
