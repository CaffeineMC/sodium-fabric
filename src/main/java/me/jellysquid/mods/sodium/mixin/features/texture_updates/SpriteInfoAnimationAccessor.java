package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import net.minecraft.class_7764;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_7764.class_7765.class)
public interface SpriteInfoAnimationAccessor {
    @Accessor("field_40546")
    class_7764.Animation getAnimation();

    @Accessor("field_40544")
    int getFrameIndex();

    @Accessor("field_40545")
    int getFrameTicks();
}
