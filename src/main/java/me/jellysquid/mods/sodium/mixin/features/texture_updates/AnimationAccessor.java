package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import net.minecraft.class_7764;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(class_7764.Animation.class)
public interface AnimationAccessor {
    @Accessor
    List<class_7764.AnimationFrame> getFrames();

    @Accessor
    int getFrameCount();
}
