package net.caffeinemc.mods.sodium.mixin.features.textures.animations.upload;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.FrameInfo.class)
public interface SpriteContentsFrameInfoAccessor {
    @Accessor
    int getIndex();

    @Accessor
    int getTime();
}
