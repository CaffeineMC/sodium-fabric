package me.jellysquid.mods.sodium.mixin.features.texture_updates;

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
