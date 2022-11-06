package net.caffeinemc.sodium.mixin.features.chunk_rendering;

import net.caffeinemc.sodium.render.texture.SpriteAnimationInterface;
import net.minecraft.client.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteContents.class)
public class MixinSpriteContents implements SpriteAnimationInterface {
    @Shadow
    @Final
    @Nullable
    private SpriteContents.Animation animation;

    @Override
    public boolean hasAnimation() {
        return this.animation != null;
    }
}
