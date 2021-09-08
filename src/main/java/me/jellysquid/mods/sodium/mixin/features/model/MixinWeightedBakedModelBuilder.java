package me.jellysquid.mods.sodium.mixin.features.model;

import me.jellysquid.mods.sodium.interop.vanilla.model.DynamicWeightedBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.util.collection.Weighted;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WeightedBakedModel.Builder.class)
public class MixinWeightedBakedModelBuilder {
    @Shadow
    @Final
    private List<Weighted.Present<BakedModel>> models;

    @Inject(method = "build", at = @At(value = "NEW", target = "net/minecraft/client/render/model/WeightedBakedModel", shift = At.Shift.BEFORE), cancellable = true)
    private void injectBuild(CallbackInfoReturnable<BakedModel> cir) {
        cir.setReturnValue(new DynamicWeightedBakedModel(this.models));
    }
}
