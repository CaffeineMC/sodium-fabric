package me.jellysquid.mods.sodium.mixin.features.textures;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.class)
public interface SpriteContentsInvoker {
    @Invoker
    void invokeUpload(int x, int y, int unpackSkipPixels, int unpackSkipRows, NativeImage[] images);
}