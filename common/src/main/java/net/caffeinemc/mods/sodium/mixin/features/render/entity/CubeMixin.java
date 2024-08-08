package net.caffeinemc.mods.sodium.mixin.features.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ModelPart.Cube.class)
public class CubeMixin {
    @Mutable
    @Shadow
    @Final
    public float minX;

    @Unique
    private ModelCuboid sodium$cuboid;

    // Inject at the start of the function, so we don't capture modified locals
    @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/model/geom/ModelPart$Cube;minX:F", ordinal = 0))
    private void onInit(ModelPart.Cube instance, float value, int u, int v, float x, float y, float z, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight, Set<Direction> renderDirections) {
        this.sodium$cuboid = new ModelCuboid(u, v, x, y, z, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, renderDirections);

        this.minX = value;
    }

    @Inject(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;pose()Lorg/joml/Matrix4f;"), cancellable = true)
    private void onCompile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color, CallbackInfo ci) {
        VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(buffer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        EntityRenderer.renderCuboid(pose, writer, this.sodium$cuboid, light, overlay, ColorARGB.toABGR(color));
    }
}
