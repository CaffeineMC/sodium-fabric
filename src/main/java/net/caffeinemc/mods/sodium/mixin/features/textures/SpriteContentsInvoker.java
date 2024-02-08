package net.caffeinemc.mods.sodium.mixin.features.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.class)
public interface SpriteContentsInvoker {
    @Invoker
    void invokeUpload(int x, int y, int unpackSkipPixels, int unpackSkipRows, NativeImage[] images);
}