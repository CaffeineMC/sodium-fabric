package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.FrameInfo.class)
public interface SpriteContentsAnimationFrameAccessor {
    @Accessor("time")
    int getTime();
}
