package me.jellysquid.mods.sodium.interop.vanilla.model;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;

public abstract class DynamicBakedModel implements BakedModel, FabricBakedModel {
    private final BakedModel defaultModel;

    protected DynamicBakedModel(BakedModel defaultModel) {
        this.defaultModel = defaultModel;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.defaultModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return this.defaultModel.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return this.defaultModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return this.defaultModel.isBuiltin();
    }

    @Override
    public Sprite getSprite() {
        return this.defaultModel.getSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return this.defaultModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return this.defaultModel.getOverrides();
    }
}
