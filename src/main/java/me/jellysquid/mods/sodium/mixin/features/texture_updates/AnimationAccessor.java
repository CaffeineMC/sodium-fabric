package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SpriteContents.Animation.class)
public interface AnimationAccessor {
    @Accessor
    List<SpriteContents.AnimationFrame> getFrames();

    @Accessor
    int getFrameCount();
}
