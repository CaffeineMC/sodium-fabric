package net.caffeinemc.mods.sodium.mixin.features.textures.animations.upload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;

@Mixin(SpriteContents.AnimatedTexture.class)
public interface SpriteContentsAnimatedTextureAccessor {
    @Accessor
    List<SpriteContents.FrameInfo> getFrames();

    @Accessor
    int getFrameRowSize();
}
