package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import me.jellysquid.mods.sodium.client.render.entity.EntityExtended;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntity.class)
public abstract class MixinArmorStandEntity implements EntityExtended {
    private EntityDimensions nonMarkerDimensions = EntityType.ARMOR_STAND.getDimensions();

    @Shadow
    protected abstract EntityDimensions method_31168(boolean marker);

    @Inject(method = "calculateDimensions", at = @At("TAIL"))
    public void postCalculateDimensions(CallbackInfo ci) {
        this.nonMarkerDimensions = this.method_31168(false);
    }

    @Override
    public float getLitWidth() {
        return this.nonMarkerDimensions.width;
    }

    @Override
    public float getLitHeight() {
        return this.nonMarkerDimensions.height;
    }
}
