package me.jellysquid.mods.sodium.mixin.features.textures.animations.upload;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.FrameInfo.class)
public interface SpriteContentsAnimationFrameAccessor {
    @Accessor
    int getIndex();

    @Accessor
    int getTime();
}
