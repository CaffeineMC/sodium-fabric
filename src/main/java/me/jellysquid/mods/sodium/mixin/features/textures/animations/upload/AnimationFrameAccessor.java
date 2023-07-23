package me.jellysquid.mods.sodium.mixin.features.textures.animations.upload;

import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.AnimationFrame.class)
public interface AnimationFrameAccessor {
    @Accessor
    int getIndex();

    @Accessor
    int getTime();
}
