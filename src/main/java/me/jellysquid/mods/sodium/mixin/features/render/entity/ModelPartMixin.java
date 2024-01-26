package me.jellysquid.mods.sodium.mixin.features.render.entity;

import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.immediate.model.EntityRenderer;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelPartData;
import me.jellysquid.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartData {
    @Shadow
    public float pivotX;
    @Shadow
    public float pivotY;
    @Shadow
    public float pivotZ;

    @Shadow
    public float xScale;
    @Shadow
    public float yScale;
    @Shadow
    public float zScale;

    @Shadow
    public float yaw;
    @Shadow
    public float pitch;
    @Shadow
    public float roll;

    @Shadow
    public boolean visible;
    @Shadow
    public boolean hidden;

    @Mutable
    @Shadow
    @Final
    private List<ModelPart.Cuboid> cuboids;

    @Mutable
    @Shadow
    @Final
    private Map<String, ModelPart> children;

    @Unique
    private ModelPart[] sodium$children;

    @Unique
    private ModelCuboid[] sodium$cuboids;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(List<ModelPart.Cuboid> cuboids, Map<String, ModelPart> children, CallbackInfo ci) {
        var copies = new ModelCuboid[cuboids.size()];

        for (int i = 0; i < cuboids.size(); i++) {
            var accessor = (ModelCuboidAccessor) cuboids.get(i);
            copies[i] = accessor.sodium$copy();
        }

        this.sodium$cuboids = copies;
        this.sodium$children = children.values()
                .toArray(ModelPart[]::new);

        // Try to catch errors caused by mods touching the collections after we've copied everything.
        this.cuboids = Collections.unmodifiableList(this.cuboids);
        this.children = Collections.unmodifiableMap(this.children);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", at = @At("HEAD"), cancellable = true)
    private void onRender(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(vertices);

        if (writer == null) {
            return;
        }

        ci.cancel();

        EntityRenderer.render(matrices, writer, (ModelPart) (Object) this, light, overlay, ColorABGR.pack(red, green, blue, alpha));
    }

    /**
     * @author JellySquid
     * @reason Apply transform more quickly
     */
    @Overwrite
    public void rotate(MatrixStack matrixStack) {
        if (this.pivotX != 0.0F || this.pivotY != 0.0F || this.pivotZ != 0.0F) {
            matrixStack.translate(this.pivotX * (1.0f / 16.0f), this.pivotY * (1.0f / 16.0f), this.pivotZ * (1.0f / 16.0f));
        }

        if (this.pitch != 0.0F || this.yaw != 0.0F || this.roll != 0.0F) {
            MatrixHelper.rotateZYX(matrixStack.peek(), this.roll, this.yaw, this.pitch);
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            matrixStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    @Override
    public ModelCuboid[] getCuboids() {
        return this.sodium$cuboids;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public boolean isHidden() {
        return this.hidden;
    }

    @Override
    public ModelPart[] getChildren() {
        return this.sodium$children;
    }
}
