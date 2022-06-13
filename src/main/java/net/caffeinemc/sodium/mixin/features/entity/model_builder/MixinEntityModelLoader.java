package net.caffeinemc.sodium.mixin.features.entity.model_builder;

import net.caffeinemc.sodium.interop.vanilla.mixin.ModelManagerHolder;
import net.caffeinemc.sodium.render.entity.compile.SodiumModelManager;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(EntityModelLoader.class)
public class MixinEntityModelLoader implements ModelManagerHolder {

    @Shadow
    private Map<EntityModelLayer, TexturedModelData> modelParts;

    private SodiumModelManager modelManager;

    @Override
    public SodiumModelManager getSodiumEntityModelManager() {
        return this.modelManager;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.modelManager = new SodiumModelManager();
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private void rebuildModels(ResourceManager manager, CallbackInfo ci) {
        this.modelManager.buildModels(this.modelParts);
    }
}
