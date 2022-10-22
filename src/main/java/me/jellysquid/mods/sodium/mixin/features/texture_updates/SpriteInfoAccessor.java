package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import net.minecraft.class_7764;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_7764.class)
public interface SpriteInfoAccessor {
    @Accessor("field_40540")
    NativeImage[] getImages();
}
