package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.AnimatorImpl.class)
public interface SpriteInfoAnimationAccessor {
    @Accessor
    SpriteContents.Animation getAnimation();

    @Accessor("frame")
    int getFrameIndex();

    @Accessor("currentTime")
    int getFrameTicks();
}
