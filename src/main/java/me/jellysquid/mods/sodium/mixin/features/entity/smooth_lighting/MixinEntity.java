package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import me.jellysquid.mods.sodium.client.render.entity.EntityExtended;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class MixinEntity implements EntityExtended {
    @Shadow
    protected abstract float getWidth();

    @Shadow
    protected abstract float getHeight();

    @Override
    public float getLitWidth() {
        return this.getWidth();
    }

    @Override
    public float getLitHeight() {
        return this.getHeight();
    }
}
