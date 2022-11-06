package net.caffeinemc.sodium.mixin.features.texture_tracking;

import net.caffeinemc.sodium.interop.vanilla.mixin.SpriteVisibilityStorage;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpriteContents.class)
public abstract class MixinSpriteContents implements SpriteVisibilityStorage {
    private boolean active;

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }
}
