package me.jellysquid.mods.sodium.mixin.features.model;

import me.jellysquid.mods.sodium.interop.vanilla.model.DynamicMultipartBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.MultipartBakedModel;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(MultipartBakedModel.Builder.class)
public class MixinMultipartBakedModelBuilder {
    @Shadow
    @Final
    private List<Pair<Predicate<BlockState>, BakedModel>> components;

    @Inject(method = "build", at = @At("HEAD"), cancellable = true)
    private void injectBuild(CallbackInfoReturnable<BakedModel> cir) {
        if (this.components.size() <= DynamicMultipartBakedModel.MAX_COMPONENT_COUNT) {
            cir.setReturnValue(new DynamicMultipartBakedModel(this.components));
        }
    }
}
