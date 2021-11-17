/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import graphics.kiln.bakedminecraftmodels.BakedMinecraftModelsRenderLayerManager;
import graphics.kiln.bakedminecraftmodels.access.BatchContainer;
import graphics.kiln.bakedminecraftmodels.access.RenderLayerContainer;
import graphics.kiln.bakedminecraftmodels.data.InstanceBatch;
import graphics.kiln.bakedminecraftmodels.model.GlobalModelUtils;
import graphics.kiln.bakedminecraftmodels.model.VboBackedModel;
import me.jellysquid.mods.sodium.interop.vanilla.model.VboBackedModel;
import me.jellysquid.mods.sodium.render.entity.GlobalModelUtils;
import me.jellysquid.mods.sodium.render.entity.data.InstanceBatch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AnimalModel.class,
        BookModel.class, // TODO OPT: inject into renderBook instead of render so it works on block models
        CompositeEntityModel.class,
        EnderDragonEntityRenderer.DragonEntityModel.class,
        DragonHeadEntityModel.class,
        LlamaEntityModel.class,
        RabbitEntityModel.class,
        ShieldEntityModel.class,
        SignBlockEntityRenderer.SignModel.class,
        SinglePartEntityModel.class,
        SkullEntityModel.class,
        TintableAnimalModel.class,
        TintableCompositeModel.class,
        TridentEntityModel.class, // FIXME: enchantment glint uses dual
        TurtleEntityModel.class
})
public class MixinModels implements VboBackedModel {

    @Unique
    @Nullable
    private VertexBuffer bmm$bakedVertices;

    @Override
    @Unique
    public VertexBuffer getBakedVertices() {
        return bmm$bakedVertices;
    }

    @Unique
    private int bmm$vertexCount;

    @Override
    public int getVertexCount() {
        return bmm$vertexCount;
    }

    @Unique
    private float[] bmm$primitivePositions;

    @Override
    public float[] getPrimitivePositions() {
        return bmm$primitivePositions;
    }

    @Unique
    private int[] bmm$primitivePartIds;

    @Override
    public int[] getPrimitivePartIds() {
        return bmm$primitivePartIds;
    }

    @Unique
    private boolean bmm$currentPassBakeable;

    @Unique
    private VertexFormat.DrawMode bmm$drawMode;

    @Unique
    private VertexFormat bmm$vertexFormat;

    @Unique
    private MatrixStack.Entry bmm$baseMatrix;

    @Unique
    private InstanceBatch bmm$previousStoredBatch; // TODO: is this necessary anymore? should this be handled differently with batches?

    @Unique
    protected boolean bmm$childBakeable() { // this will be overridden by the lowest in the hierarchy as long as it's not private
        return bmm$currentPassBakeable;
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", at = @At("HEAD"))
    private void updateCurrentPass(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!bmm$childBakeable() && GlobalModelUtils.getNestedBufferBuilder(vertexConsumer) instanceof RenderLayerContainer renderLayerContainer) {
            RenderLayer convertedRenderLayer = BakedMinecraftModelsRenderLayerManager.tryDeriveSmartRenderLayer(renderLayerContainer.getRenderLayer());
            bmm$currentPassBakeable = convertedRenderLayer != null && MinecraftClient.getInstance().getWindow() != null;
            if (bmm$currentPassBakeable) {
                bmm$drawMode = convertedRenderLayer.getDrawMode();
                bmm$vertexFormat = convertedRenderLayer.getVertexFormat();
                bmm$baseMatrix = matrices.peek();

                BatchContainer batchContainer = (BatchContainer) matrices;
                bmm$previousStoredBatch = batchContainer.getBatch();
                batchContainer.setBatch(GlobalModelUtils.bakingData.getOrCreateInstanceBatch(convertedRenderLayer, this));
            }
        }
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", at = @At("HEAD"))
    private VertexConsumer changeVertexConsumer(VertexConsumer existingConsumer) {
        if (getBakedVertices() != null && bmm$currentPassBakeable) {
            return null;
        } else if (bmm$currentPassBakeable) {
            GlobalModelUtils.VBO_BUFFER_BUILDER.begin(bmm$drawMode, bmm$vertexFormat); // FIXME: not thread safe, could use a lock around it but may freeze program if nested model
            return GlobalModelUtils.VBO_BUFFER_BUILDER;
        } else {
            return existingConsumer;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", at = @At("TAIL"))
    private void createVbo(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (getBakedVertices() == null && bmm$currentPassBakeable) {
            bmm$vertexCount = GlobalModelUtils.VBO_BUFFER_BUILDER.getVertexCount();
            GlobalModelUtils.VBO_BUFFER_BUILDER.end();
            bmm$primitivePositions = GlobalModelUtils.VBO_BUFFER_BUILDER.getPrimitivePositions();
            bmm$primitivePartIds = GlobalModelUtils.VBO_BUFFER_BUILDER.getPrimitivePartIds();
            bmm$bakedVertices = new VertexBuffer();
            getBakedVertices().upload(GlobalModelUtils.VBO_BUFFER_BUILDER.getInternalBufferBuilder());
            GlobalModelUtils.bakingData.addCloseable(bmm$bakedVertices);
            GlobalModelUtils.VBO_BUFFER_BUILDER.clear();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", at = @At("TAIL"))
    private void setModelInstanceData(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (bmm$currentPassBakeable) {
            BatchContainer batchContainer = (BatchContainer) matrices;
            batchContainer.getBatch().addInstance(bmm$baseMatrix, red, green, blue, alpha, overlay, light);

            bmm$currentPassBakeable = false; // we want this to be false by default when we start at the top again
            // reset variables that we don't need until next run
            bmm$drawMode = null;
            bmm$vertexFormat = null;
            bmm$baseMatrix = null;
            batchContainer.setBatch(bmm$previousStoredBatch);
            bmm$previousStoredBatch = null;
        }
    }

}
