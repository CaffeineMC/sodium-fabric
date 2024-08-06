package net.caffeinemc.mods.sodium.mixin.features.textures.animations.upload;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.Ticker.class)
public interface SpriteContentsTickerAccessor {
    @Accessor
    SpriteContents.AnimatedTexture getAnimationInfo();

    @Accessor("frame")
    int getFrameIndex();

    @Accessor("subFrame")
    int getFrameTicks();
}
