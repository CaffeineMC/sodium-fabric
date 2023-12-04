package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SpriteContents.Animation.class)
public interface SpriteContentsAnimationAccessor {
    @Accessor("frames")
    List<SpriteContents.AnimationFrame> getFrames();
}
