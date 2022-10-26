package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpriteContents.class)
public abstract class MixinSpriteContents implements SpriteExtended {
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
