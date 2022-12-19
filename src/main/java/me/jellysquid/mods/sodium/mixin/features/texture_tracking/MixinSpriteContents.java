package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteContents.class)
public abstract class MixinSpriteContents implements SpriteExtended {
    @Shadow
    @Final
    @Nullable
    private SpriteContents.Animation animation;

    private boolean active;

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean hasAnimation() {
        return this.animation != null;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }
}
