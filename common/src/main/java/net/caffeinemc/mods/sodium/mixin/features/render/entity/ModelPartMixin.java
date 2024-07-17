package net.caffeinemc.mods.sodium.mixin.features.render.entity;

import net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

@Mixin(ModelPart.class)
public class ModelPartMixin {
    @Shadow
    public float x;
    @Shadow
    public float y;
    @Shadow
    public float z;

    @Shadow
    public float xScale;
    @Shadow
    public float yScale;
    @Shadow
    public float zScale;

    @Shadow
    public float yRot;
    @Shadow
    public float xRot;
    @Shadow
    public float zRot;

    /**
     * @author JellySquid
     * @reason Apply transform more quickly
     */
    @Overwrite
    public void translateAndRotate(PoseStack matrixStack) {
        if (this.x != 0.0F || this.y != 0.0F || this.z != 0.0F) {
            matrixStack.translate(this.x * (1.0f / 16.0f), this.y * (1.0f / 16.0f), this.z * (1.0f / 16.0f));
        }

        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            MatrixHelper.rotateZYX(matrixStack.last(), this.zRot, this.yRot, this.xRot);
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            matrixStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }
}
